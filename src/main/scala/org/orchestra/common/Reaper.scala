package org.orchestra.common

import akka.actor.{Actor, ActorRef, Props, Terminated}

import scala.collection.mutable.Set

object Reaper {

  case class WatchСonductor(ref: ActorRef)

  case class WatchClient(ref: ActorRef)

  def props: Props = Props(new Reaper)
}


class Reaper extends Actor {

  import Reaper._

  val watchedConductors = Set.empty[ActorRef]
  val watchedClients = Set.empty[ActorRef]

  // Derivations need to implement this method.  It's the
  // hook that's called when everything's dead
  def allSoulsReaped() = {
    context.parent ! "finish_event_triggered"
  }

  def allClientsReaped = {
    context.system.shutdown()
  }

  // Watch and check for termination
  def receive = {
    case WatchСonductor(ref) => {
      context.system.log.debug("ref={} received", ref)
      context.watch(ref)
      watchedConductors += ref
    }
    case WatchClient(ref) => {
      context.system.log.debug("ref={} received", ref)
      context.watch(ref)
      watchedClients += ref
    }
    case Terminated(ref) => {
      context.system.log.debug("terminated event received {}", ref)
      if (watchedConductors.contains(ref)) {
        watchedConductors -= ref
        if (watchedConductors.isEmpty) allSoulsReaped
      } else if (watchedClients.contains(ref)) {
        watchedClients -= ref
        if (watchedClients.isEmpty) allClientsReaped
      }
    }
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }
}

