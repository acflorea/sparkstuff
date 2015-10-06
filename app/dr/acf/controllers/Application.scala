package dr.acf.controllers

import dr.acf.services.spark.SparkService._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(s"Spark App - ${sc.appName} :: ${sc.applicationId} on ${sc.master}"))
  }

}