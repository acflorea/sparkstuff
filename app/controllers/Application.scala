package controllers

import play.api._
import play.api.mvc._
import dr.acf.services.spark.SparkService._

object Application extends Controller {

  def index = Action {

    Ok(views.html.index(s"Spark App - ${sc.appName} :: ${sc.applicationId}"))
  }

}