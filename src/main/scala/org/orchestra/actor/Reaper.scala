package org.orchestra.actor


import akka.actor.{Props, Actor, ActorRef, Terminated}
import scala.collection.mutable.ArrayBuffer

object Reaper {

  case class WatchMe(ref: ActorRef)

  def props: Props = Props(new Reaper)
}

class Reaper extends Actor {

  import Reaper._

  val watched = ArrayBuffer.empty[ActorRef]

  // Derivations need to implement this method.  It's the
  // hook that's called when everything's dead
  def allSoulsReaped() = context.system.shutdown()

  // Watch and check for termination
  def receive = {
    case WatchMe(ref) =>
      context.system.log.info("ref={} received", ref)
      context.watch(ref)
      watched += ref
    case Terminated(ref) =>
      watched -= ref
      if (watched.isEmpty) allSoulsReaped
    case _: Any => context.system.log.info(_)
  }
}