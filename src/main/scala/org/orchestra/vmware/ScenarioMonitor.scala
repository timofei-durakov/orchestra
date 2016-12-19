package org.orchestra.vmware

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.event.LoggingAdapter
import org.orchestra.common.Reaper.WatchClient
import org.orchestra.common.{AnsibleActor, Reaper}
import org.orchestra.vmware.config._
import org.orchestra.vmware.steps._

object ScenarioMonitor {
  def props(scenario: List[BaseStep], env: Env): Props =
    Props(new ScenarioMonitor(scenario, env))
}

class ScenarioMonitor(scenario: List[BaseStep], env: Env) extends Actor {
  var currentStep: Option[Int] = None: Option[Int]
  var ansible: ActorRef = null
  var reaper: ActorRef = null

  def init_monitor(): Unit = {
    implicit val system = context.system
    system.log.info("Starting scenario monitor for VMware")
    reaper = context.actorOf(Reaper.props, name = "reaper")
    ansible = context.actorOf(AnsibleActor.props(env.playbook_path), name = "ansible")
    reaper ! WatchClient(ansible)
    self ! "processNextStep"
  }

  def process_next_step() = {
    if (currentStep.isEmpty)
      currentStep = Some(0)
    else
      currentStep = Some(currentStep.get + 1)

    if (currentStep.get == scenario.length) {
      context stop ansible
      context stop self
    } else {
      self ! scenario(currentStep.get)
    }
  }

  override def receive: Receive = {
    case "start" => init_monitor()
    case "processNextStep" => process_next_step()
    case x: StartMigrationMonitorStep => StartMigrationMonitor.run(self, ansible, env)
    case x: VmotionMigrateStep => VmotionMigrate.run(self, ansible, env)
    case x: DestroyVmsStep => DestroyVms.run(self, ansible, env)
    case x: StopMigrationMonitorStep => StartMigrationMonitor.run(self, ansible, env)
  }
}
