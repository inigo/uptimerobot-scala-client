name := """bricks-uptimerobot"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % "3.8" % "test"
  , "org.specs2" %% "specs2-mock" % "3.8" % "test"
  , "com.typesafe" % "config" % "1.3.0" % "test"
  , "org.slf4j" % "slf4j-simple" % "1.7.21" % "test"

  , "org.slf4j" % "slf4j-api" % "1.7.21"
  , "org.apache.httpcomponents" % "httpclient" % "4.5.2"
  , "org.apache.httpcomponents" % "fluent-hc" % "4.5.2"
  , "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
)
