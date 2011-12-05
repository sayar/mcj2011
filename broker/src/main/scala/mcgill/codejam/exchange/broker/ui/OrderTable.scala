/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/
package mcgill.codejam.exchange.broker.ui

import mcgill.codejam.exchange.broker.Transactions._
import java.util.IdentityHashMap
import collection.JavaConversions._
import collection.mutable
import mcgill.codejam.exchange.broker.Exchange._
import swing._
import javax.swing.SwingUtilities.invokeLater
import BrokerUI.fun2Run
import javax.swing.table.TableModel
import javax.swing.BorderFactory
import java.awt.Color
import swing.Table.AbstractRenderer
import TableRenderers.{centeredLabelRenderer, rightLabelRenderer}

class OrderTable extends Table {

  val orderRows = new IdentityHashMap[Order,Int]
  val orderExecutions = mutable.Map.empty[String,  Seq[Execution]]
  val executionColor = mutable.Map.empty[Int, Color]

  val columnNames = Seq("Type", "Stock", "Qty", "Price", "Twillio", "Phone", "RefId", "Accepted", "Rejected", "Executed")
  val TYPE_COL = columnNames.indexOf("Type")
  val STOCK_COL = columnNames.indexOf("Stock")
  val QTY_COL = columnNames.indexOf("Qty")
  val PRICE_COL = columnNames.indexOf("Price")
  val TWILLIO_COL = columnNames.indexOf("Twillio")
  val PHONE_COL = columnNames.indexOf("Phone")
  val REFID_COL = columnNames.indexOf("RefId")
  val ACCEPT_COL = columnNames.indexOf("Accepted")
  val REJECT_COL = columnNames.indexOf("Rejected")
  val FILLED_COL = columnNames.indexOf("Executed")
  val checkboxColumns = Seq(ACCEPT_COL, REJECT_COL, FILLED_COL)

  showGrid = false

  border = BorderFactory.createLineBorder(Color.black)

  model = new BrokerTableModel(Array.ofDim[Any](0, columnNames.size - 1), columnNames)

  peer.setFillsViewportHeight(true)

  selection.elementMode = Table.ElementMode.Row
  selection.intervalMode = Table.IntervalMode.SingleInterval

  def selectedRefIds: Seq[String] = {

    def row2refid (row: Int) = {
      model.getValueAt(row, REFID_COL) match {
        case "" => None
        case s: String => Some(s)
      }
    }

    peer.getSelectedRows.map(row2refid).toSeq.flatten
  }

  def order2Array(o: Order): Array[Any] = o match {
    case Buy(qty, stock, price, phone, twillio) =>
      Array("Buy", stock, qty, price.toString, twillio, phone, "", false, false, false)
    case Sell(qty, stock, price, phone, twillio) =>
      Array("Sell", stock, qty, price.toString, twillio, phone, "", false, false, false)
  }

  def flattenOrders(orders: CompositeBook): List[Order] = {
    orders.foldRight (List.empty[Order]) { (x,xs) =>
      x match {
        case o: Order => o :: xs
        case ys: CompositeBook => flattenOrders(ys) ++ xs
      }
    }
  }

  override def model_=(model: TableModel) {
    super.model_=(model)
    setColumnSizes()
  }

  def setOrders(orders: CompositeBook) {
    orderRows.clear()

    val flattenedOrders = flattenOrders(orders)

    // Cannot find a Scala way of doing this: a regular Map keyed by Order will not allow duplicate orders
    // We need something which considers object identity as the key, not object equality
    // Hence Java's IdentityHashMap
    flattenedOrders.zipWithIndex.foreach { case (o,i) => orderRows put (o,i) }

    val ordArray: Array[Array[Any]] = (flattenedOrders map order2Array).toArray
    model = new BrokerTableModel(ordArray, columnNames)
  }

  class ColoredCheckbox extends CheckBox {
    def prepare(a: Boolean, row: Int) {
      selected = executionColor.contains(row)
      background = executionColor.getOrElse(row, Color.white)
      horizontalAlignment = Alignment.Center
    }
  }

  val coloredCheckboxRenderer = new AbstractRenderer[Boolean, ColoredCheckbox](new ColoredCheckbox) {
    def configure(table: Table, isSelected: Boolean, hasFocus: Boolean, a: Boolean, row: Int, column: Int) {
      component.prepare(a, row)
    }
  }

  override protected def rendererComponent(selected: Boolean, focused: Boolean, row: Int, column: Int) = {
    val value = model.getValueAt(
      peer.convertRowIndexToModel(row),
      peer.convertColumnIndexToModel(column)
    )

    column match {
      case TYPE_COL | STOCK_COL =>
        centeredLabelRenderer.componentFor(this, selected, focused, value.toString, row, column)
      case FILLED_COL =>
        coloredCheckboxRenderer.componentFor(this, selected, focused, value.asInstanceOf[Boolean], row, column)
      case PRICE_COL | REFID_COL =>
        rightLabelRenderer.componentFor(this, selected, focused, value.toString, row, column)
      case _ => super.rendererComponent(selected, focused, row, column)
    }
  }

  def execute(order: Order, execution: Execution) {
    val row = orderRows.get(order)
    val refId = execution.orderRefId
    val executions: Seq[Execution] = orderExecutions.getOrElse(refId, Seq()) :+ execution
    orderExecutions += refId -> executions

    // check how many shares we have, and alter color accordingly
    val totalSharesFilled = executions.foldLeft (0) { (sum,x) => sum + x.shares }
    executionColor(row) =
      if ( totalSharesFilled == order.qty ) Color.green
      else if ( totalSharesFilled > order.qty ) Color.red
      else if ( totalSharesFilled == 0 ) Color.white
      else Color.yellow

    invokeLater { model.setValueAt(true, row, FILLED_COL) }
  }

  def accept(order: Order, accept: Accept) {
    val row = orderRows get order
    invokeLater {
      model.setValueAt(true, row, ACCEPT_COL)
      model.setValueAt(accept.orderRefId, row, REFID_COL)
    }
  }

  def reject(order: Order) {
    invokeLater { model.setValueAt(true, orderRows get order, REJECT_COL) }
  }

  def reset() {
    peer.clearSelection()
    orderExecutions.clear()
    executionColor.clear()
    for ( row <- orderRows.values.iterator ) {
      model.setValueAt("", row, REFID_COL)
      for ( n <- checkboxColumns ) model.setValueAt(false, row, n)
    }
  }

  protected def setColumnSizes() {
    val cm = peer.getColumnModel
    Seq(
      (TYPE_COL, 50),
      (STOCK_COL, 60),
      (QTY_COL, 70),
      (PRICE_COL, 70),
      (TWILLIO_COL, 50),
      (REFID_COL, 80),
      (ACCEPT_COL, 65),
      (REJECT_COL, 65),
      (FILLED_COL, 65)
    ) foreach { case (i, width) =>
      cm.getColumn(i).setMaxWidth(width)
    }
  }
}

