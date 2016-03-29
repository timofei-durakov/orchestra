package org.orchestra.actor

import akka.actor.{Props, ActorRef, Actor}

/**
  * Created by tdurakov on 29.03.16.
  */


object CountdownLatch {
  def props(amount: Int): Props = Props(
    new CountdownLatch(amount))
}

class CountdownLatch(amount: Int) extends Actor {

  val reported =  scala.collection.mutable.ListBuffer.empty[ActorRef]

  def receive = {
    case x: ActorRef => {
      context.system.log.info("event received from {}", x.toString())
      reported += x
      if (reported.length == amount) {
        context.system.log.info("all events received")
        context.parent ! "countdown_latch_triggered"
      }
    }
    case _ => context.system.log.info("unexpected message received")
  }

}
