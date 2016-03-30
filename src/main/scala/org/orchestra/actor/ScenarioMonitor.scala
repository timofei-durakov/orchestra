package org.orchestra.actor

import akka.actor.{ActorRef, Actor, Props}
import org.orchestra.actor.Reaper.{WatchClient, WatchСonductor}
import org.orchestra.actor.model.{FloatingIPAddress, FloatingIP}

import org.orchestra.config.{Backend, Scenario, VmTemplate, Cloud}
import scala.collection.mutable.Set

import scala.collection.mutable.ArrayBuffer

/**
  * Created by tdurakov on 29.03.16.
  */

object ScenarioMonitor {
  def props(cloud: Cloud, vmTemplate: VmTemplate, runNumber: Int, backend: Backend, scenario: Scenario): Props =
    Props(new ScenarioMonitor(cloud, vmTemplate, runNumber, backend, scenario))
}

class ScenarioMonitor(cloud: Cloud, vmTemplate: VmTemplate, var runNumber: Int, backend: Backend, scenario: Scenario) extends Actor {

  var influx: ActorRef = null
  var reaper: ActorRef = null
  var countdownLatch: ActorRef = null
  var telegraph: ActorRef = null
  var current_sync_event = 0
  var current_finish_event = 0
  var finished = false
  val conductors = ArrayBuffer.empty[ActorRef]
  val vmFloatingIps = Set.empty[String]
  var idGenerator: Int = 0

  def start_conductors = {
    reaper = context.actorOf(Reaper.props, name = "reaper")
    influx = context.actorOf(InfluxDB.props(backend.influx_host, backend.database), "influx")
    reaper ! WatchClient(influx)
    countdownLatch = context.actorOf(CountdownLatch.props(scenario.parallel), "cdl")
    telegraph = context.actorOf(TelegraphActor.props(scenario.playbook_path, runNumber, scenario.id), name = "telegraph")
    reaper ! WatchClient(telegraph)
    init_conductors
  }

  def init_conductors = {
    for (i <- 1 to scenario.parallel) {
      val conductor = context.actorOf(InstanceConductorActor.props(idGenerator,
        cloud, vmTemplate, scenario.steps, runNumber, scenario.id, influx, countdownLatch),
        name = "conductor" + idGenerator)
      reaper ! WatchСonductor(conductor)
      conductor ! "start"
      conductors += conductor
      idGenerator += 1
    }
  }

  def reset_event_counters = {
    current_sync_event = 0
    current_finish_event = 0
  }

  def new_iteration = {
    if (runNumber == scenario.repeat) {
      terminate_clients
    } else {
      runNumber += 1
      reset_event_counters
      conductors.clear()
      init_conductors
    }

  }

  def terminate_clients = {
     context stop influx
     context stop telegraph
     context stop countdownLatch
  }

  def on_sync_events = {
    if (current_sync_event == scenario.on_sync_events.length) {
      continueConductorExecution
    } else {
      self ! scenario.on_sync_events(current_sync_event)
      current_sync_event += 1
    }
  }

  def on_finish = {
    finished = true
    if (current_finish_event == scenario.on_sync_events.length) {
      new_iteration
    } else {
      self ! scenario.on_finish(current_finish_event)
      current_finish_event += 1
    }
  }

  def start_telegraph = {
    telegraph ! TelegraphStart(scenario.hosts,vmFloatingIps)

  }

  def shutdown_telegraph = {
    telegraph ! "stop"
  }

  def on_event_result = {
    if (finished) {
      on_finish
    } else {
      continueConductorExecution
    }
  }

  def cache_instance_ip(ip: FloatingIPAddress) = {
    vmFloatingIps.add(ip.address)
  }

  private def continueConductorExecution = {
    conductors.foreach((a: ActorRef) => a ! "processNextStep")
  }

  def receive = {
    case "start" => start_conductors
    case "start_telegraph" => start_telegraph
    case "shutdown_telegraph" => shutdown_telegraph
    case "countdown_latch_triggered" => on_sync_events
    case "finish_event_triggered" => on_finish
    case "resume_conductors" => continueConductorExecution
    case "processNextEvent" => on_event_result
    case ip: FloatingIPAddress => cache_instance_ip(ip)
  }
}
