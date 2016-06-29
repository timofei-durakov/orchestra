package org.orchestra.libvirt

/**
  * Created by vova on 6/26/16.
  */

import akka.actor.ActorSystem
import config.Config
import config.ConfigYamlProtocol._

import net.jcazevedo.moultingyaml._


object LibvirtMigrationTest {
  def start(data: String) {
    val config = data.parseYaml.convertTo[Config]

    for ((name, scenario) <- config.scenarios) {
      val system = ActorSystem("sys_" + name)
      system.log.info("starting scenario: {}", name)

      val scenarioMonitor = system.actorOf(ScenarioExecutor.props(scenario,config.test, config.env), "executor")
      scenarioMonitor ! "start"
      system.awaitTermination()
    }

  }
}
