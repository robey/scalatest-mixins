/*
 * Copyright 2012 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.scalatest

import java.io.File
import org.scalatest._

/**
 * Creates a temporary folder for the lifetime of a single test.
 * The folder's name will exist in a `File` field named `testFolder`.
 */
trait TestFolder extends AbstractSuite { self: Suite =>
  var testFolder: File = _

  private def deleteFile(file: File) {
    if (!file.exists) return
    if (file.isFile) {
      file.delete()
    } else {
      file.listFiles().foreach(deleteFile)
      file.delete()
    }
  }

  abstract override def withFixture(test: NoArgTest) {
    val tempFolder = System.getProperty("java.io.tmpdir")
    var folder: File = null

    do {
      folder = new File(tempFolder, "scalatest-" + System.nanoTime)
    } while (! folder.mkdir())

    testFolder = folder

    try {
      super.withFixture(test)
    } finally {
      deleteFile(testFolder)
    }
  }
}
