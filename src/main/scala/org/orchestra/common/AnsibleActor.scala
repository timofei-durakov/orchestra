package org.orchestra.actor

import java.io.File

import akka.actor.{Actor, Props}
import scala.collection._

import scala.util.parsing.json.JSONObject

import java.nio.file.Paths

/**
  * Created by tdurakov on 30.03.16.
  */
case class AnsibleCommand(
  hostIps: List[String],
  vmIps: mutable.Set[String],
  playbook: String,
  extra_args: immutable.Map[String, String])


object AnsibleActor {
  def props(playbook_path: String): Props = Props(new AnsibleActor(playbook_path))
}

class AnsibleActor(playbook_path: String) extends Actor {

  var hosts = new File("hosts")

  private def populateHosts(command: AnsibleCommand): Unit = {
    import java.io._
    val pw = new PrintWriter(hosts)
    if (!command.hostIps.isEmpty){
      pw.write("[hard]\n")
      command.hostIps.foreach((h: String) => pw.write(h + "\n"))
    }
    if (!command.vmIps.isEmpty) {
      pw.write("[virtual]\n")
      command.vmIps.foreach((h: String) => pw.write(h + "\n"))
    }

    pw.close
  }

  def execute_command(ac: AnsibleCommand) = {
    import scala.sys.process._
    populateHosts(ac)

    val playbook = Paths.get(playbook_path, ac.playbook).toString

    val cmd = Seq("ansible-playbook", playbook, "-i", hosts.getCanonicalPath, "--extra-vars", JSONObject(ac.extra_args).toString())
    context.system.log.debug("command to be executed \"{}\"", cmd.mkString(" "))
    cmd.!

    context.parent ! "processNextEvent"
  }

  def receive = {
    case x: AnsibleCommand => execute_command(x)
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }
}
