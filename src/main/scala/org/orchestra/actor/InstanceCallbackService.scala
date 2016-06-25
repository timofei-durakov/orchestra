package org.orchestra.actor

/**
  * Created by tdurakov on 24.06.16.
  */

import akka.actor._
import akka.io.IO
import org.orchestra.actor.Reaper.WatchClient
import org.orchestra.actor.model.{InstanceAvailableEvent}
import spray.http.HttpMethods.{GET}
import spray.can.Http
import spray.http.{HttpRequest, HttpResponse}

object InstanceCallbackService {
  def props(scenarioMonitor: ActorRef): Props =
    Props(new InstanceCallbackService(scenarioMonitor))
}

class InstanceCallbackService(scenarioMonitor: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, path, _, _, _) => {
      var req = path.path.tail.head
      scenarioMonitor ! InstanceAvailableEvent(req.toString)
      sender ! HttpResponse(status = 200)
    }
    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")


  }
}
