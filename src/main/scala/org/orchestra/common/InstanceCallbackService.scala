package org.orchestra.common

/**
  * Created by tdurakov on 24.06.16.
  */

import akka.actor._
import org.orchestra.common.model.InstanceAvailableEvent
import spray.can.Http
import spray.http.HttpMethods.GET
import spray.http.{HttpRequest, HttpResponse}

object InstanceCallbackService {
  def props(scenarioMonitor: ActorRef): Props =
    Props(new InstanceCallbackService(scenarioMonitor))
}

class InstanceCallbackService(scenarioMonitor: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, path, _, _, _) => {
      val req = path.path.tail.head
      scenarioMonitor ! InstanceAvailableEvent(req.toString)
      sender ! HttpResponse(status = 200)
    }
    case r: HttpRequest => {
      context.system.log.warning("unexpected request received by callback service {}", r.toString)
      sender ! HttpResponse(status = 404, entity = "Unknown resource!")
    }


  }
}
