/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

import io._
import collection._
import java.io.File
import actors.Actor
import Actor._
import TransactionsParser._
import mcgill.codejam.exchange.broker.Exchange._
import mcgill.codejam.exchange.broker.Transactions._

object BrokerMain {
  case object ShutdownBroker
  case class SuccessfulBook (order: Order, response: Response)
  case class FailedBook (order: Order)

  case object ClearOrderBook
  case class EnterOrder(refid: String, order: Order)
  case class HasOrder(refid: String)
  case object PrintBuys
  case object PrintSells
}
import BrokerMain._

trait BrokerMain {

  val bookKeeper = actor {
    val ordersBooked: mutable.Map[String, Order] = mutable.Map()
    loop {
      react {
        case ClearOrderBook => ordersBooked.clear()
        case EnterOrder(refid,order) => ordersBooked += refid -> order
        case HasOrder(refid) => reply { ordersBooked get refid }
        case PrintBuys => ordersBooked.values.filter( _.isInstanceOf[Buy] ).foreach( println )
        case PrintSells => ordersBooked.values.filter( _.isInstanceOf[Sell] ).foreach( println )
        case ShutdownBroker => exit()
      }
    }
  }

  val brokerMain = actor {
    loop {
      react {
        case SuccessfulBook(order, resp) => {
          resp match {
            case a @ Accept(orderRefId) => {
              bookKeeper ! EnterOrder(orderRefId, order)
              callbackOrderBooked(order, a)
            }
            case r: Reject => callbackOrderFailed(order, r)
          }
        }

        case FailedBook(order) => callbackOrderError(order)

        case exec: Execution => {
          bookKeeper !? HasOrder(exec.orderRefId) match {
            case Some(order: Order) => callbackOrderFilled(order, exec)
            case None => callbackUnassignedExecution(exec)
          }
        }

        case ShutdownBroker => exit()
      }
    }
  }

  def callbackOrderBooked(order: Order, resp: Accept)
  def callbackOrderFailed(order: Order, resp: Reject)
  def callbackOrderError(order: Order)
  def callbackOrderFilled(order: Order, exec: Execution)
  def callbackUnassignedExecution(exec: Execution)

  private def using (source: BufferedSource)(f: java.io.Reader => CompositeBook) = {
    try { f( source.reader() ) } finally { source.close() }
  }

  // why does this not exist in Scala? (or maybe it does and I haven't found it yet?)
  private def printAndReturn[T](x: Any)(ret: => T): T = { println(x) ; ret }

  // parse all data files and flatten them into one big list of orders
  def parseOrderFiles( files: Traversable[String] ): Map[File,Bookable] = {

    files.map( new File(_) ).map( f => (f, parseOrderFile(f)) ).toMap
  }

  def parseOrderFile( file: File ): CompositeBook = {

    print("Parsing input file: '%s' ... " format file)
    using (Source fromFile file) { inputStream =>
      TransactionsParser parse inputStream match {
        case Success(result: CompositeBook,_) => printAndReturn("Success!"){ result }
        case NoSuccess(msg,_) => printAndReturn("Error: " + msg){ EmptyBook }
      }
    }
  }

  def countOrders( orders: Traversable[Bookable] ): Int = {
    orders.foldLeft(0) { (sum,o) =>
      sum + (o match {
        case _: Order => 1
        case list: CompositeBook => countOrders(list)
      })
    }
  }

  def startServer (exchangeUrl: String, contextPath: String, serverPort: Int, numThreads: Int) = {

    val exchange = new DispatchExchange(brokerMain, exchangeUrl, contextPath, serverPort, numThreads)
    val server = new JettyServer(brokerMain, contextPath, serverPort)

    (exchange, server)
  }
}
