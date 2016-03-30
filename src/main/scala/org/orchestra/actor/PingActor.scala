package org.orchestra.actor

import akka.actor.{ActorRef, Props, Actor}
import scala.sys.process.ProcessIO
import scala.sys.process.Process

/**
  * Created by tdurakov on 22.03.16.
  */

object PingActor {
  def props(domain_id: String, vmName: String, address: String, runNumber: Int, scenarioId: Int, influx: ActorRef): Props =
    Props(new PingActor(domain_id, vmName, address, runNumber, scenarioId, influx))
}

class PingActor(domain_id: String, vmName: String, address: String, runNumber: Int, scenarioId: Int, influx: ActorRef) extends Actor {

  var processRef = None: Option[Process]
  var instance_reachable = false

  def pingStart = {
    context.system.log.info("ping start for server={} triggered", vmName)
    val command = "ping -i 0.2 -D %s".format(address)
    val pio = new ProcessIO(_ => (),
      stdout => scala.io.Source.fromInputStream(stdout)
        .getLines.foreach(handlePingMessage),
      _ => ())
    val process = Process(command)
    processRef = Some(process.run(pio))
  }

  def pingStop = {
    context.system.log.info("ping process termination for server={} with pid={} triggered", vmName,
      processRef.get.toString)
    processRef.get.destroy()
  }

  def handlePingMessage(message: String) = {
    context.system.log.info("ping message='{}' for server={} received", message, vmName)
    var time = "0"
    if (message.endsWith("ms")) {
      if (!instance_reachable) {
        instance_reachable = true
        context.parent ! "processNextStep"
      }
      val timeStart = message.lastIndexOf("=")
      val timeEnd = message.lastIndexOf(" ")
      time = message.substring(timeStart + 1, timeEnd)
    }
    val pattern = "\\[(.*?)\\]".r
    val timestamp = pattern.findFirstIn(message)
    var ts: Long = 0
    if (timestamp.isEmpty) {
      ts = System.currentTimeMillis() * 1000000
    } else {
      ts = timestamp.get.substring(1, timestamp.get.length - 1).replace(".", "").toLong * 1000
    }
    val writeData = "pings,domain_name=" + domain_id + ",vm_name=" + vmName + ",address=" + address + ",scenario=" + scenarioId +
      ",run=" + runNumber + " value=" + time + " " + ts
    influx ! writeData
    //    }

  }

  def receive = {
    case "start" => pingStart
    case "stop" => pingStop
    case _ => context.system.log.info("unexpected message received")
  }

}
