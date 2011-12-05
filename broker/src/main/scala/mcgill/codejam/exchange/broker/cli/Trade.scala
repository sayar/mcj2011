/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.cli

import mcgill.codejam.exchange.broker.Exchange.Execution
import mcgill.codejam.exchange.broker.Transactions.{Sell, Buy, Order}

class Trade (val matchNb: Int) {
  var buy: Option[Execution] = None
  var sell: Option[Execution] = None

  def setSide (order: Order, exec: Execution) {
    order match {
      case _: Buy => buy = Some(exec)
      case _: Sell => sell = Some(exec)
    }
  }
  
  override def toString = {
    // get either the buy or the sell, whichever is defined first
    val bs = Seq( buy, sell ).flatten.head
    
    val sPhone = sell map ( _.to ) getOrElse "[NO SELL SIDE]"
    val bPhone = buy map ( _.to ) getOrElse "[NO BUY SIDE]"
    var result = "(MatchNb: %s Shares %s Price: %s Buy Phone: %s Sell Phone: %s" format (matchNb,bs.shares,bs.price,bPhone,sPhone)
    if ( !buy.isDefined ) result += " Warning: No buy side"
    if ( !sell.isDefined ) result += " Warning: No sell side"
    
    buy foreach { b =>
      sell foreach { s =>
        if ( b.price != s.price ) result += " Error: execution prices differ (%s != %s)" format (b.price,s.price)
        if ( b.shares != s.shares ) result += " Error: execution shares differ (%s != %s)" format (b.shares,s.shares)
      }
    }
    
    result + ")"
  }
}
