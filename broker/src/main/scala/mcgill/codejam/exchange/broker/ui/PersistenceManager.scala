/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import java.util.Properties
import java.io.{FileOutputStream, FileInputStream, File}

object PersistenceManager {
  private lazy val propertyFile =
    new File(System.getProperty("user.home"), ".mcj2011.properties")

  private lazy val settings: Properties = {
    val props = new Properties
    if (propertyFile.exists) {
      props.load(new FileInputStream(propertyFile.getCanonicalPath))
    }
    props
  }

  private def saveSettings() {
    val out = new FileOutputStream(propertyFile)
    settings.store(out, null)
    out.close()
  }

  def getSetting(name: String,  default: String) =
    Option(settings.getProperty(name)) getOrElse default

  def saveSetting(name: String, value: String) {
    settings.setProperty(name, value)
    saveSettings()
  }
}

