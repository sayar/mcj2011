/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

import actors._
import Actor._
import Transactions.Order

object Exchange {
  sealed trait Response
  case class Accept (orderRefId: String) extends Response
  case class Reject (reason: Char) extends Response

  case class Execution (messageType: Char, orderRefId: String, shares: Int, price: Double, matchNb: Int, to: String, timeStamp: Long)
}

trait Exchange extends OutputChannel[Order] {
  def book (order: Order)
  def close()
}

abstract class ExchangeActor (numThreads: Int) extends Exchange with Actor {

  case object Stop
  case class Ready(sender: Actor)
  case class Book(order: Order)
  case object PrintTrades

  class Trader (n: Int) extends Actor {
    def act() {
      loop {
        react {
          case Ready(sender) => sender ! Ready(self)
          case Stop => exit()
          case Book(order) => {
            println("Trader #%s is processing order %s" format (n,order))
            book(order)
          }
        }
      }
    }

    start()
  }

  val traders = for ( i <- 1 to numThreads ) yield new Trader(i)

  def close() {
    traders foreach { _ ! Stop }
    this ! Stop
  }

  def act() {
    loop {
      react {
        case order: Order =>
          traders foreach { _ ! Ready(self) }
          react {
            // send the request to the first available consumer
            case Ready(trader) => trader ! Book(order)
          }
        case Stop => exit()
        case PrintTrades => {}
      }
    }
  }

  start()
}