import com.twitter.sbt._

seq((
  Project.defaultSettings ++
  StandardProject.newSettings ++
  SubversionPublisher.newSettings
): _*)

organization := "com.twitter"

name := "scalatest-mixins"

version := "1.0.3"

resolvers <<= resolvers { r => r ++ Seq(Classpaths.typesafeResolver) } 

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.7.1" % "provided",
  "com.twitter" %% "util-logging" % "2.0.0" % "provided",
  "org.scala-tools.sbt" %% "sbt" % "0.11.2" % "provided"
)

SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")

