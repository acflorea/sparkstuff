name := "sparkstuff"

version := "1.0"

lazy val `sparkstuff` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(jdbc, anorm, cache, ws)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.5.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.14",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.14"
)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")