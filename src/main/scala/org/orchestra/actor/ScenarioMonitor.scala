package org.orchestra.actor

import akka.actor.{ActorRef, Actor, Props}
import org.orchestra.actor.Reaper.WatchMe
import org.orchestra.config.{Backend, Scenario, VmTemplate, Cloud}

/**
  * Created by tdurakov on 29.03.16.
  */

object ScenarioMonitor {
  def props(cloud: Cloud, vmTemplate: VmTemplate, runNumber: Int, backend: Backend, scenario: Scenario): Props =
    Props(new ScenarioMonitor(cloud, vmTemplate, runNumber, backend, scenario))
}

class ScenarioMonitor(cloud: Cloud, vmTemplate: VmTemplate, runNumber: Int, backend: Backend, scenario: Scenario) extends Actor {

  import context.system
  var influx: ActorRef = null
  var reaper: ActorRef = null
  var countdownLatch: ActorRef = null


  def start_conductors = {
    influx = system.actorOf(InfluxDB.props(backend.influx_host, backend.database), "influx")
    reaper = system.actorOf(Reaper.props, name = "reaper")
    countdownLatch = system.actorOf(CountdownLatch.props(scenario.parallel), "cdl")

    var idGenerator: Int = 0
    for (i <- 1 to scenario.parallel) {
      val conductor = system.actorOf(InstanceConductorActor.props(idGenerator,
        cloud, vmTemplate, scenario.steps, runNumber, scenario.id, influx, countdownLatch),
        name = "conductor" + idGenerator)
      reaper ! WatchMe(conductor)
      conductor ! "start"
      idGenerator += 1
    }
  }


  def receive = {
    case "start" => start_conductors
  }
}
