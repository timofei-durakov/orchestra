package org.orchestra.actor

import akka.actor.{ActorRef, Actor, Props}
import org.orchestra.config.{VmTemplate, Cloud}

import org.orchestra.actor.model.{CreateServerResponse, Service, Access, AuthResponse}

/**
  * Created by tdurakov on 20.03.16.
  */

object InstanceConductorActor {
  def props(id: Int, cloud: Cloud, vmTemplate: VmTemplate, scenario: List[String]): Props = Props(
    new InstanceConductorActor(id, cloud, vmTemplate, scenario))
}

class InstanceConductorActor(id: Int, cloud: Cloud, vmTemplate: VmTemplate, scenario: List[String]) extends Actor {
  var access = None: Option[Access]
  var auth = None: Option[ActorRef]
  var nova = None: Option[ActorRef]
  var currentStep = None: Option[Int]
  import context.system

  def init = {
    auth = Some(context.actorOf(AuthActor.props(cloud), "auth"))
    auth.get ! "auth"
  }

  def initNovaClient = {
    val novaService = access.get.serviceCatalog.find((s: Service) => s.`type` == "compute")
    //NOTE: expected to have only one endpoint object here
    val endpoint = novaService.get.endpoints.head
    nova = Some(context.actorOf(NovaActor.props(vmTemplate.name_template.format(id), endpoint.publicURL, access.get.token.id, vmTemplate), "nova"))
  }

  def processNextStep = {
    Thread.sleep(5000)
    if (currentStep isEmpty)
      currentStep = Some(0)
    else
      currentStep = Some(currentStep.get + 1)
    system.log.info("next step is about to be triggered {}", scenario(currentStep.get))
    self ! scenario(currentStep.get)
  }

  def wait_for_active = {
    nova.get ! "wait_for_active"
  }

  def build = {
    nova.get ! "create"
  }
  def liveMigrate = {
    nova.get ! "live-migration"
  }
  def delete = {
    nova.get ! "delete"
  }

  def create_floating_ip = {
    nova.get ! "create_floating_ip"
  }

  def associate_floating_ip = {
    nova.get ! "associate_floating_ip"
  }

  def initPing = {
    nova.get ! "ping_init"
  }

  def stopPing = {
    nova.get ! "ping_stop"
  }

  def receive = {
    case x: AuthResponse => {
      access = Some(x.access)
      initNovaClient
      processNextStep

    }
    case "build" => build
    case "processNextStep" => processNextStep
    case "start" => init
    case "create_floating_ip" => create_floating_ip
    case "associate_floating_ip" => associate_floating_ip
    case "start_ping" => initPing
    case "stop_ping" => stopPing
    case "live_migrate" => liveMigrate
    case "wait_for_active" => wait_for_active
    case "delete" => delete
  }

}
