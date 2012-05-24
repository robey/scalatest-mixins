import com.twitter.sbt._

seq((
  Project.defaultSettings ++
  StandardProject.newSettings ++
  SubversionPublisher.newSettings
): _*)

organization := "com.twitter"

name := "scalatest-mixins"

version := "1.0.5-SNAPSHOT"

crossScalaVersions := Seq("2.8.1", "2.9.2")

resolvers <<= resolvers { r => r ++ Seq(Classpaths.typesafeResolver) } 

libraryDependencies <<= (scalaVersion, libraryDependencies) { (sv, libs) =>
  val utilLogging = if (sv == "2.8.1") {
    "com.twitter" % "util-logging" % "4.0.1" % "provided"
  } else {
    "com.twitter" % "util-logging_2.9.1" % "4.0.1" % "provided"
  }
  libs ++ Seq(
    "org.scalatest" %% "scalatest" % "1.7.1" % "provided"
  ) :+ utilLogging
}

SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")

