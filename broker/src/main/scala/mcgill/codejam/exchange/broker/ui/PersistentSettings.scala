/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

trait PersistentSettings {

  val settingName: String
  val defaultSettingValue: String

  def getSetting = PersistenceManager.getSetting(settingName, defaultSettingValue)

  def saveSetting(value: String) {
    PersistenceManager.saveSetting(settingName, value)
  }
}

