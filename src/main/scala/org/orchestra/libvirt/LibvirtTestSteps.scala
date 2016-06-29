package org.orchestra.libvirt

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.japi.Procedure
import org.orchestra.common.Reaper.WatchClient
import org.orchestra.common._
import org.orchestra.libvirt.config._

/**
  * Created by vova on 6/26/16.
  */




object LibvirtTestSteps {
  def props(reaper: ActorRef, env: Env): Props =
    Props(new LibvirtTestSteps(reaper, env))

}

class LibvirtTestSteps(reaper: ActorRef, env: Env) extends Actor {

  type StepRun = (Int, Scenario) => Unit

  val influx = context.actorOf(InfluxDB.props(env.influx.host, env.influx.database), "influx")
  val ansible = context.actorOf(AnsibleActor.props(env.playbook_path), name = "ansible")
  val callback = context.actorOf(CallbackActor.props(env.callback, reaper), name = "callback")

  // TODO get rid of this state
  var domain_name:String = null;
  var config_drive_name:String = null;

  def timestamp() = {
    System.currentTimeMillis / 1000
  }

  def start = {
    reaper ! WatchClient(influx)
    reaper ! WatchClient(ansible)
    reaper ! WatchClient(callback)

    context.parent ! "done"
  }

  def terminate_clients = {
    influx ! PoisonPill
    ansible ! PoisonPill
    callback ! PoisonPill
  }

  def ansible_command(hosts: List[String], pb: String, extra: Map[String, String] = Map.empty): Unit = {
    ansible ! AnsibleCommand(hosts, Set.empty, pb, extra)
  }

  def install_prerequisites: StepRun = (_, _) => {
    ansible_command(List(env.libvirt.source), "install_prerequisites.yml")
  }

  def generate_config_drive: StepRun = (_, s) => {
    val l = s.load
    val ts = timestamp()

    config_drive_name = s"config_w${l.vm_workers}_m${l.malloc_mem_mb}_${ts}.iso"
    domain_name = s"dom_w${l.vm_workers}_m${l.malloc_mem_mb}_${ts}"

    val extra = Map(
      "vm_workers" -> l.vm_workers.toString,
      "malloc_mem_mb" -> l.malloc_mem_mb.toString,
      "call_home_url" -> s"http://${env.callback.host}:${env.callback.port}/knock/",
      "config_drive_name" -> config_drive_name)

    ansible_command(List(env.libvirt.source), "config_drive.yml", extra)
  }

  def boot_vm: StepRun = (_, s) => {
    val l = s.load


    val extra = Map(
      "domain_name" -> domain_name,
      "config_drive_name" -> config_drive_name)

    ansible_command(List(env.libvirt.source), "boot_vm.yml", extra)
  }

  def start_telegraf: StepRun = (r, s) => {
    val extra = Map(
      "influxdb_host" -> env.influx.host,
      "influxdb_port" -> env.influx.port.toString,
      "influxdb_user" -> "",
      "influxdb_pass" -> "",
      "influxdb_database" -> env.influx.database,
      "lm_run" -> r.toString,
      "lm_scenario" -> s.id.toString)

    ansible_command(List(env.libvirt.source, env.libvirt.destination), "start.yml", extra)
  }

  def wait_for_call_home: StepRun = (_, _) => {
  }

  def migrate: StepRun = (_, s) => {
    val extra = Map(
      "domain_name" -> domain_name,
      "destination_uri" -> s"qemu+tcp://${env.libvirt.destination}/system",
      "virsh_params" -> ""
    )

    ansible_command(List(env.libvirt.source), "migrate.yml", extra)
  }

  def stop_telegraf: StepRun = (_, _) => {
    ansible_command(List(env.libvirt.source, env.libvirt.destination), "stop.yml")
  }

  def drop_domain: StepRun = (_, _) => {
    val extra = Map("domain_name" -> domain_name)
    ansible_command(List(env.libvirt.source, env.libvirt.destination), "drop_domain.yml", extra)
  }

  def clean_storage: StepRun = (_, _) => {
    ansible_command(List(env.libvirt.source, env.libvirt.destination), "clean_storage.yml")
  }

  def process_step(name: String, runNumber: Int, scenario: Scenario) = {
    val fn = name match {
      case "install_prerequisites" => install_prerequisites
      case "generate_config_drive" => generate_config_drive
      case "boot_vm" => boot_vm
      case "start_telegraf" => start_telegraf
      case "wait_for_call_home" => wait_for_call_home
      case "migrate" => migrate
      case "stop_telegraf" => stop_telegraf
      case "drop_domain" => drop_domain
      case "clean_storage" => clean_storage
    }

    fn(runNumber, scenario)

    context.system.log.info("Executing {}", name)
  }

  def receive: Receive = {
    case "start" => start
    case (Step("nothing"), _, _) => context.parent ! "done"
    case (Step(name), run: Int, scenario: Scenario) => process_step(name, run, scenario)
    case "end" => terminate_clients
    case "processNextEvent" => context.parent ! "done"

    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }



}
