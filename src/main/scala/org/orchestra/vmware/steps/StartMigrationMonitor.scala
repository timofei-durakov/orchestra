package org.orchestra.vmware.steps

import akka.actor.ActorRef
import com.typesafe.scalalogging.Logger
import org.orchestra.common.AnsibleCommand
import org.orchestra.vmware.config._
import org.slf4j.LoggerFactory

final case class StartMigrationMonitorStep() extends BaseStep

object StartMigrationMonitor extends Step{
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  override def run(monitor: ActorRef, ansible: ActorRef, env: Env): Unit = {
    log.info("Start migration monitor")
    ansible ! AnsibleCommand(
      List(env.monitoring_node.hostname),
      Set.empty,
      "start.yml",
      Map.empty)

    monitor ! "processNextStep"
  }
}
