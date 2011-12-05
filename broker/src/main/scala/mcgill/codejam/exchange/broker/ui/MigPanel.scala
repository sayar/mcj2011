/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

//import java.awt.Component

import javax.swing.JPanel

import net.miginfocom.swing.MigLayout

import swing._

/**
* Implement LayoutContainer for a panel with MigLayout
*
* Seems LayoutManagers are coupled to Components in
* scala swing. So you can't just use an existing
* component with a new layout manager (like MigLayout),
* you need to create a new class.
*
* The reasoning behind the coupling is to catch more
* type errors.
*/
class MigPanel(
  val layoutCons: String = "",
  val colCons: String = "",
  val rowCons: String = ""
) extends Panel with LayoutContainer {

  type Constraints = String

  override lazy val peer =
    new JPanel(new MigLayout(layoutCons, colCons, rowCons)) with SuperMixin

  private lazy val layoutMgr =
    peer.getLayout.asInstanceOf[MigLayout]

  override lazy val contents = new MigContent

  protected class MigContent extends Content {

    def +=(comp: Component, cons: Constraints) {
      add(comp, cons)
    }

    def ++(comps: Seq[(Component, Constraints)]) {
      comps foreach {
        case (comp, cons) => add(comp, cons)
      }
    }
  }

  protected def constraintsFor(c: Component) =
    layoutMgr.getComponentConstraints(c.peer).asInstanceOf[String]

  protected def areValid(c: Constraints) = (true, "")

  def add(comp: Component) {
    peer.add(comp.peer)
  }

  def add(comp: Component, cons: Constraints) {
    peer.add(comp.peer, cons)
  }
}
