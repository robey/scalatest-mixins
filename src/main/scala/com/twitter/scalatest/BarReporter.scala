package com.twitter.scalatest

import org.scalatest.Reporter
import org.scalatest.events._
import scala.collection.mutable

/**
 * Common code to the other "pretty" reporters.
 */
trait SharedReporter extends Reporter {
  case class ErrorData(classname: Option[String], name: String, throwable: Option[Throwable])

  var total = 0
  var count = 0
  var failed = 0
  val errors = new mutable.ListBuffer[ErrorData]

  private[this] def allowAnsi = (System.console ne null)
  private[this] def color(c: Int) = if (allowAnsi) "\033[%dm".format(c) else ""

  val RED = color(31)
  val GREEN = color(32)
  val BOLD = color(1)
  val NORMAL = color(0)

  def durationToHuman(x: Long) = {
    val seconds = x / 1000
    "%d:%02d.%03d".format(seconds / 60, seconds % 60, x % 1000)
  }

  def buildStackTrace(throwable: Throwable, highlight: String, limit: Int): List[String] = {
    var out = new mutable.ListBuffer[String]
    out += throwable.toString
    if (limit > 0) {
      out ++= throwable.getStackTrace.map { elem =>
        val line = "    at %s".format(elem.toString)
        if (line contains highlight) {
          BOLD + line + NORMAL
        } else {
          line
        }
      }
      if (out.length > limit) {
        out.trimEnd(out.length - limit)
        out += "    (...more...)"
      }
    }
    if ((throwable.getCause ne null) && (throwable.getCause ne throwable)) {
      out += "Caused by %s".format(throwable.getCause.toString)
      out ++= buildStackTrace(throwable.getCause, highlight, limit)
    }
    out.toList
  }

  def dumpError(error: ErrorData) {
    println(RED + "FAILED" + NORMAL + ": " + error.name)
    val className = error.classname.getOrElse("")
    error.throwable.foreach {
      case e: Debugging.DebuggedException => {
        e.log.foreach { line => println("  + " + line) }
        buildStackTrace(e.getCause, className, 50).foreach(println)
      }
      case e => {
        buildStackTrace(e, className, 50).foreach(println)
      }
    }
    println()
  }
}

/**
 * Shut up and only print a single line if tests passed.
 */
class QuietReporter extends SharedReporter {
  def apply(event: Event) {
    event match {
      case RunStarting(ordinal, testCount, configMap, formatter, payload, threadName, timestamp) => {
        total = testCount
        count = 0
        failed = 0
      }
      case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
      }
      case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
        failed += 1
        errors += ErrorData(suiteClassName, "%s: %s".format(suiteName, testName), throwable)
      }
      case RunCompleted(ordinal, duration, summary, formatter, payload, threadName, timestamp) => {
        if (failed > 0) {
          errors.foreach { dumpError(_) }
          println()
          println(BOLD + "TESTS FAILED: %d/%d in %s".format(failed, count, durationToHuman(duration.get)) + NORMAL)
        } else {
          println("Tests passed: %d in %s".format(total, durationToHuman(duration.get)))
        }
      }
      case _ =>
    }
  }
}

/**
 * Display tests, one-per line, in a tree format. This is very similar to what sbt would normally
 * do, and is the most verbose reporter.
 */
class TextReporter extends SharedReporter {
  def display(indent: Int, s: String, args: Any*) {
    println((" " * indent * 2) + s.format(args: _*))
  }

  def display(formatter: Option[Formatter], good: Boolean = true) {
    formatter foreach {
      case IndentedText(formatted, raw, level) => {
        val text = if (good) ("- " + raw) else (RED + "X " + raw + NORMAL)
        display(level + 1, "%s", text)
      }
      case MotionToSuppress =>
    }
  }

  def apply(event: Event) {
    event match {
      case RunStarting(ordinal, testCount, configMap, formatter, payload, threadName, timestamp) => {
        total = testCount
        count = 0
        failed = 0
        println()
        println("Running %d tests:".format(total))
      }
      case SuiteStarting(ordinal, suiteName, suiteClassName, formatter, rerunner, payload, threadName, timestamp) => {
        println("+ " + suiteName)
      }
      case InfoProvided(ordinal, message, nameInfo, aboutAPendingTest, throwable, formatter, payload, threadName, timestamp) => {
        display(formatter)
      }
      case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
        display(formatter)
      }
      case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
        failed += 1
        display(formatter, false)
        errors += ErrorData(suiteClassName, "%s: %s".format(suiteName, testName), throwable)
      }
      case RunCompleted(ordinal, duration, summary, formatter, payload, threadName, timestamp) => {
        println()
        if (failed > 0) {
          errors.foreach { dumpError(_) }
          println()
          println(BOLD + "FAILED: %d/%d".format(failed, total) + NORMAL)
        }
        println("Finished in " + durationToHuman(duration.get))
        println()
      }
      case _ =>
    }
  }
}

/**
 * Display a pretty updating bar graph of tests as they run. Errors are displayed after the tests
 * are all finished.
 */
class BarReporter extends SharedReporter {
  val WIDTH = 60

  def apply(event: Event) {
    event match {
      case RunStarting(ordinal, testCount, configMap, formatter, payload, threadName, timestamp) => {
        total = testCount
        count = 0
        failed = 0
        println()
        println("Running %d tests:".format(total))
      }
      case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
        updateDisplay()
      }
      case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunner, payload, threadName, timestamp) => {
        count += 1
        failed += 1
        errors += ErrorData(suiteClassName, "%s: %s".format(suiteName, testName), throwable)
        updateDisplay()
      }
      case RunCompleted(ordinal, duration, summary, formatter, payload, threadName, timestamp) => {
        println()
        println("Finished in " + durationToHuman(duration.get))
        println()
        errors.foreach { dumpError(_) }
      }
      case _ =>
    }
  }

  def updateDisplay() {
    val hashes = (WIDTH * count.toDouble / total).toInt
    val bar = (if (failed > 0) RED else GREEN) + ("#" * hashes) + (" " * (WIDTH - hashes)) + NORMAL
    val note = if (failed > 0) "(errors: %d)".format(failed) else ""
    print("\r [%s] %d/%d %s ".format(bar, count, total, note))
  }
}
