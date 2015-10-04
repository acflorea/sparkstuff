package controllers

import dr.acf.services.spark.SparkService._
import org.apache.hadoop.fs.Path
import org.apache.spark.mllib.recommendation.Rating
import play.api.Play
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller

import scala.concurrent.Future
import play.api.Play.current

/**
 * @see http://shop.oreilly.com/product/0636920035091.do - Chapter 3
 *      Recommending Music and the Audioscrobbler Data Set
 *      Created by aflorea on 30.09.2015.
 */
object MusicRecommenderController extends Controller {

  val rootFolder = s"/user/ds"

  // Load data from HDFS - raw format
  val rawUserArtistData = sc.textFile(fs.resolvePath(s"$rootFolder/user_artist_data.txt"), 10)
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
  lazy val trainData = {
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
    sc.textFile(fs.resolvePath(parsed_user_artist_data))
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
  def sample(
              withReplacement: Option[Boolean],
              count: Option[Int]) = Action.async { implicit request =>

    implicit val ratingFormat = Json.format[Rating]
    val samples = trainData.
      takeSample(withReplacement.getOrElse(false), count.getOrElse(10))
    // ***
    Future.successful(Ok(Json.toJson(samples)))
  }


}
