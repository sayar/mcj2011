/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import swing.{Table, Label, Alignment}
import swing.Table.AbstractRenderer
import java.text.SimpleDateFormat

object TableRenderers {

  class CustomAlignedLabel(alignment: Alignment.Value) extends Label {
    def prepare(s: String) {
      text = s
      horizontalAlignment = alignment
      val font = peer.getFont
      peer.setFont(font.deriveFont(0))
    }
  }

  class CustomAlignedLabelRenderer(c: CustomAlignedLabel) extends AbstractRenderer[String, CustomAlignedLabel](c) {
    def configure(table: Table, isSelected: Boolean, hasFocus: Boolean, a: String, row: Int, column: Int) {
      component.prepare(a)
    }
  }

  val centeredLabelRenderer = new CustomAlignedLabelRenderer(new CustomAlignedLabel(Alignment.Center))

  val rightLabelRenderer = new CustomAlignedLabelRenderer(new CustomAlignedLabel(Alignment.Right))
}