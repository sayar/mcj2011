/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

object Transactions {

  trait Bookable {
    def bookTo (exchange: Exchange)
  }

  sealed trait Order extends Bookable {
    def bookTo (exchange: Exchange) { exchange book this }
    
    def qty: Int
    def stock: String
    def price: Double
    def phone: String
    def twillio: Boolean

  }
  case class Buy  (qty: Int, stock: String, price: Double, phone: String, twillio: Boolean) extends Order
  case class Sell (qty: Int, stock: String, price: Double, phone: String, twillio: Boolean) extends Order

  abstract class CompositeBook extends Bookable with Iterable[Bookable] {
    val orders: Seq[Bookable] // abstract
    def iterator = orders.iterator
  }

  case class SeqBook (orders: Seq[Bookable]) extends CompositeBook {
    def bookTo (exchange: Exchange) { orders foreach (_ bookTo exchange) }
  }

  case class ParallelBook (orders: Seq[Bookable]) extends CompositeBook {
    def bookTo (exchange: Exchange) {
      orders foreach ( _ match {
        // send all orders to the exchange channel asynchronously (using !)
        case order: Order => exchange ! order

        // composite orders (SeqBook/ParallelBook), we iterate through normally by calling their bookTo() fct
        case other: Bookable => other bookTo exchange
      })
    }
  }

  case object EmptyBook extends CompositeBook {
    val orders = Nil
    def bookTo (exchange: Exchange) {}
  }
}
