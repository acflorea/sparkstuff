package dr.acf.controllers

import dr.acf.model.musicrecommender.Artist
import dr.acf.services.spark.SparkService._
import org.apache.spark.mllib.recommendation.{MatrixFactorizationModel, ALS, Rating}
import org.apache.spark.rdd.RDD
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

/**
 * @see http://shop.oreilly.com/product/0636920035091.do - Chapter 3
 *      Recommending Music and the Audioscrobbler Data Set
 *      Created by aflorea on 30.09.2015.
 */
object MusicRecommenderController extends Controller {

  val rootFolder = "/user/ds"

  // Load data from HDFS - raw format
  val rawUserArtistData = sc.textFile(fs.resolvePath(s"$rootFolder/user_artist_data.txt"))
  val rawArtistAlias = sc.textFile(fs.resolvePath(s"$rootFolder/artist_alias.txt"))
  val rawArtistData = sc.textFile(fs.resolvePath(s"$rootFolder/artist_data.txt"))

  /**
   * Artist data split and mapped to Option(id, name)
   * None in case of bad formatted data
   */
  lazy val artistByID = rawArtistData.flatMap { line =>
    // split the line
    val (id, name) = line.span(_ != '\t')
    // bad data ?
    if (name.isEmpty) {
      None
    } else {
      try {
        Some((id.toInt, name.trim))
      } catch {
        // bad again ?
        case e: NumberFormatException => None
      }
    }
  }

  /**
   * Artist alias (bad_id -> ok_id)
   */
  lazy val artistAlias = rawArtistAlias.flatMap { line =>
    val tokens = line.split('\t')
    if (tokens(0).isEmpty) {
      None
    } else {
      Some((tokens(0).toInt, tokens(1).toInt))
    }
  }.collectAsMap()

  /**
   * Train data
   * File consisting on all available instances of
   * Rating(userID, finalArtistID, count)
   */
  lazy val trainData: RDD[Rating] = {
    val parsed_user_artist_data = s"$rootFolder/parsed_user_artist_data.txt"
    if (!fs.exists(parsed_user_artist_data)) {
      // compute and store parsed/formatted file
      val bArtistAlias = sc.broadcast(artistAlias)
      rawUserArtistData.map { line =>
        // canonical artistID (use the Alias Map)
        val Array(userID, artistID, count) = line.split(' ').map(_.toInt)
        val finalArtistID =
          bArtistAlias.value.getOrElse(artistID, artistID)
        Rating(userID, finalArtistID, count)
      }
      rawUserArtistData.saveAsTextFile(
        fs.resolvePath(rootFolder).concat("/parsed_user_artist_data.txt")
      )
    }
    // return parsed and formatted file
    sc.textFile(fs.resolvePath(parsed_user_artist_data)) map { line =>
      val Array(userID, artistID, count) = line.split(' ').map(_.toInt)
      Rating(userID, artistID, count)
    }
  }

  /**
   * Retrieves artist name by id
   * @param id - artist id to look for
   * @return
   */
  def lookupArtistById(id: Int) = Action.async { implicit request =>
    Future.successful(
      artistByID.lookup(id).headOption match {
        case Some(artist) => Ok(Json.toJson(artist))
        case None => NotFound
      }
    )
  }

  /**
   * Obtain a data sample
   * @return a fraction of data
   */
  def sample(withReplacement: Option[Boolean],
             count: Option[Int]) = Action.async { implicit request =>

    implicit val ratingFormat = Json.format[Rating]
    val samples = trainData.
      takeSample(withReplacement.getOrElse(false), count.getOrElse(10))
    // ***
    Future.successful(Ok(Json.toJson(samples)))
  }


  /**
   * Build the model and stores it for later use
   * ALS => MatrixFactorizationModel
   * @param modelName the name to use to store the model
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   * @param alpha      confidence parameter
   */
  def trainAndSave(modelName: String,
                   rank: Option[Int],
                   iterations: Option[Int],
                   lambda: Option[Double],
                   alpha: Option[Double]) = {
    Action.async { implicit request =>
      val clock = System.currentTimeMillis()
      trainData.cache()
      val model = ALS.trainImplicit(trainData, rank.get, iterations.get, lambda.get, alpha.get)
      model.save(sc, fs.resolvePath(rootFolder).concat(s"/models/$modelName"))
      val duration = (System.currentTimeMillis() - clock) / 1000
      Future.successful(Ok(s"Model trained and saved in $duration seconds."))
    }
  }

  /**
   * Charges a previously saved model
   * @param modelName name of the saved model
   */
  def loadModel(modelName: String) = {
    Action.async { implicit request =>
      val clock = System.currentTimeMillis()
      val path = fs.resolvePath(rootFolder).concat(s"/models/$modelName")
      if (fs.exists(path)) {
        val model = MatrixFactorizationModel.load(sc, path)
        val duration = (System.currentTimeMillis() - clock) / 1000
        Future.successful(Ok(s"Model loaded in $duration seconds."))
      } else {
        Future.successful(NotFound)
      }
    }
  }

  /**
   * Model quality check with a human touch
   * @param modelName - name of the model to use
   * @return - most listen to 5 artists plus 5 recommended artists
   */
  def randomCheck(modelName: String) = {
    Action.async { implicit request =>
      import dr.acf.model.musicrecommender.Artist._
      val artist = (r: Rating) => Artist(r.product, artistByID.lookup(r.product).head)

      val path = fs.resolvePath(rootFolder).concat(s"/models/$modelName")
      if (fs.exists(path)) {
        // load model
        val model = MatrixFactorizationModel.load(sc, path)
        // take on rating randomly
        val sampleRating = trainData.takeSample(withReplacement = false, 1).head
        // retrieve the user
        val userID = sampleRating.user
        // get this user top 5 ratings
        val ratings = trainData.filter(_.user == userID).takeOrdered(5)(Ordering.by(r => -r.rating))
        val ratedProducts = ratings map (_.product)
        // make recommendation
        val predictions =
          model.recommendProducts(userID, 10) filter (r => !ratedProducts.contains(r.product))

        val preferredArtists = ratings map artist
        val predictedArtists = predictions take 5 map artist

        val output = Map("preferred" -> preferredArtists, "predicted" -> predictedArtists)
        Future.successful(Ok(Json.toJson(output)))
      } else {
        Future.successful(NotFound)
      }
    }
  }

}
