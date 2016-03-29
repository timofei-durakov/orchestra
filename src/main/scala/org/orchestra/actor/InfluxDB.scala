package org.orchestra.actor

import akka.actor.{Props, Actor}
import akka.pattern.pipe
import spray.client.pipelining._

import org.orchestra.config.{VmTemplate, Cloud}

import org.orchestra.actor.model._
import spray.http.{HttpResponse, HttpRequest}

import scala.concurrent.Future


/**
  * Created by tdurakov on 28.03.16.
  */
object InfluxDB {
  def props(endpoint: String, database: String): Props = Props(
    new InfluxDB(endpoint, database))
}


class InfluxDB(endpoint: String, database: String) extends Actor{

  import context.dispatcher

  def sendData(message:String) = {
    val pipeline: HttpRequest => Future[HttpResponse] = (
        sendReceive
        ~> unmarshal[HttpResponse]
      )
    val response: Future[HttpResponse] = pipeline(Post(endpoint +"/write?db="+ database, message))
    response
  }

  def handleResponse(response: HttpResponse) = {
    //TODO: deal with responses
  }

  def receive = {
    case message: String => sendData(message)
    case response: HttpResponse => handleResponse(response)
  }

}
