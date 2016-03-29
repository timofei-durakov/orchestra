package org.orchestra.actor

import akka.actor.{ActorRef, Props, Actor}
import scala.sys.process.ProcessIO
import scala.sys.process.Process

/**
  * Created by tdurakov on 22.03.16.
  */

object PingActor {
  def props(vmName: String, address: String, runNumber: Int, scenarioId: Int, influx: ActorRef): Props =
    Props(new PingActor(vmName, address, runNumber, scenarioId, influx))
}

class PingActor(vmName: String, address: String, runNumber: Int, scenarioId: Int, influx: ActorRef) extends Actor {

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
    val pattern = "\\[(.*?)\\]".r
    val timestamp = pattern.findFirstIn(message)
    var time = "0"
    if (!timestamp.isEmpty) {
      var ts = timestamp.get
      ts = ts.substring(1, ts.length - 1)
      if (!message.endsWith("Unreachable")) {
        val timeStart = message.lastIndexOf("=")
        val timeEnd = message.lastIndexOf(" ")
        time = message.substring(timeStart + 1, timeEnd)
        if (!instance_reachable) {
          instance_reachable = true
          context.parent ! "processNextStep"
        }
      }
      val writeData = "pings,vm_name=" + vmName + ",address=" + address + ",run_number=" + runNumber +
        " time=" + time + " " + ts.substring(0, ts.indexOf("."))
      influx ! writeData
    }

  }

  def receive = {
    case "start" => pingStart
    case "stop" => pingStop
    case _ => context.system.log.info("unexpected message received")
  }

}
