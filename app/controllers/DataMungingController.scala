package controllers

import model.MatchData
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.StatCounter
import play.api.Play
import play.api.mvc.{Action, Controller}
import dr.acf.services.spark.SparkService._
import play.api.libs.json._
import play.api.Play.current
import scala.concurrent.Future

/**
 * @see http://shop.oreilly.com/product/0636920035091.do - Chapter 2
 *      Introduction to Data Analysis with Scala and Spark
 *      Created by aflorea on 27.09.2015.
 */
object DataMungingController extends Controller {

  // configs
  val hdfsHost = Play.configuration.getString("spark.hdfs.host").getOrElse("localhost")
  val hdfsPort = Play.configuration.getInt("spark.hdfs.port").getOrElse(54310)

  // Load data from HDFS - raw format
  lazy val rawBlocks = sc.textFile(s"hdfs://$hdfsHost:$hdfsPort/linkage")

  // Parsed data
  lazy val dataBlocks = rawBlocks.
    // No header lines
    filter(l => !isHeader(l)).
    // Map to case class
    map(l => parse(l))

  /**
   * Returns first row
   * @return first data row
   */
  def first(raw: Boolean) = Action.async { implicit request =>
    if (raw) {
      // ***
      val first = rawBlocks.first()
      // ***
      Future.successful(Ok(Json.toJson(first)))
    } else {
      // ***
      val first = dataBlocks.first()
      // ***
      Future.successful(Ok(Json.toJson(first)))
    }
  }

  /**
   * Returns first x rows - no more than 100 (safety first)
   * @return first x rows
   */
  def take(raw: Boolean, howMany: Int) = Action.async { implicit request =>
    if (raw) {
      // ***
      val firstX = rawBlocks.take(Math.min(howMany, 100))
      // ***
      Future.successful(Ok(Json.toJson(firstX)))
    } else {
      // ***
      val firstX = dataBlocks.take(Math.min(howMany, 100))
      // ***
      Future.successful(Ok(Json.toJson(firstX)))
    }
  }

  /**
   * Returns the number of rows
   * @return row count
   */
  def count(raw: Boolean) = Action.async { implicit request =>
    if (raw) {
      // ***
      val count = rawBlocks.count()
      // ***
      Future.successful(Ok(Json.toJson(count)))
    } else {
      // ***
      val count = dataBlocks.count()
      // ***
      Future.successful(Ok(Json.toJson(count)))
    }
  }


  /**
   * Obtain a data sample
   * @return first data row
   */
  def sample(
              raw: Boolean,
              withReplacement: Option[Boolean],
              fraction: Option[Double]) = Action.async { implicit request =>
    if (raw) {
      // ***
      val samples = rawBlocks.
        sample(withReplacement.getOrElse(false), fraction.getOrElse(.000001)).
        collect()
      // ***
      Future.successful(Ok(Json.toJson(samples)))
    } else {
      // ***
      val samples = dataBlocks.
        sample(withReplacement.getOrElse(false), fraction.getOrElse(.000001)).
        collect()
      // ***
      Future.successful(Ok(Json.toJson(samples)))
    }
  }

  /**
   * Statistics per score element
   * @param position - score index
   *                 must be between 0 and 9 to match a valid position
   */
  def stats(position: Int) = {
    Action.async {
      implicit request =>
        if (position > 0 && position < 9) {
          import java.lang.Double.isNaN
          val stats = dataBlocks.
            map(md => md.scores(position).getOrElse(Double.NaN)).
            filter(!isNaN(_)).
            stats()
          Future.successful(Ok(Json.toJson(stats.toString())))
        } else {
          Future.successful(BadRequest("Wrong index"))
        }
    }
  }

  /** Private space */

  /**
   * Checks if a line is a header line (contains column names)
   * @param line - current line
   * @return true for header lines, false otherwise
   */
  def isHeader(line: String) = line.contains("id_1")

  /**
   * Converts a potential missing (?) value to Double
   * @param s - input value
   * @return - corresponding double value or NaN in case of missing input
   */
  def toOptionalDouble(s: String): Option[Double] = {
    if ("?".equals(s)) None else Some(s.toDouble)
  }

  /**
   * Data parser String -> MatchData
   * @param line - input line
   * @return - MatchData representation
   */
  def parse(line: String) = {
    val pieces = line.split(',')
    val id1 = pieces(0).toInt
    val id2 = pieces(1).toInt
    val scores = pieces.slice(2, 11).map(toOptionalDouble)
    val matched = pieces(11).toBoolean
    MatchData(id1, id2, scores, matched)
  }
}
