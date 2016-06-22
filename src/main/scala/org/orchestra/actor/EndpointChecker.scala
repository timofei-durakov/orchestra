package org.orchestra.actor

import scala.language.postfixOps
import java.net.URI
import spray.client.pipelining._
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Props, Actor}
import akka.pattern.pipe
import spray.client.pipelining._
import org.orchestra.actor.model.AuthResponse
import org.orchestra.config.Cloud

/**
  * Created by tdurakov on 22/06/16.
  */

object EndpointChecker {
  def props(influx: ActorRef, cloud: Cloud, checkCooldown: Double): Props =
    Props(new EndpointChecker(influx, cloud, checkCooldown))
}

class EndpointChecker(influx: ActorRef, cloud: Cloud, checkCooldown: Double) extends Actor{


  var auth = None: Option[ActorRef]
  val endpoints: collection.mutable.Set[String] = collection.mutable.Set()
  var token: Option[String] = None
  import context.dispatcher




  def init = {
    auth = Some(context.actorOf(AuthActor.props(cloud), "auth"))
    auth.get ! "auth"
  }

  def buildEndpoint(uri:URI) = {
    uri.getScheme + "://" + uri.getAuthority
  }


  def start_checking(authResponse:AuthResponse) = {
    token = Some(authResponse.access.token.id)
    val endpointList = authResponse.access.serviceCatalog.map  {
      x => {
        x.`type` match {
          case "compute" => {
            endpoints += buildEndpoint(URI.create(x.endpoints.head.adminURL))
          }
          case "volume" => {
            endpoints += buildEndpoint(URI.create(x.endpoints.head.adminURL))
          }
          case "image" => {
            endpoints += buildEndpoint(URI.create(x.endpoints.head.adminURL))
          }
          case "network" => {
            endpoints += buildEndpoint(URI.create(x.endpoints.head.adminURL))
          }
          case _ => {}
        }
      }
    }
    context.system.scheduler.scheduleOnce(checkCooldown seconds, self, "check")
  }

  def check = {
    for (endpoint <- endpoints) {
      val pipeline: HttpRequest => Future[HttpResponse] = (
        addHeader("X-Auth-Token", token.get)
          ~> sendReceive
          ~> unmarshal[HttpResponse]
        )
      val response: Future[HttpResponse] = pipeline(Get(endpoint))
      response.pipeTo(self)
    }
  }

  def handleHttpResponse(resp: HttpResponse) = {
    print(resp.status.intValue)
  }

  def receive = {
    case "init" => init
    case x: AuthResponse => start_checking(x)
    case "check" => {
      context.system.scheduler.scheduleOnce(checkCooldown seconds, self, "check")
      check
    }
    case x: HttpResponse => handleHttpResponse(x)
    case _ => context.system.log.info("unexpected message received")
  }
}
