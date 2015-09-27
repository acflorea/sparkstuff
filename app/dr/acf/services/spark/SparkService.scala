package dr.acf.services.spark

import dr.acf.services.generic.AbstractService
import org.apache.commons.configuration.BaseConfiguration
import org.apache.spark.{SparkConf, SparkContext}
import play.api.Play
import play.api.Play.current

/**
 * Spark global settings
 * Created by aflorea on 27.09.2015.
 */
object SparkService extends AbstractService {

  private var _sc: SparkContext = null

  /**
   * Spark Context
   * @return
   */
  def sc = _sc

  override def doConfigure(): Unit = {
    val sparkConf = new BaseConfiguration().subset("spark")

    // merge defaults with loaded configs
    Play.configuration.getConfig("spark").map {
      _.entrySet map { tuple =>
        sparkConf.setProperty(tuple._1, tuple._2.unwrapped())
      }
    }

    // Spark configuration
    val config: SparkConf = new SparkConf().setMaster("local").setAppName("SparkPlay")
    // Initialize Spark context
    _sc = new SparkContext(config)
  }

  override def doShutdown(): Unit = {
    sc.cancelAllJobs()
    sc.stop()
  }

  override def doSetup(): Unit = {}
}
