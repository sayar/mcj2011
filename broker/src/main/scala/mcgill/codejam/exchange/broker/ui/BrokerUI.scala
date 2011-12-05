/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import java.io.File
import swing._
import event.TableRowsSelected
import mcgill.codejam.exchange.broker._
import Exchange._
import BrokerMain._
import Transactions._
import scala.actors.Actor
import javax.swing.JOptionPane._

object BrokerUI extends SimpleSwingApplication with BrokerMain with Actor {

  // the only message this actor accepts
  case object Book

  // This is used for SwingUtilities.invoke*
  implicit def fun2Run[T](x: => T) = new Runnable { def run() { x } }

  private var server: Option[DispatchExchange] = None
  private var jetty: Option[JettyServer] = None
  private var orders: CompositeBook = _
  private[ui] val ordersTable = new OrderTable
  private[ui] val executionsTable = new ExecutionsTable
  private[ui] val badExecutionsTable = new ExecutionsTable

  val top = new BrokerUIMainFrame

  listenTo(ordersTable.selection)

  reactions += {
    case TableRowsSelected(source: OrderTable,_,false) => executionsTable.selectedRefIds = source.selectedRefIds
  }

  start() // start our actor

  def instantiateServer(exchangeHost: String, jettyPort: Int, threads: Int) {

    // check if any of the settings have changed, if so we need to start a new server instance
    val identicalSettings = Seq(
      jetty.exists( _.port == jettyPort ),
      server.exists( _.exchangeUrl == exchangeHost ),
      server.exists( _.numThreads == threads )
    )

    if ( identicalSettings exists ( _ == false ) ) {
      // first stop the existing server
      jetty foreach (_ stop())
      server foreach (_ close())

      // then start a new instance
      println("Initialising new server using %s / %s" format (exchangeHost, jettyPort))
      val (s,j) = startServer(exchangeHost, "/broker", jettyPort, threads)
      server = Some(s)
      jetty = Some(j)
    }
  }

  def act() { loop { react { case Book => book() } } }

  def book() {
    try {
      server foreach (orders bookTo _)

    } catch {
      case _: IllegalArgumentException =>
        showMessageDialog(null, "You must provide a valid url.", "Bad url", ERROR_MESSAGE)
      case e =>
        showMessageDialog(null, "Error occured: " + e, "Error", ERROR_MESSAGE)
    }
  }

  def close() {
    server foreach (_ close())
    jetty foreach (_ stop())
    brokerMain ! ShutdownBroker
    quit()
  }

  def reset() {
    bookKeeper ! ClearOrderBook
    ordersTable.reset()
    executionsTable.reset()
    badExecutionsTable.reset()
  }

  def loadFile(file: File) {
    orders = parseOrderFile(file)
    ordersTable.setOrders(orders)
  }

  def callbackOrderBooked(order: Order, resp: Accept) {
    val refid = resp.orderRefId
    println("Order %s accepted. Order ref id %s" format ( order, refid ))
    ordersTable accept (order, resp)

    // check if executions came in before we managed to get this confirmation id, if yes move to the 'filled' table
    val unassigned = badExecutionsTable remove refid
    unassigned foreach { callbackOrderFilled(order, _) }
    if (unassigned.nonEmpty)println("moved %s executions" format unassigned.size )
  }

  def callbackOrderFailed(order: Order, resp: Reject) {
    println("Order %s rejected... reason: %s" format (order, resp.reason))
    ordersTable reject order
  }

  def callbackOrderError(order: Order) {
    println("Order %s failed to send" format order)
    ordersTable reject order
  }

  def callbackOrderFilled(order: Order, execution: Execution) {
    println("Order %s filled: %s" format (order, execution))
    ordersTable execute (order, execution)
    executionsTable add execution
  }

  def callbackUnassignedExecution(execution: Execution) {
    println("Unassigned execution: " + execution)
    badExecutionsTable add execution
  }
}
