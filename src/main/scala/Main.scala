
import java.io.File
import com.typesafe.config.ConfigFactory
import spray.can.Http

import scala.io.Source
import akka.actor.{ActorSystem}
import akka.io.IO
import net.jcazevedo.moultingyaml._

import org.orchestra.actor._
import org.orchestra.config._
import org.orchestra.config.ConfigYamlProtocol._

object Main {

  def main(args: Array[String]) {
    //TODO: change to some existing lib
    if (args.isEmpty) {
      println("application.conf is required")
      return
    }
    val confFile = args(0)

    //TODO: check if it's possible to use akka extensions instead
    val data = Source.fromFile(new File(confFile)).mkString
    println(data)
    val config = data.parseYaml.convertTo[Config]
    val actorConfig = ConfigFactory.load
    val logConfig = ConfigFactory.parseString("akka.loglevel = \"%s\"".format(config.app_config.log_level))
    val systemConfig = logConfig.withFallback(actorConfig)
    for ((name, scenario) <- config.scenarios) {
      val system = ActorSystem("OrchestraSystem" + name, systemConfig)
      system.log.info("starting scenario: {}", name)
      val scenarioMonitor = system.actorOf(ScenarioMonitor.props(config.cloud, config.run_number,
        config.backend, scenario), "monitor")
      scenarioMonitor ! "start"
      system.awaitTermination()
    }

    println("Orchestra system is terminated")
  }
}
