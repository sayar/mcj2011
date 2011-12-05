/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

import dispatch._
import actors.Actor
import Exchange._
import BrokerMain._
import Transactions._
import java.net.InetAddress

/*
From the docs, we should send this:

----------
POST /exchange/endpoint HTTP/1.1
Host: localhost
User-Agent: Mozilla/4.0
Content-Length: 70
Content-Type: application/x-www-form-urlencoded

MessageType=O&From=%2B15145551212&BS=B&Shares=100&Stock=XYZ&Price=9205&Twilio=N&BrokerAddress=192%2E168%2E2%2E4&BrokerPort=8888&BrokerEndPoint=broker%2Fendpoint
----------

And receive this:

----------
<?xml version="1.0" encoding="UTF-8"?>
<Response>
<Exchange>[Response]</Exchange>
</Response>
----------
*/
class DispatchExchange (main: Actor,
                        val exchangeUrl: String,
                        val contextPath: String,
                        val brokerPort: Int,
                        val numThreads: Int,
                        verbose: Boolean = false)
  extends ExchangeActor (numThreads) {

  val brokerAddress = InetAddress.getLocalHost.getHostAddress

  trait ConfigurableLogger extends RequestLogging {
    private val logger = super.make_logger
    override def make_logger = new Logger {
      def info (msg: String, items: Any*) { if ( verbose ) logger.info(msg, items) }
      def warn (msg: String, items: Any*) { logger.warn(msg, items) }
    }
  }

  /* a reusable Thread-safe HttpClient instance */
  lazy val http = new Http with ConfigurableLogger with thread.Safety

  lazy val postRequest = url(exchangeUrl).POST

  override def close() {
    super.close()
    http.shutdown()
  }

  // this is done synchronously, so need to wrap the call to book in a Futures.future() call
  def book (order: Order) {

    println( "Booking order: %s ..." format order )
    if ( verbose ) println("Using URL " + exchangeUrl)

    val buyOrSellIndicator = order match {
      case b: Buy => "B"
      case s: Sell => "S"
    }

    val params = Map(
      "MessageType" -> "O",
      "BS" -> buyOrSellIndicator,
      "From" -> order.phone,
      "Shares" -> order.qty.toString,
      "Stock" -> order.stock,
      "Price" -> ( order.price * 100 ).toInt.toString,
      "Twilio" -> { if (order.twillio) "Y" else "N" },
      "BrokerAddress" -> brokerAddress,
      "BrokerPort" -> brokerPort.toString,
      "BrokerEndpoint" -> contextPath
    )

    if ( verbose ) println("Params: " + (params.map { case (k,v) => k + "=" + v }.mkString("&")))
    val request = postRequest << params

    try {
      // Verb '<>' triggers the XML parsing handler, '\\' is the xpath query syntax
      // We get every node under <Exchange> (through the \"_" xpath) which yields a node Sequence
      main ! http(request <> { _ \\ "Exchange" \ "_" match {
          case Seq(elem @ <Accept/>) => SuccessfulBook(order, Accept( (elem \ "@OrderRefId").text ))
          case Seq(elem @ <Reject/>) => SuccessfulBook(order, Reject( (elem \ "@Reason").text(0) ))
          case invalidReply => FailedBook(order)
        }
      })

    } catch {
      case error =>
        println("Exception sending HTTP message to exchange: " + error)
        main ! FailedBook(order)
    }
  }
}
