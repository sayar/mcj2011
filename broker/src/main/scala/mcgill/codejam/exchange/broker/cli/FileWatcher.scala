/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/

package mcgill.codejam.exchange.broker.cli

import collection._
import java.io.File
import actors.Actor
import java.util._

class FileWatcher (main: Actor, files: Seq[File]) {

  case class ModifiedFile (file: File)

  val pollPeriod = 1000
  val timer = new Timer("FileWatcher",true)

  val timestamps: mutable.Map[File,Long] = mutable.Map( files.map { f => (f, f.lastModified) } : _* )

  val timerTask = new TimerTask {
    def run {
      timestamps foreach {
        case (f,ts) => if (f.lastModified > ts) {
          main ! ModifiedFile(f)
          timestamps(f) = f.lastModified
        }
      }
    }
  }

  timer.schedule( timerTask, new Date, pollPeriod )
}