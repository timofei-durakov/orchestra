
import java.io.File


import akka.actor.{ActorSystem}
import org.orchestra.actor._
import scala.io.Source
import net.jcazevedo.moultingyaml._
import org.orchestra.config._

object Main extends ConfigYamlProtocol {

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

    for ((name, scenario) <- config.scenarios) {
      val system = ActorSystem("OrchestraSystem" + name)
      system.log.info("starting scenario: {}", name)
      val scenarioMonitor = system.actorOf(ScenarioMonitor.props(config.cloud, config.vm_template, config.run_number,
        config.backend, scenario), "monitor")
      scenarioMonitor ! "start"
      system.awaitTermination()
    }

    println("Orchestra system is terminated")
  }
}