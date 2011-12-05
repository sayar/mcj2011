/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/
package mcgill.codejam.exchange.broker.ui

import collection._
import swing.Table
import javax.swing.BorderFactory
import java.awt.Color
import mcgill.codejam.exchange.broker.Exchange._
import javax.swing.table.TableModel
import javax.swing.SwingUtilities.invokeLater
import BrokerUI.fun2Run
import TableRenderers.{centeredLabelRenderer, rightLabelRenderer}

class ExecutionsTable extends Table {

  val columnNames = Seq("TimeStamp", "RefId", "Shares", "Price", "Match #", "To")
  val TIMESTAMP_COL = columnNames.indexOf("TimeStamp")
  val REFID_COL = columnNames.indexOf("RefId")
  val SHARES_COL = columnNames.indexOf("Shares")
  val PRICE_COL = columnNames.indexOf("Price")
  val MATCH_NUM_COL = columnNames.indexOf("Match #")
  val TO_PHONE_COL = columnNames.indexOf("To")

  val executions = mutable.Map.empty[Execution,Array[Any]]
  val filtered = executions.view.filter { case (e,_) => refIds.isEmpty || refIds.contains( e.orderRefId ) }
  private var refIds: Seq[String] = Nil

  showGrid = false
  peer.setFillsViewportHeight(true)
  border = BorderFactory.createLineBorder(Color.black)

  // sorted executions array takes the value of the map, converts it to an array then sorts
  private def sortedExecutions = filtered.map( t => t._2 ).toArray.sortWith(sortArrays)
  private def sortArrays (a: Array[Any], b: Array[Any]) = {
    ( a(MATCH_NUM_COL), b(MATCH_NUM_COL) ) match {
      case (x: Int, y: Int) => x < y
      case (c, d) => c.toString < d.toString
    }
  }

  override def model_=(model: TableModel) {
    super.model_=(model)
    setColumnSizes()
  }

  def selectedRefIds = refIds
  def selectedRefIds_= (ids: Seq[String]) {
    refIds = ids
    rebuildModel()
  }

  def add (e: Execution) {
    val refid = e.orderRefId
    val date = new java.util.Date(e.timeStamp)
    val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    val timeStampString = dateFormatter.format(date)
    executions += e -> Array(timeStampString, refid, e.shares.toString, e.price.toString, e.matchNb, e.to)
    rebuildModel()
  }

  def remove (refid: String): Seq[Execution] = {

    // find all executions that have the passed-in order ref id
    val matches = executions.keys filter (_.orderRefId == refid)

    // remove them all
    executions --= matches

    // update table if we removed something
    if ( matches.nonEmpty ) rebuildModel()

    // then return what was removed
    matches.toSeq
  }

  def rebuildModel() {
    invokeLater { model = new BrokerTableModel(sortedExecutions, columnNames) }
  }

  def reset() {
    refIds = Nil
    executions.clear()
    rebuildModel()
  }

  override protected def rendererComponent(selected: Boolean, focused: Boolean, row: Int, column: Int) = {
    val value = model.getValueAt(
      peer.convertRowIndexToModel(row),
      peer.convertColumnIndexToModel(column)
    )

    column match {
      case TIMESTAMP_COL =>
        centeredLabelRenderer.componentFor(this, selected, focused, value.toString, row, column)
      case PRICE_COL | REFID_COL | SHARES_COL =>
        rightLabelRenderer.componentFor(this, selected, focused, value.toString, row, column)
      case _ => super.rendererComponent(selected, focused, row, column)
    }
  }

  protected def setColumnSizes() {
    val cm = peer.getColumnModel
    Seq(
      (TIMESTAMP_COL, 240),
      (REFID_COL, 100),
      (SHARES_COL, 100),
      (PRICE_COL, 100),
      (MATCH_NUM_COL, 100)
    ) foreach { case (i, width) =>
      cm.getColumn(i).setMaxWidth(width)
      cm.getColumn(i).setPreferredWidth(width)
    }
    cm.getColumn(TIMESTAMP_COL).setPreferredWidth(190)
  }
}

