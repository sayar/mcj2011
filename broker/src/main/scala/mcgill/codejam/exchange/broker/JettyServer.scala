/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

import actors._
import javax.servlet.http._
import org.eclipse.jetty.server._
import handler._
import Exchange.Execution
import org.eclipse.jetty.io.UncheckedIOException

class JettyServer (brokerMain: Actor, val contextPath: String,  val port: Int) {

  println( "Starting Jetty server on port " + port )

  val server = new Server(port)
  server.setHandler(new ExchangeResponseHandler)
  server.start()

  def stop() { server.stop() }

  class ExchangeResponseHandler extends AbstractHandler {

    def parseResponse (request: HttpServletRequest) = {

      def isInteger (s: String, digits: Int) = s.matches("""\d+""") && s.length <= digits

      // Map keyed by parameter name, containing a tuple with a validation function and an error message template
      val params: Map[String, (String => Boolean, String)] = Map(
        "MessageType" ->              ( { s: String => s != "E" },                       "Expected %s 'E'" ),
        "OrderReferenceIdentifier" -> ( { s: String => s.length > 8 || s.length == 0 },  "%s exceeded maximum length or is blank" ),
        "ExecutedShares" ->           ( { s: String => !isInteger(s,6) || s.toInt < 0 }, "%s must be a positive 6-digit integer" ),
        "ExecutionPrice" ->           ( { s: String => !isInteger(s,6) || s.toInt < 0 }, "%s must be a positive 6-digit integer" ),
        "MatchNumber" ->              ( { s: String => !isInteger(s,9) },                "%s must be a 9-digit integer" ),
        "To" ->                       ( { s: String => false },                          "** no validation required **" )
      )

      // get the parameter, perform validation checks, and convert to a simple key-value map
      // after this call all parameters are either valid or an exception has been thrown
      val values: Map[String,String] = params map {
        case (name, (isInvalidValue, errorMessage)) =>
          val value = request getParameter name
          if ( value == null ) sys.error(name + " parameter is empty")
          else if ( isInvalidValue(value) ) sys.error(errorMessage format name)
          else (name -> value)
      }

      new Execution(
        values("MessageType")(0),
        values("OrderReferenceIdentifier"),
        values("ExecutedShares").toInt,
        values("ExecutionPrice").toInt / 100.0,
        values("MatchNumber").toInt,
        values("To"),
        System.currentTimeMillis
      )
    }

    val acceptResponse = """
      |<?xml version="1.0" encoding="UTF-8"?>
      |<Response>
      |<Broker><Accept /></Broker>
      |</Response>
      """.stripMargin.trim

    val errorResponse = """
      |<?xml version="1.0" encoding="UTF-8"?>
      |<Response>
      |<Broker><Reject>%s</Reject></Broker>
      |</Response>
      """.stripMargin.trim

    def respond (template: String, status: Int, details: Option[String])(response: HttpServletResponse) {
      val s = template format details.getOrElse("")
      response setStatus status
      response setContentType "text/xml; charset=UTF-8"
      response setContentLength (s.length+1)
      response.getWriter.println( s )
    }

    def handle (target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {

      try {
        // Do not change the order of these statements below. A bug in Jetty calls this handler twice.
        // The second time after the channel has been closed.
        baseRequest setHandled true
        // need to do this here because parsing the request might throw an exception
        // we want that exception returned to the user
        val execution = parseResponse(request)

        // this will throw UncheckedIOException when it is called a second time
        respond(acceptResponse, 201, None)(response)

        // return the response to the main broker actor
        brokerMain ! execution

      } catch {
        case _: UncheckedIOException =>
          println("Warning! You have hit John's Well of Infinite Weird Duplicating Bugs. If you're not John, call us so we can reproduce this!")
        case e: Exception =>
          respond(errorResponse, 400, Some(e.getMessage))(response)
      }
    }
  }
}