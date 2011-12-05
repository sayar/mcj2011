/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import swing.event.EditDone
import swing.TextField

class PersistentTextField(val settingName: String, val defaultSettingValue: String)
  extends TextField with PersistentSettings {

  listenTo(this)

  println("Loading " + settingName)
  text = getSetting

  override def text_=(t: String) {
    saveSetting(t)
    super.text_=(t)
  }

  reactions += {
    case EditDone(src) =>
      saveSetting(src.text)
  }
}