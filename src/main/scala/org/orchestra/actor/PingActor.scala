package org.orchestra.actor

import java.util.Calendar

import akka.actor.{Props, Actor}
import scala.sys.process.ProcessIO
import scala.sys.process.Process
/**
  * Created by tdurakov on 22.03.16.
  */

case class PingStart(frequency:Int)
case class PingStop()


object PingActor{
  def props(address: String): Props =
    Props(new PingActor(address))
}



class PingActor(address: String) extends Actor {

  var processRef = None: Option[Process]

  def pingStart = {
    val command = "ping -i 0.1 %s".format(address)
    val pio = new ProcessIO(_ => (),
      stdout => scala.io.Source.fromInputStream(stdout)
        .getLines.foreach(handle),
      _ => ())
    val process = Process(command)
    processRef = Some(process.run(pio))
  }

  def pingStop = {
    processRef.get.destroy()
  }

  def handlePingMessage(message: String) = {
    println(message)
  }

  def receive = {
    case "start" => pingStart
    case "stop" => pingStop
  }

  def main(args: Array[String]): Unit = {
    val seq = "ping ya.ru -i 0.1"
    val pio = new ProcessIO(_ => (),
      stdout => scala.io.Source.fromInputStream(stdout)
        .getLines.foreach(handle),
      _ => ())
    val process = Process(seq)
    val procRef = process.run(pio)
    Thread.sleep(4000)
    println(procRef.exitValue())
    procRef.destroy()

    println("process finished")
  }
    def handle(s:String) ={
      print("> ")
      println(s)
    }


}
