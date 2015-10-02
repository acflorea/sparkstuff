package controllers

import dr.acf.services.spark.SparkService. _
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

  // configs
  val hdfsHost = Play.configuration.getString("spark.hdfs.host").getOrElse("localhost")
  val hdfsPort = Play.configuration.getInt("spark.hdfs.port").getOrElse(54310)

  val rootFolder = "user/ds"

  // Load data from HDFS - raw format
  lazy val rawUserArtistData = sc.textFile(s"hdfs://$hdfsHost:$hdfsPort/$rootFolder/user_artist_data.txt")
  lazy val rawArtistAlias = sc.textFile(s"hdfs://$hdfsHost:$hdfsPort/$rootFolder/artist_alias.txt")
  lazy val rawArtistData = sc.textFile(s"hdfs://$hdfsHost:$hdfsPort/$rootFolder/artist_data.txt")

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

}
