import com.twitter.sbt._

seq((
  Project.defaultSettings ++
  StandardProject.newSettings ++
  SubversionPublisher.newSettings
): _*)

organization := "com.twitter"

name := "scalatest-mixins"

version := "1.1.1-SNAPSHOT"

scalaVersion := "2.9.1"

crossPaths := true

resolvers <<= resolvers { r => r ++ Seq(Classpaths.typesafeResolver) }

libraryDependencies ++= Seq(
  "com.twitter" % "util-logging" % "5.0.3" % "provided",
  "org.scalatest" %% "scalatest" % "1.8" % "provided"
)

SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public")
