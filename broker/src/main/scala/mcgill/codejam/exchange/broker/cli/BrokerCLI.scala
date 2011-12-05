/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.cli

import collection._
import java.io.File
import actors.Actor._
import mcgill.codejam.exchange.broker._

import Exchange._
import BrokerMain._
import mcgill.codejam.exchange.broker.Transactions._

object BrokerCLI extends App with BrokerMain {

  if ( args.length < 5 ) {
    sys.error( "Expecting 5+ parameters: the exchange URL, the broker context path, the server port, the number of threads and the transactions file(s) to process" )
  }

  // parse command line: first parameter is the URL, then server port, then thread count
  // rest are an array of data files to process
  val (exchangeUrl, contextPath, serverPort, threads, files) = (args(0), args(1), args(2).toInt, args(3).toInt, args drop 4)

  // parse all data files and store in a mutable Map, keyed by file
  val ordersMap = mutable.Map( parseOrderFiles( files ).toSeq : _* )

  // deep-search orders list to count the number of actual orders in all contained collections
  var totalOrders, totalOrdersSent = 0

  val executions = new mutable.ListBuffer[Execution]
  val badExecutions = new mutable.ListBuffer[Execution]
  val trades = mutable.Map.empty[Int,Trade]

  var startTime: Long = _

  def bookTo (exchange: Exchange) {

    bookKeeper ! ClearOrderBook
    startTime = System.currentTimeMillis
    val orders = ordersMap.values
    totalOrdersSent = 0
    totalOrders = countOrders(orders)
    executions.clear()
    badExecutions.clear()
    trades.clear()
    for (order <- orders) order bookTo exchange
  }

  def checkIfDone {
    if ( totalOrdersSent == totalOrders ) {
      println("%s orders booked in %s ms" format (totalOrders, System.currentTimeMillis - startTime))
      println("Done!\nWaiting for data file changes... (press enter to run again)")
    }
  }

  def callbackOrderBooked(order: Order, resp: Accept) {
    totalOrdersSent += 1
    println("%s/%s Response for %s: %s" format (totalOrdersSent,totalOrders,order,resp))

    // check if executions came in before we managed to get this confirmation id, if yes move to the 'filled' table
    // find all bad executions that have the passed-in order ref id
    val unassigned = badExecutions filter ( _.orderRefId == resp.orderRefId )

    // remove them all
    badExecutions --= unassigned

    // mark those as filled now
    unassigned foreach { callbackOrderFilled(order, _) }
    if (unassigned.nonEmpty) println("moved %s executions" format unassigned.size )

    checkIfDone
  }

  def callbackOrderFailed(order: Order, resp: Reject) {
    totalOrdersSent += 1
    println("%s/%s ERROR booking order '%s': %s" format (totalOrdersSent,totalOrders,order,resp.reason))
    checkIfDone
  }

  def callbackOrderError(order: Order) {
    totalOrdersSent += 1
    println("Invalid HTTP reply, expecting <Accept OrderRefId=> or <Reject Reason=> node under <Exchange>")
    checkIfDone
  }

  def callbackOrderFilled(order: Order, exec: Execution) {
    println("-> Notification to %s for order %s: %s shares at %s, match #%s" format (exec.to,exec.orderRefId,exec.shares,exec.price,exec.matchNb))

    executions += exec
    trades get exec.matchNb match {
      case Some(trade) => trade setSide (order,exec)
      case None => {
        val trade = new Trade (exec.matchNb)
        trade setSide (order, exec)
        trades += exec.matchNb -> trade
      }
    }
  }

  def callbackUnassignedExecution(exec: Execution) {
    println("Received notify response with unexpected orderRefId: " + exec)
    badExecutions += exec
  }

  // CLI-specific actor logic
  actor {
    val input = new UserInput(self)

    // start a file watcher over the data files, to receive notifications when they are modified
    val fw = new FileWatcher (self, files map {new File(_)})

    // start Jetty http server to listen for execute notifications
    val (exchange,server) = startServer(exchangeUrl, contextPath, serverPort, threads)

    // book all orders to the exchange
    bookTo(exchange)

    loop {
      react {
        case fw.ModifiedFile(file) => {
          println("Detected modified data file: " + file)
          ordersMap += (file -> parseOrderFile(file))
          bookTo(exchange)
        }
        case input.Enter => bookTo(exchange)
        case input.Buys => bookKeeper ! PrintBuys
        case input.Sells => bookKeeper ! PrintSells
        case input.Trades => trades.values.toSeq.sortBy( _.matchNb ).foreach( println )
        case input.Executions => executions.sortBy( _.matchNb ).foreach( println )
        case input.BadExecutions => badExecutions.sortBy( _.matchNb ).foreach( println )
        case input.Quit => { 
          server.stop()
          exchange.close()
          brokerMain ! ShutdownBroker
          bookKeeper ! ShutdownBroker
          exit()
        }
      }
    }
  }
}
