package org.orchestra.openstack

import akka.actor.{Actor, ActorRef, Props}
import akka.io.IO

import spray.can.Http

import org.orchestra.common._
import org.orchestra.common.model.InstanceAvailableEvent
import org.orchestra.common.Reaper._

import org.orchestra.openstack.config._
import org.orchestra.openstack.model.FloatingIPAddress

import scala.collection._

/**
  * Created by tdurakov on 29.03.16.
  */

object ScenarioMonitor {
  def props(cloud: Cloud, runNumber: Int, backend: Backend, scenario: Scenario): Props =
    Props(new ScenarioMonitor(cloud, runNumber, backend, scenario))
}

class ScenarioMonitor(cloud: Cloud, var runNumber: Int, backend: Backend, scenario: Scenario) extends Actor {

  var influx: ActorRef = null
  var reaper: ActorRef = null
  var countdownLatch: ActorRef = null
  var callback_service: ActorRef = null
  var callback_listener: ActorRef = null
  var ansible: ActorRef = null
  var current_sync_event = 0
  var current_finish_event = 0
  var iteration_finished = false
  var started = false
  val conductors = mutable.Map.empty[String, ActorRef]
  val vmFloatingIps = mutable.Set.empty[String]
  var idGenerator: Int = 0

  def init_monitor = {
    implicit val system = context.system
    system.log.info("Starting  scenario monitor")
    reaper = context.actorOf(Reaper.props, name = "reaper")
    callback_service = context.actorOf(InstanceCallbackService.props(context.self))
    reaper ! WatchClient(callback_service)

    IO(Http) ! Http.Bind(callback_service, interface = backend.callback_host, port = backend.callback_port)

    influx = context.actorOf(InfluxDB.props(backend.influx_host, backend.database), "influx")
    reaper ! WatchClient(influx)
    countdownLatch = context.actorOf(CountdownLatch.props(scenario.parallel), "cdl")
    ansible = context.actorOf(AnsibleActor.props(scenario.playbook_path), name = "ansible")
    reaper ! WatchClient(ansible)
    configure_env
  }

  def configure_env = {
    if (scenario.pre_config.isDefined) {
      ansible ! AnsibleCommand(
        scenario.hosts,
        vmFloatingIps,
        "nova_flags.yml",
        immutable.Map("nova_compress" -> scenario.pre_config.get.nova_compress.toString,
          "nova_autoconverge" -> scenario.pre_config.get.nova_autoconverge.toString,
          "nova_concurrent_migrations" -> scenario.pre_config.get.nova_concurrent_migrations.toString,
          "nova_max_downtime" -> scenario.pre_config.get.nova_max_downtime.toString))
    } else {
      self ! "processNextEvent"
    }
  }

  def init_conductors = {
    started = true
    for (i <- 1 to scenario.parallel) {
      val conductorName = scenario.vm_template.name_template.format(idGenerator)
      val conductor = context.actorOf(InstanceConductorActor.props(idGenerator,
        cloud, scenario.vm_template, backend, scenario.steps, runNumber, scenario.id, influx, countdownLatch),
        name = "conductor" + idGenerator)
      reaper ! WatchСonductor(conductor)
      conductor ! "start"
      conductors(conductorName) = conductor
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
      terminate_listener
    } else {
      runNumber += 1
      reset_event_counters
      conductors.clear()
      init_conductors
    }

  }

  def terminate_listener = {
    implicit val system = context.system
    callback_listener ! Http.Unbind
  }

  def terminate_clients = {
    context stop callback_listener
    context stop influx
    context stop ansible
    context stop countdownLatch
    context stop callback_service
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
      if (!scenario.on_finish.isEmpty) {
        self ! scenario.on_finish(current_finish_event)
        current_finish_event += 1
      }
    }
  }

  def start_telegraf = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "start.yml",
      immutable.Map("lm_run" -> runNumber.toString,
        "lm_scenario" -> scenario.id.toString))
  }

  def shutdown_telegraf = {
    ansible ! AnsibleCommand(
      scenario.hosts,
      vmFloatingIps,
      "stop.yml",
      immutable.Map.empty[String, String])
  }

  def load_test = {
    if (scenario.load_config.isDefined) {
      ansible ! AnsibleCommand(
        scenario.hosts,
        vmFloatingIps,
        "load.yml",
        immutable.Map("vm_workers" -> scenario.load_config.get.vm_workers.toString,
          "malloc_mem_mb" -> scenario.load_config.get.malloc_mem_mb.toString))
    } else {
      self ! "processNextEvent"
    }
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
    conductors.values.foreach((a: ActorRef) => a ! "processNextStep")
  }

  def notifyConductorOnEvent(event: InstanceAvailableEvent): Unit = {
    conductors(event.name) ! event
  }

  def receive = {
    case "start" => init_monitor
    case "start_telegraph" => start_telegraf
    case "shutdown_telegraph" => shutdown_telegraf
    case "countdown_latch_triggered" => on_sync_events
    case "finish_event_triggered" => on_finish
    case "resume_conductors" => continueConductorExecution
    case "processNextEvent" => on_event_result
    case "load_test" => load_test
    case Http.Bound(address) => {
      context.system.log.debug("http bound event received: {}", address)
      callback_listener = sender()
      reaper ! WatchClient(callback_listener)
    }
    case Http.Unbound => {
      terminate_clients
    }

    case x: InstanceAvailableEvent => notifyConductorOnEvent(x)
    case ip: FloatingIPAddress => cache_instance_ip(ip)
    case a: Any => context.system.log.warning("unexpected message received => {}", a)
  }
}
