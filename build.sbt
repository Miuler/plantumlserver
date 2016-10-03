name := """PlantUmlServer"""

version := "1.0a1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

libraryDependencies += "net.sourceforge.plantuml" % "plantuml" % "8047"
//libraryDependencies += "org.pegdown" % "pegdown" % "1.6.0"
libraryDependencies += "com.atlassian.commonmark" % "commonmark" % "0.7.0"
libraryDependencies += "com.atlassian.commonmark" % "commonmark-ext-autolink" % "0.7.0"
libraryDependencies += "com.atlassian.commonmark" % "commonmark-ext-gfm-tables" % "0.7.0"
libraryDependencies += "com.atlassian.commonmark" % "commonmark-ext-gfm-strikethrough" % "0.7.0"
libraryDependencies += "com.atlassian.commonmark" % "commonmark-ext-heading-anchor" % "0.7.0"


