/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import java.lang.{Object => JObject}
import javax.swing.table.DefaultTableModel

class BrokerTableModel(var rowData: Array[Array[Any]], val columnNames: Seq[String])
  extends DefaultTableModel(
    rowData.asInstanceOf[Array[Array[JObject]]],
    columnNames.toArray.asInstanceOf[Array[JObject]]
  ) {
  override def isCellEditable(row: Int, column: Int) = false
}
