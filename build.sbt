name := "orchestra"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.12" % "3.5.0"
libraryDependencies += "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.14"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"
libraryDependencies += "io.spray" % "spray-client_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-httpx_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.2"

libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.2"
libraryDependencies += "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.4"

libraryDependencies += "org.scalactic" %% "scalactic" % "2.2.6"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

libraryDependencies += "com.toastcoders" % "yavijava" % "6.0.04"

scalacOptions += "-feature"

mainClass in (Compile, run) := Some("Main")
