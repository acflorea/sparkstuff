package controllers

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

  // Load data from HDFS
  lazy val rawblocks = sc.textFile(s"hdfs://$hdfsHost:$hdfsPort/linkage")

  /**
   * Returns first row
   * @return first data row
   */
  def first() = Action.async { implicit request =>
    // ***
    val first = rawblocks.first()
    // ***
    Future.successful(Ok(Json.toJson(first)))
  }

  /**
   * Obtain a data sample
   * @return first data row
   */
  def sample(
              withReplacement: Option[Boolean],
              fraction: Option[Double]) = Action.async { implicit request =>
    // ***
    val samples = rawblocks.
      sample(withReplacement.getOrElse(false), fraction.getOrElse(.000001)).
      collect()
    // ***
    Future.successful(Ok(Json.toJson(samples)))
  }

}
