/*
Copyright 2011 John Lobaugh, Bruno Navert, Frederick Dubois
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License
*/
import sbt._
import Keys._
import Defaults.defaultSettings

import ProguardPlugin._

import java.io.{File, FileWriter}

object EStockExchange extends Build {

  val mainClass = "mcgill.codejam.exchange.broker.ui.BrokerUI"

  val pathSep = System.getProperty("path.separator")
  val lineSep = System.getProperty("line.separator")

  val runscriptTemplate = Seq(
    "%s",
    "%s",
    "%s",
    "",
    "java %s"
  ).mkString(lineSep)

  val cpLinux = "${CLASSPATH}"
  val cpWindows = "%CLASSPATH%"
  val windowsHeader = Seq(
    "@echo off",
    "",
    "set CLASSPATH=''"
  ).mkString(lineSep)

  val linuxHeader = Seq(
    "#!/bin/bash",
    "",
    "CLASSPATH=''"
  ).mkString(lineSep)

  val createRunscript: Command = Command.command("create-runscript") {
    state =>
    val extracted = Project.extract(state)
    import extracted.evalTask
    val ur = evalTask(update, state)
    val currentDir = file(System.getProperty("user.dir"))
    val jar = evalTask(packageBin in Compile, state)
    IO.copyFile(jar, currentDir / "lib" / jar.getName)
    val isWindows = (pathSep == ";")
    val (cpVar, header, footer, set, runFile) =
      if (isWindows)
        (cpWindows, windowsHeader, "", "set ", file("run.bat"))
      else
        (cpLinux, linuxHeader, "export CLASSPATH", "", file("run.sh"))
    val classpath = ur
      .select(
        configuration=Set("runtime"),
        artifact=artifactFilter(`type`="jar"),
        module=moduleFilter(name = (n: String) => n != "scala-library")
      ) ++ (currentDir / "lib" ** "*.jar").get
    val relativeCP = classpath x relativeTo(currentDir)
    val exportedCP = relativeCP map { case (_, absolutePath) =>
      "%sCLASSPATH=%s%s%s" format (set, cpVar, pathSep, absolutePath)
    }
    val fw = new FileWriter(runFile)
    val runscript = runscriptTemplate format (
      header, exportedCP.mkString(lineSep), footer, mainClass
    )
    fw.write(runscript.split("\n").toSeq.mkString(lineSep))
    fw.close()
    runFile.setExecutable(true, false)
    state
  }

  val broker = Project("broker", file("broker"),
    settings = defaultSettings ++ proguardSettings ++
      Seq(
        commands += createRunscript,
        proguardOptions ++= Seq(
          "-keep class mcgill.* { *; }",
          keepMain("mcgill.codejam.exchange.broker.ui.BrokerUI"),
          keepAllScala
        ),
        retrieveManaged := true,
        scalacOptions ++= Seq("-deprecation", "-unchecked"),
        fork in (Compile, run) := true
//        javaOptions in (Compile, run) ++= Seq(
//          "-Xdebug",
//          "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=50000"
//        )
      )
  )

  val mock = Project("mock", file("mock"))

  val refImpl = Project("refImpl", file("refImpl/mcj2011"),
    settings = Defaults.defaultSettings ++ Seq(
      javaSource in Compile <<= baseDirectory (_ / "main" / "packages"),
      javaSource in Test <<= baseDirectory (_ / "main" / "tests"),
      parallelExecution in Test := false,
      libraryDependencies +=
        "com.novocode" % "junit-interface" % "0.7" % "test->default"
    )
  )
}
