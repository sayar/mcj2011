/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.mock

import java.io._
import dispatch._
import collection._
import actors.Actor._
import actors.TIMEOUT
import actors.Futures._
import java.util.concurrent.atomic.AtomicInteger
import java.net.ServerSocket

object MockExchangeServer extends App {

  val headerPattern = """
    |HTTP/1.1 200 OK
    |Content-Length: %s
    |Content-Type: text/xml; charset=UTF-8
    """.stripMargin.trim

  val responsePattern = """
    |<?xml version="1.0" encoding="UTF-8"?>
    |<Response>
    |<Exchange>%s</Exchange>
    |</Response>
    """.stripMargin.trim

  val acceptPattern = """<Accept OrderRefId="%s"/>"""
  val rejectPattern = """<Reject Reason="%s"/>"""
  val rejectReasons = "ZSXI"

  val (port, brokerPort) = if (args.length >= 2) (args(0).toInt, args(1).toInt) else (8888,9999)

  val ss = new ServerSocket(port)
  println( "Listening on port %s (sending replies to localhost:%s)" format (port,brokerPort))

  val refIdCounter = new AtomicInteger(1)
  val rnd = new java.util.Random

  val http = new Http with thread.Safety

  def reject = {
    val idx = rnd.nextInt(4)
    val reason = rejectReasons(idx)
    rejectPattern format reason
  }

  // Notify message is sent with a certain delay (up to 1sec)
  def sendNotify (refId: String) = actor {
    reactWithin ( rnd nextInt 1000 ) {
      case TIMEOUT =>
        val notifyMsg = url("http://localhost:%s/broker" format brokerPort) << Map(
          "MessageType" -> "E",
          "OrderReferenceIdentifier" -> refId,
          "ExecutedShares" -> "100",
          "ExecutionPrice" -> "9000",
          "MatchNumber" -> "1",
          "To" -> "5145551212"
        )

        try {
          http(notifyMsg >- { response => println("\n Notify response=\n" + response) })

        } catch {
          case e => println("\nError sending notify response: " + e)
        }
    }
  }

  actor {
    loop {
      val socket = ss.accept
      println( "\nIncoming socket connection!" )

      val in = new BufferedReader( new InputStreamReader( socket.getInputStream ) )
      val headers = readHeaders(in)

      val length = headers collectFirst { case (name,value) if (name == "Content-Length") => value.toInt }

      val body = length map ( len => readBody(in, len) )
      println(">> " + body.mkString)

      val response = body map { request =>

        val id = refIdCounter.getAndIncrement
        val orderRefId = "S" + id
        val success = true//rnd.nextBoolean
        val decision = if ( success ) { acceptPattern format orderRefId } else reject
        val responseBody = responsePattern format decision
        val headers = headerPattern format (responseBody.length + 8)
        val fullResponse = headers + "\n\n" + responseBody

        println( "\nSending response: \n" + fullResponse )

        if ( success ) future { sendNotify (orderRefId) }

        fullResponse
      }

      val out = new OutputStreamWriter( socket.getOutputStream )
      out.write( response getOrElse "" )
      out.flush()

      socket.close()
    }
  }

  def readHeaders (in: BufferedReader) = {

    val httpLine = in.readLine
    println( "=> " + httpLine )

    val headers: mutable.Map[String, String] = mutable.Map()

    // TODO surely there's a better FP way of doing this...

    var line: String = null
    do {
      line = in.readLine
      if ( line == null ) line = ""

      if ( line != "" ) {
        println( "-> " + line )
        val name = line.takeWhile( _ != ':' ) // keep before :
        val value = line.dropWhile( _ != ':' ).drop(2) // drop left of :, drop :, drop space
        headers += name -> value
      }
    } while ( line != "" )

    headers.toMap // make immutable
  }

  def readBody (in: Reader, len: Int) = {

    val buf = new Array[Char](len)

    in.read(buf, 0, len)

    buf.mkString
  }
}
