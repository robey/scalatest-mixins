package com.twitter.scalatest

import sbt._
import Keys._

object ScalaTestMixins {
  val testSettings = Seq(
    test <<= (streams, scalaInstance, taskTemporaryDirectory, fullClasspath in Test, exportedProducts in Test) map { (s, scala, temp, fcp, tests) =>
      val classname = Option(System.getenv("SBT_TEST_FORMAT")).getOrElse("graph") match {
        case "graph" => "BarReporter"
        case "quiet" => "QuietReporter"
        case "verbose" => "TextReporter"
      }
      val testPaths = tests map { _.data.getAbsolutePath() }
      val args = Array("-p", testPaths.mkString(" "), "-r", "com.twitter.scalatest." + classname)

      val loader = TestFramework.createTestLoader(fcp.map(_.data), scala, IO.createUniqueDirectory(temp))
      val runner = "org.scalatest.tools.Runner"
      val rv = loader.loadClass(runner).getMethod("run", classOf[Array[String]]).invoke(null, args).asInstanceOf[Boolean]
      if (!rv) throw new Exception("Tests failed.")
    }
  )
}
