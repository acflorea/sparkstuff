import play.api.{Application, GlobalSettings}

/**
 * App setup
 */
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    super.onStart(app)
    // setup services
  }

  override def onStop(app: Application): Unit = {
    super.onStop(app)
  }

}
