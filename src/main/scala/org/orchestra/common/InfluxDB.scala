package org.orchestra.common

import akka.actor.{Props, Actor}
import akka.pattern.pipe
import spray.client.pipelining._


import spray.http.{HttpResponse, HttpRequest}

import scala.collection.mutable.ArrayBuffer
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
  val buffer = ArrayBuffer.empty[String]
  val bufferCapacity = 20
  def sendData(message:String) = {
    buffer.append(message)
    if (buffer.length >= bufferCapacity) {
      flushBuffer
      buffer.clear
    }
  }
  def flushBuffer = {
    val message = buffer.mkString("\n")
    context.system.log.debug("Messages '{}' are about to be send to influx", message)
    val pipeline: HttpRequest => Future[HttpResponse] = (
        sendReceive
        ~> unmarshal[HttpResponse]
      )
    val response: Future[HttpResponse] = pipeline(Post(endpoint +"/write?db="+ database, message))
    response.pipeTo(self)
  }

  def handleResponse(response: HttpResponse) = {
    context.system.log.debug("http response received from influxdb code={}", response.status.intValue)
  }

  override def postStop = {
    if (!buffer.isEmpty) {
      flushBuffer
    }
  }

  def receive = {
    case message: String => sendData(message)
    case response: HttpResponse => handleResponse(response)
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }

}
