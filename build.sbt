name := "orchestra"

version := "1.0"

scalaVersion := "2.11.8"
libraryDependencies +=  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"
//libraryDependencies += "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.2"
//libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.2"
libraryDependencies += "io.spray" % "spray-client_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-httpx_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.2"

libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.2"

mainClass in (Compile, run) := Some("Main")
