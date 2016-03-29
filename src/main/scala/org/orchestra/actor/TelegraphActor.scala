package org.orchestra.actor

import akka.actor.{Actor, Props}

import scala.sys.process.Process

/**
  * Created by tdurakov on 30.03.16.
  */

object TelegraphActor {
  def props(runNumber:Int, scenarioId:Int): Props = Props(new TelegraphActor(runNumber, scenarioId))
}

class TelegraphActor(runNumber: Int, scenarioId: Int) extends Actor {

  var processRef: Process = null

  def execute_start = {
    val command = "ansible-playbook start.yml -i hosts --extra-vars '{\"lm_run\":\"" + runNumber +
      "\",\"lm_scenario\":\"" + scenarioId + "\"}'"
    val process = Process(command)
    processRef = process.run()
    processRef.exitValue()
    context.parent ! "processNextEvent"
    processRef = null
  }


  def execute_stop = {
    val command = "ansible-playbook stop.yml -i hosts"
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
    case "start" => execute_start
    case "stop" => execute_stop
  }

}
