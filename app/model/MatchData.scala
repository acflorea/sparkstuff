package model

import play.api.libs.json.Json

/**
 * Created by aflorea on 27.09.2015.
 * Case class - matches data rows @see http://bit.ly/1Aoywaq
 * @param id1 - id of the first patient
 * @param id2 - id of the second patient
 * @param scores - scores on various criteria
 * @param matched - weather records are a match
 */
case class MatchData(id1: Int, id2: Int,
                     scores: Array[Option[Double]], matched: Boolean)

object MatchData {
  // Default Json format for MatchData
  implicit val matchDataJsonFormat = Json.format[MatchData]
}
