package dr.acf.model.musicrecommender

import play.api.libs.json.Json

/**
 * Artist transporter
 * Created by aflorea on 08.10.2015.
 */
case class Artist(id:Int, name:String)

object Artist{
  implicit val artistJsonFormat = Json.format[Artist]
}
