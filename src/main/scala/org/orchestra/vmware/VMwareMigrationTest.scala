package org.orchestra.vmware

import akka.actor.{ActorSystem, Props, Actor, ActorRef}
import com.typesafe.config.ConfigFactory
import net.jcazevedo.moultingyaml._
import org.orchestra.vmware.config.Config
import org.orchestra.vmware.config.ConfigYamlProtocol._


object VMwareMigrationTest {
  def start(data:String) : Unit = {
    val config = data.parseYaml.convertTo[Config]

    for ((name, scenario) <- config.scenarios) {
      val actorConfig = ConfigFactory.load
      val logConfig = ConfigFactory.parseString(
        """
          |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
          |akka.loglevel = %s
        """.format(config.app_config.log_level).stripMargin)

      val systemConfig = logConfig.withFallback(actorConfig)
      val system = ActorSystem("sys_" + name, systemConfig)


      system.log.info("starting scenario: {}", name)

      val scenarioMonitor = system.actorOf(ScenarioMonitor.props(scenario.steps, config.env), "monitor")
      scenarioMonitor ! "start"
      system.awaitTermination()
    }
  }
}
