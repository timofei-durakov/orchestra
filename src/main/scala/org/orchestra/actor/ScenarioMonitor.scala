package org.orchestra.actor

import akka.actor.{ActorRef, Actor, Props}
import org.orchestra.actor.Reaper.{WatchClient, WatchСonductor}
import org.orchestra.actor.model.{FloatingIPAddress, FloatingIP}

import org.orchestra.config._

import scala.collection._

/**
  * Created by tdurakov on 29.03.16.
  */

object ScenarioMonitor {
  def props(cloud: Cloud, runNumber: Int, backend: Backend, scenario: Scenario, periodic: Option[List[Periodic]]): Props =
    Props(new ScenarioMonitor(cloud, runNumber, backend, scenario, periodic))
}

class ScenarioMonitor(cloud: Cloud, var runNumber: Int, backend: Backend, scenario: Scenario,
                      periodic: Option[List[Periodic]]) extends Actor {
  var influx: ActorRef = null
  var reaper: ActorRef = null
  var countdownLatch: ActorRef = null
  var ansible: ActorRef = null
  var current_sync_event = 0
  var current_finish_event = 0
  var iteration_finished = false
  var started = false
  val conductors = mutable.ArrayBuffer.empty[ActorRef]
  val vmFloatingIps = mutable.Set.empty[String]
  var idGenerator: Int = 0

  def start_conductors = {
    reaper = context.actorOf(Reaper.props, name = "reaper")
    influx = context.actorOf(InfluxDB.props(backend.influx_host, backend.database), "influx")
    reaper ! WatchClient(influx)
    countdownLatch = context.actorOf(CountdownLatch.props(scenario.parallel), "cdl")
    reaper ! WatchClient(countdownLatch)
    ansible = context.actorOf(AnsibleActor.props(scenario.playbook_path), name = "ansible")
    reaper ! WatchClient(ansible)
    if (periodic.isDefined) {
      val periodicList = periodic.get
      val periodics = periodicList.map {
        case x: CheckEndpoints => {
          context.actorOf(EndpointChecker.props(influx, cloud, x.period.getOrElse(5.0)), "endpoint_checker")
        }
      }
      for (p <- periodics) {
        p ! "init"
        reaper ! WatchClient(p)
      }
    }
    configure_env
  }

  def configure_env = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "nova_flags.yml",
      immutable.Map("nova_compress" -> scenario.pre_config.nova_compress.toString,
                    "nova_autoconverge" -> scenario.pre_config.nova_autoconverge.toString,
                    "nova_concurrent_migrations" -> scenario.pre_config.nova_concurrent_migrations.toString,
                    "nova_max_downtime" -> scenario.pre_config.nova_max_downtime.toString))
  }

  def init_conductors = {
    started = true
    for (i <- 1 to scenario.parallel) {
      val conductor = context.actorOf(InstanceConductorActor.props(idGenerator,
        cloud, scenario.vm_template, scenario.steps, runNumber, scenario.id, influx, countdownLatch),
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
    vmFloatingIps.clear
    iteration_finished = false
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
     context stop ansible
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
    iteration_finished = true
    if (current_finish_event == scenario.on_finish.length) {
      new_iteration
    } else {
      self ! scenario.on_finish(current_finish_event)
      current_finish_event += 1
    }
  }

  def start_telegraph = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "start.yml",
      immutable.Map("lm_run" -> runNumber.toString,
                    "lm_scenario" -> scenario.id.toString))
  }

  def shutdown_telegraph = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "stop.yml",
      immutable.Map.empty[String, String])
  }

  def load_test = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "load.yml",
      immutable.Map("vm_workers" -> scenario.load_config.vm_workers.toString,
                    "malloc_mem_mb" -> scenario.load_config.malloc_mem_mb.toString))
  }

  def on_event_result = {
    if (!started) {
      init_conductors
    } else if (iteration_finished) {
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
    case "load_test" => load_test
    case ip: FloatingIPAddress => cache_instance_ip(ip)
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }
}
