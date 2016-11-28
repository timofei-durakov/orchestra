package org.orchestra.vmware.steps

import akka.actor.{ActorRef, Actor}
import org.orchestra.vmware.config.Env


trait BaseStep

abstract class Step extends BaseStep {
  def run(monitor: ActorRef, ansible: ActorRef, env: Env): Unit
}
