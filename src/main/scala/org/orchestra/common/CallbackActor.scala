package org.orchestra.common

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import akka.io.IO
import akka.util.Timeout
import org.orchestra.common.Reaper.WatchClient
import org.orchestra.common.model.InstanceAvailableEvent
import org.orchestra.libvirt.config.Callback
import spray.can.Http


object CallbackActor {
  def props(callback: Callback, reaper: ActorRef): Props =
    Props(new CallbackActor(callback.host, callback.port, reaper))
}

class CallbackActor(host:String, port:Int, reaper: ActorRef) extends Actor {

//  var callback_listener: ActorRef = null
  val callback_service = context.actorOf(InstanceCallbackService.props(context.self), name="cb_service")
  reaper ! WatchClient(callback_service)

  implicit val system = context.system
  IO(Http) ! Http.Bind(callback_service, interface="0.0.0.0", port=port)



  override def receive: Receive = {
//    case "wait" => Thread.sleep(1000); self ! InstanceAvailableEvent("")
    case x: InstanceAvailableEvent => context.parent ! "processNextEvent"

    case Http.Bound(address) => {
      context.system.log.debug("http bound event received: {}", address)
      val callback_listener = sender()
      reaper ! WatchClient(callback_listener)
    }
//    case Http.Unbound => {
//      callback_listener ! PoisonPill
//    }
  }

  override def postStop = {
//    if(callback_listener != null)
//    {
//      callback_listener ! Http.Unbind
//      context.system.log.debug("http unbind")
//    }
  }

}
