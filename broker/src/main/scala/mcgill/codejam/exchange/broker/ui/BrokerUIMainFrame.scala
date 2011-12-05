/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.ui

import java.io.File
import swing._
import FileChooser.SelectionMode.FilesOnly
import javax.swing.BorderFactory
import javax.swing.JFrame.EXIT_ON_CLOSE
import BorderFactory.{createCompoundBorder, createTitledBorder, createEmptyBorder}
import Orientation.Horizontal
import event.ButtonClicked
import BrokerUI.Book

class BrokerUIMainFrame extends MainFrame {

  val windowLocationSetting = new PersistentSettings {
    val settingName = "window.location"
    val defaultSettingValue = "0,0"

    def get = {
      val s = getSetting.split(",")
      new Point(s(0).toInt, s(1).toInt)
    }
    def set (p: Point) { saveSetting( "%s,%s" format (p.x,p.y) ) }
  }

  val windowSizeSetting = new PersistentSettings {
    val settingName = "window.size"
    val defaultSettingValue = "800,800"

    def get = {
      val s = getSetting.split(",")
      new Dimension(s(0).toInt, s(1).toInt)
    }
    def set (d: Dimension) { saveSetting( "%s,%s" format (d.width,d.height) ) }
  }

  title = "Broker"
  location = windowLocationSetting.get
  size = windowSizeSetting.get
  peer setDefaultCloseOperation EXIT_ON_CLOSE

  def createBorder(title: String) =
    createCompoundBorder(
      createTitledBorder(title),
      createEmptyBorder(5, 5, 5, 5)
    )

  override def closeOperation() {
    println("Closing")

    windowLocationSetting set location
    windowSizeSetting set size

    BrokerUI.close()
  }

  def reset() {
    BrokerUI.reset()

    val text = exchangeAddr.text
    val exchangeHost = if (text startsWith "http") text else "http://" + text
    val jettyPort = brokerPort.text.toInt
    val threads = numThreads.text.toInt

    BrokerUI.instantiateServer(exchangeHost, jettyPort, threads)
  }

  val bookBtn = new Button {
    text = "Book!"
    enabled = false
    reactions += {
      case ButtonClicked(_) => {
        reset()
        BrokerUI ! Book
      }
    }
  }

  /********************************
   * Start of configuration panel *
   ********************************/
  val exchangeAddr = new PersistentTextField("exchange.url", "http://localhost:30000")
  val brokerContextPath = new PersistentTextField("broker.contextpath", "/broker")
  val brokerPort = new PersistentTextField("broker.port", "40000")
  val numThreads = new PersistentTextField("threads", "10")
  val orderFileTxt = new PersistentTextField("order.file", "") { editable = false }

  val orderLoadBtn = new Button {
    text = "..."

    lazy val fileChooser = new PersistentFileChooser( "order.dir", System.getProperty("user.dir") ) {
      title = "Choose orders file"
      fileSelectionMode = FilesOnly
    }

    if (orderFileTxt.text != "") parseFile(new File(orderFileTxt.text))

    def parseFile(file: File) = if ( file.exists ) {
      reset()
      bookBtn.enabled = true
      BrokerUI loadFile file
      file.getAbsolutePath
    }

    reactions += {
      case ButtonClicked(btn) =>
        fileChooser.showOpenDialogWithLastDirectory(btn)
        orderFileTxt.text = Option(fileChooser.selectedFile) match {
          case None => ""
          case Some(file) =>
            parseFile(file)
            file.getAbsolutePath
        }
    }
  }

  def label(caption: String) = {
    val lbl = new Label
    lbl.text = caption
    lbl
  }

  val configurationPanel = new MigPanel("insets 0, fill", "[]") {
    border = createBorder("Configuration")
    contents ++ Seq(
      (label("Exchange"), "split, span"),
      (new Separator, "growx, wrap"),
      (label("Full URL"), "align right, gap 10"),
      (exchangeAddr, "growx, wrap"),
      (label("Broker"), "split, span"),
      (new Separator, "growx, wrap"),
      (label("Context path"), "align right, gap 10"),
      (brokerContextPath, "split 5, growx, push, span"),
      (label("Port"), ""),
      (brokerPort, "width 60!"),
      (label("Threads"), ""),
      (numThreads, "width 35!, wrap"),
      (label("Orders file"), "align right"),
      (orderFileTxt, "split, span, push, growx, width 600:600:"),
      (orderLoadBtn, "")
    )
  }
  /******************************
   * End of configuration panel *
   ******************************/

  // Orders panel (orders table and executions table)
  val ordersScrollPane = new ScrollPane(BrokerUI.ordersTable)
  val executionsScrollPane = new ScrollPane(BrokerUI.executionsTable)
  val clearFilterButton = new Button() {
    text = "Clear filtering"
    listenTo(this)
    reactions += {
      case ButtonClicked(btn) =>
        BrokerUI.ordersTable.peer.clearSelection()
    }
  }
  val ordersPanel = new MigPanel("fill") {
    contents ++ Seq(
      (label("Orders"), "align left, wrap"),
      (ordersScrollPane, "grow, push, height 50:200:600, wrap")
    )
  }
  val executionsPanel = new MigPanel("fill") {
    contents ++ Seq(
      (label("Order executions"), "align left, split 3"),
      (label(""), "grow"), // spacer
      (clearFilterButton, "wrap"),
      (executionsScrollPane, "grow, push, height 50:100:600")
    )
  }

  val ordersSplit = new SplitPane(Horizontal, ordersPanel, executionsPanel)

  // Unmatched executions table
  val badExecutionsScrollPane = new ScrollPane(BrokerUI.badExecutionsTable)
  val badExecutionsPanel = new MigPanel("fill") {
    contents ++ Seq(
      (label("Unmatched executions"), "wrap, shrink"),
      (badExecutionsScrollPane, "grow, push, growprio 1000, height 50:200:600")
    )
  }

  val mainSplit = new SplitPane(Horizontal, ordersSplit, badExecutionsPanel)

  // Main panel
  contents = new MigPanel("fill") {
    contents ++ Seq(
      (configurationPanel, "growx, wrap"),
      (mainSplit, "grow, wrap"),
      (bookBtn, "align right")
    )
  }
}