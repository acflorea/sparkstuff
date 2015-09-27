package model

import org.apache.spark.util.StatCounter
import play.api.libs.json._

import scala.collection.immutable.Stream.#::

/**
 * Generic stats computation utility class
 * Created by aflorea on 27.09.2015.
 */
class NAStatCounter(values: TraversableOnce[Double]) extends Serializable {

  val stats: StatCounter = new StatCounter()
  var missing: Long = 0
  var sumSquares: Double = 0 // sum of squares

  merge(values)

  /**
   * Add a new sample to statistics
   * @param x - double value
   * @return
   */
  def merge(x: Double): NAStatCounter = {
    if (java.lang.Double.isNaN(x)) {
      missing += 1
    } else {
      stats.merge(x)
      sumSquares += x * x
    }
    this
  }

  /** Add multiple values into this StatCounter, updating the internal statistics. */
  def merge(values: TraversableOnce[Double]): NAStatCounter = {
    values.foreach(v => merge(v))
    this
  }

  /**
   * Merge two instances of NAStatCounter together
   * @param other - NAStatCounter to combine with
   * @return
   */
  def merge(other: NAStatCounter): NAStatCounter = {
    stats.merge(other.stats)
    missing += other.missing
    sumSquares = other.sumSquares
    this
  }

  /**
   * Pretty printer
   * @return
   */
  override def toString = {
    "stats: " + stats.toString + " sum(x^2): " + sumSquares + " NaN: " + missing
  }
}

/**
 * Companion object for NAStatCounter
 */
object NAStatCounter extends Serializable {

  import play.api.libs.functional.syntax._

  val NAStatCounterWrites: Writes[NAStatCounter] = (
    (JsPath \ "stats").write[String] and
      (JsPath \ "sumSquares").write[Double] and
      (JsPath \ "missing").write[Long]
    )(s =>
    (s.stats.toString(), s.sumSquares, s.missing))

  // TODO - ugly hack, try to find a better Reads implementation
  val NAStatCounterReads: Reads[NAStatCounter] = (
    (JsPath \ "stats").read[String] and
      (JsPath \ "sumSquares").read[Double] and
      (JsPath \ "missing").read[Long]
    )((stats, sum, missing) => NAStatCounter(Double.NaN))

  implicit val NAStatCounterJsonFormat = Format[NAStatCounter](NAStatCounterReads, NAStatCounterWrites)

  /** Build a StatCounter from a list of values. */
  def apply(values: TraversableOnce[Double]): NAStatCounter = new NAStatCounter(values)

  /** Build a StatCounter from a list of values passed as variable-length arguments. */
  def apply(values: Double*): NAStatCounter = new NAStatCounter(values)

}