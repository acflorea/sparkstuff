package dr.acf.services.generic

import play.api.Logger

/**
 * Base trait for all App Services
 * Created by aflorea on 27.09.2015.
 */
trait AbstractService {

  final val name = this.getClass.getSimpleName

  def log(message: => String) = Logger.info(s"[Services:$name] $message")

  /* Lifecycle handlers */

  def doConfigure()

  def doSetup()

  def doShutdown()

}
