package org.orchestra.vmware.steps

import akka.actor.ActorRef
import com.typesafe.scalalogging.Logger
import org.orchestra.common.AnsibleCommand
import org.orchestra.vmware.config.Env
import org.slf4j.LoggerFactory


final case class StopMigrationMonitorStep() extends BaseStep

object StopMigrationMonitor extends Step{
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  override def run(parent: ActorRef, ansible: ActorRef, env: Env): Unit = {
    log.info("Stop migration-monitor")
    ansible ! AnsibleCommand(
      List(env.monitoring_node.hostname),
      Set.empty,
      "stop.yml",
      Map.empty)

    parent ! "processNextStep"
  }
}
