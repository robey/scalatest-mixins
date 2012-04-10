package com.twitter.scalatest

import java.lang.{ThreadLocal => JThreadLocal}
import org.scalatest._
import scala.collection.mutable

object Debugging {
  class DebuggedException(val log: Seq[String], cause: Throwable) extends Exception(cause)
}

trait Debugging extends AbstractSuite { self: Suite =>
  import Debugging._

  val debugLog = new mutable.ListBuffer[String]

  def debug(message: String) {
    debugLog += message
  }

  abstract override def withFixture(test: NoArgTest) {
    debugLog.clear()
    try {
      super.withFixture(test)
      // this line intentionally only happens if the test passes:
    } catch {
      case e: Throwable => {
        throw new DebuggedException(debugLog.toList, e)
      }
    } finally {
      debugLog.clear()
    }
  }
}
