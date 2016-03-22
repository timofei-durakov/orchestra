
import java.io.File


import akka.actor.{ActorSystem}
import org.orchestra.actor.InstanceConductorActor
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
    var idGenerator: Int = 0
    //TODO: check if it's possible to use akka extensions instead
    val data = Source.fromFile(new File(confFile)).mkString
    println(data)
    val config = data.parseYaml.convertTo[Config]
    val system = ActorSystem("OrchestraSystem")
    val currentScenario = config.scenarios.head._2
    for (i <- 1 to currentScenario.parallel) {
      val conductor = system.actorOf(InstanceConductorActor.props(idGenerator,
        config.cloud, config.vm_template, currentScenario.steps), name = "conductor" + idGenerator)
      conductor ! "start"
      idGenerator += 1
    }
    system.awaitTermination()
  }
}