package org.orchestra.actor

import java.io.File

import akka.actor.{Actor, Props}
import scala.collection.mutable.Set


import scala.sys.process.Process

/**
  * Created by tdurakov on 30.03.16.
  */
case class TelegraphStart(hostIps: List[String], vmIps: Set[String])

object TelegraphActor {
  def props(playbook_path: String, runNumber: Int, scenarioId: Int): Props = Props(new TelegraphActor(playbook_path,
    runNumber, scenarioId))
}

class TelegraphActor(playbook_path: String, runNumber: Int, scenarioId: Int) extends Actor {

  var processRef: Process = null
  var hosts = new File("hosts")

  private def populateHosts(command: TelegraphStart): Unit = {
    import java.io._
    val pw = new PrintWriter(hosts)
    pw.write("[hard]\n")
    command.hostIps.foreach((h: String) => pw.write(h + "\n"))
    pw.write("[virtual]\n")
    command.vmIps.foreach((h: String) => pw.write(h + "\n"))
    pw.close
  }

  def execute_start(request: TelegraphStart) = {
    new File(".").getAbsolutePath()

    populateHosts(request)
    val command = "ansible-playbook " + playbook_path + "/start.yml -i " + hosts.getCanonicalPath + " --extra-vars '{\"lm_run\":\"" + runNumber +
      "\",\"lm_scenario\":\"" + scenarioId + "\"}'"
    context.system.log.info("command to be executed {}", command)
    val process = Process(command)
    processRef = process.run()
    processRef.exitValue()
    context.parent ! "processNextEvent"
    processRef = null
  }


  def execute_stop = {
    val command = "ansible-playbook " + playbook_path + "/stop.yml -i " + hosts.getCanonicalPath
    context.system.log.info("command to be executed {}", command)
    val process = Process(command)
    processRef = process.run()
    processRef.exitValue()
    context.parent ! "processNextEvent"
    processRef = null

  }

  override def postStop = {
    if (processRef != null) {
      processRef.destroy()
    }
  }

  def receive = {
    case x: TelegraphStart => execute_start(x)
    case "stop" => execute_stop
  }

}
