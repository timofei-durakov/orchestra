package org.orchestra.actor

import java.io.File

import akka.actor.{Actor, Props}
import scala.collection.mutable.Set


import scala.sys.process.Process

/**
  * Created by tdurakov on 30.03.16.
  */
case class AnsibleCommand(hostIps: List[String], vmIps: Set[String], script: String, extra_args:List[String])

object AnsibleActor {
  def props(playbook_path: String): Props = Props(new AnsibleActor(playbook_path))
}

class AnsibleActor(playbook_path: String) extends Actor {

  var processRef: Process = null
  var hosts = new File("hosts")

  private def populateHosts(command: AnsibleCommand): Unit = {
    import java.io._
    val pw = new PrintWriter(hosts)
    pw.write("[hard]\n")
    command.hostIps.foreach((h: String) => pw.write(h + "\n"))
    pw.write("[virtual]\n")
    command.vmIps.foreach((h: String) => pw.write(h + "\n"))
    pw.close
  }

  def execute_command(ac: AnsibleCommand) = {
    populateHosts(ac)
    val command =  ac.script +" "+ playbook_path + " " + hosts.getCanonicalPath + " " + ac.extra_args.mkString(" ")
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
    case x: AnsibleCommand => execute_command(x)
    case _ => context.system.log.info("unexpected message received")
  }

}
