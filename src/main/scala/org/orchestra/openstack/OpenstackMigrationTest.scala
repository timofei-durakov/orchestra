package org.orchestra.openstack

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import config.Config
import config.ConfigYamlProtocol._

import net.jcazevedo.moultingyaml._


object OpenstackMigrationTest {
  def start(data: String) : Unit = {
    val config = data.parseYaml.convertTo[Config]

    for ((name, scenario) <- config.scenarios) {
      val actorConfig = ConfigFactory.load
      val logConfig = ConfigFactory.parseString("akka.loglevel = \"%s\"".format(config.app_config.log_level))
      val systemConfig = logConfig.withFallback(actorConfig)
      val system = ActorSystem("sys_" + name, systemConfig)

      system.log.info("starting scenario: {}", name)

      val scenarioMonitor = system.actorOf(ScenarioMonitor.props(config.cloud, config.run_number,
        config.backend, scenario), "monitor")
      scenarioMonitor ! "start"
      system.awaitTermination()
    }
  }
}
