package dr.acf.services.spark

import dr.acf.services.generic.AbstractService
import org.apache.commons.configuration.BaseConfiguration
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}
import org.apache.spark.{SparkConf, SparkContext}
import play.api.Play
import play.api.Play.current
import scala.collection.JavaConversions._

/**
 * Spark global settings
 * Created by aflorea on 27.09.2015.
 */
object SparkService extends AbstractService {

  private var _sc: SparkContext = null
  private var _fs: FileSystem = null

  /**
   * Spark Context
   * @return
   */
  def sc = _sc

  /**
   * File System
   * @return
   */
  def fs = _fs

  override def doConfigure(): Unit = {
    val sparkConf = new BaseConfiguration().subset("spark")

    // merge defaults with loaded configs
    Play.configuration.getConfig("spark").foreach {
      _.entrySet foreach { tuple =>
        sparkConf.setProperty(tuple._1, tuple._2.unwrapped())
      }
    }

    // Spark config properties
    val master = sparkConf.getString("master.URL", "local")
    val appName = sparkConf.getString("app.name", "SparkStuff")

    // Spark configuration
    val config: SparkConf = new SparkConf().setMaster(master).setAppName(appName)

    for (key <- sparkConf.getKeys.map(_.toString)) {
      config.set(s"spark.$key", sparkConf.getProperty(key).toString)
    }

    // Initialize Spark context
    _sc = new SparkContext(config)

    val dfsConf = new BaseConfiguration().subset("dfs")

    // merge defaults with loaded configs
    Play.configuration.getConfig("dfs").foreach {
      _.entrySet foreach { tuple =>
        dfsConf.setProperty(tuple._1, tuple._2.unwrapped())
      }
    }

    // DFS config properties
    val dfsHost = dfsConf.getString("host", "localhost")
    val dfsPort = dfsConf.getInt("port", 54310)

    val conf = new Configuration()
    conf.set("fs.default.name", s"hdfs://$dfsHost:$dfsPort")
    _fs = FileSystem.get(conf)

  }

  override def doShutdown(): Unit = {
    sc.cancelAllJobs()
    sc.stop()
  }

  override def doSetup(): Unit = {}

  /** Implicits **/

  implicit def strToPath(pathStr: String): Path = new Path(pathStr)

  implicit def pathToStr(path: Path): String = path.toUri.toString

}

