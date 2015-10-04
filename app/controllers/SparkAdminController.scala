package controllers

import play.api.mvc.{Action, Controller}
import dr.acf.services.spark.SparkService._

import scala.concurrent.Future

/**
 * Various admin actions on the Spark Context
 * Created by aflorea on 04.10.2015.
 */
object SparkAdminController extends Controller {

  /**
   * Cancel all Spark Jobs
   * @return
   */
  def cancelAllJobs() = Action.async { implicit request =>
    sc.cancelAllJobs()
    Future.successful(Ok("Jobs Cancelled"))
  }

}
