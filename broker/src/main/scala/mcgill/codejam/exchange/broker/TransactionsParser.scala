/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker

import util.parsing.combinator._
import mcgill.codejam.exchange.broker.Transactions._

/**
 * Transactions file parser combinator.
 *
 * If you haven't taken a Compilers class at school,
 * and if you haven't read Odersky's book (Chapter 33),
 * then this code will look like gibberish.
 *
 * Shut up and read the book first.
 *
 * File Format:
 *
 * Buy 12 MS @ 12.34 #72619 TW
 * (where 72619 is an SMS short code, or a 10-digit phone number)
 * TW denotes that a notification should be sent (via Twillio) - this is optional and can be ommitted.
 *
 * Sample:

Buy 12 MS @ 12.34 #72619
Sell 12 MS @ 12.34 #72619

Parallel {
  Buy 12 MS @ 12.34 #72619
  Sell 12 MS @ 12.34 #72619
}

Parallel(
  Sell 456 Pc @ 876.32 #5145551212
  Buy 456 Pd @ 876.32 #5145551212
)
 */
object TransactionsParser extends RegexParsers {

  private def buy: Parser[Char] = """Buy|buy|BUY""".r ^^ (x => 'B')
  private def sell: Parser[Char] = """Sell|sell|SELL""".r ^^ (x => 'S')
  private def shares: Parser[Int] = """\d+""".r ^^ (_.toInt)
  private def stock: Parser[String] = """[a-zA-Z\._]+""".r
  private def price: Parser[Double] = """(\d+(\.\d*)?|\d*\.\d+)""".r ^^ (_.toDouble)
  private def sms: Parser[String] = "#" ~> """(\+(?:[0-9] ?){6,14}[0-9])""".r
  private def twillio: Parser[Boolean] = opt("TW") ^^ (_.isDefined)

  private def order: Parser[Order] = ( (buy | sell) ~ shares ~ stock ~ ("@" ~> price) ~ sms ~ twillio ) ^^ {
    case side ~ shares ~ stock ~ price ~ sms ~ twillio =>
      if (side == 'B') new Buy(shares,stock,price,sms,twillio) else new Sell(shares,stock,price,sms,twillio)
  }

  private def parallelPrefix: Parser[String] = """(Parallel|parallel|PARALLEL)\s*(\(|\{)""".r
  private def parallel: Parser[ParallelBook] = parallelPrefix ~> sequential <~ ("}" | ")") ^^ {
    case composite: SeqBook => new ParallelBook(composite orders)
  }

  private def sequential: Parser[SeqBook] = rep( order | parallel ) ^^ {
     case bs: List[_] => new SeqBook(bs)
  }

  def parse (in: java.io.Reader) = parseAll(sequential, in)
}
