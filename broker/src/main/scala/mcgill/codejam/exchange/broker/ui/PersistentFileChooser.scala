/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import swing.FileChooser._
import java.io.File
import swing.{Component, FileChooser}

class PersistentFileChooser(val settingName: String, val defaultSettingValue: String)
  extends FileChooser with PersistentSettings {

  def setLastDirectory() {
    val lastDir = {
      val dir = new File(getSetting)
      if (dir.isDirectory) {
        dir
      } else {
        // Current setting or default didn't work
        // fall back to current folder
        new File(System.getProperty("user.dir"))
      }
    }
    peer.setCurrentDirectory(lastDir)
  }

  def showOpenDialogWithLastDirectory(over: Component): Result.Value = {
    setLastDirectory()
    val result = super.showOpenDialog(over)
    persistLastDirectory()
    result
  }

  override def showOpenDialog(over: Component): Result.Value = {
    val result = super.showOpenDialog(over)
    persistLastDirectory()
    result
  }

  private def persistLastDirectory() {
    Option(selectedFile) match {
      case None =>
      case Some(file) =>
        val dir = if (file.isDirectory) file else file.getParentFile
        if (dir.exists()) {
          println("Saving FileChooser lastDirectory: " + dir)
          saveSetting(dir.getAbsoluteFile.getPath)
        }
    }
  }
}