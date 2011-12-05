/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.cli

import actors.Actor

class UserInput (main: Actor) extends Actor {

  case object Enter
  case object Quit
  case object Buys
  case object Sells
  case object Trades
  case object Executions
  case object BadExecutions
  
  val exitRegex = """(:?quit|exit|:?q)""".r
  val buysRegex = """(:?buys)""".r
  val sellsRegex = """(:?sells)""".r
  val tradesRegex = """(:?trades)""".r
  val executionsRegex = """(:?executions)""".r
  val badExecutionsRegex = """(:?bexecutions)""".r

  def act() {
    loop {
      readLine() match {

        case "" =>  main ! Enter
        case exitRegex(_) => {
          main ! Quit
          exit()
        }
        case buysRegex(_) => { main ! Buys }
        case sellsRegex(_) => { main ! Sells }
        case tradesRegex(_) => { main ! Trades }
        case executionsRegex(_) => { main ! Executions }
        case badExecutionsRegex(_) => { main ! BadExecutions}

        case other => // ignore
      }
    }
  }

  start() // auto-start actor thread
}