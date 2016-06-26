package org.orchestra.actor.model

import spray.httpx.SprayJsonSupport

import akka.pattern.pipe
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.json
import spray.json._

import scala.concurrent.Future

/**
  * Created by tdurakov on 19.03.16.
  */
//NOTE: This script was used for spray-json custom protocol testing
object Run extends App {

  case class Color(name: String, red: Int, green: Int, blue: Int)
  case class Tone(name: String, code: Int)
  case class Pencil(name: String, code: Int)
  case class Pallete(owner: String, color: Color, toneList: List[Tone], pMap:Map[String, List[Pencil]])

  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val colorFormat = jsonFormat4(Color)
    implicit val toneFormat = jsonFormat2(Tone)
    implicit val pencilFormat = jsonFormat2(Pencil)
    implicit object PalleteJsonFormat extends RootJsonFormat[Pallete] {
      def write(p: Pallete) = JsObject("owner" -> JsNull, "color" -> p.color.toJson,
        "toneList" -> p.toneList.toJson,
        "pMap" -> p.pMap.toJson)

      def read(value: JsValue) = {
        value.asJsObject.getFields("owner", "color", "toneList", "pMap") match {
          case Seq(owner, color, tList: JsArray, pMap) => {
            val tones = tList.elements.map((t:JsValue) => t.convertTo[Tone]).toList
            new Pallete(if (owner.isInstanceOf[JsNull.type])  null else "dddd" , color.convertTo[Color], tones, pMap.convertTo[Map[String, List[Pencil]]])
          }
        }
      }
    }

  }

  import MyJsonProtocol._
  import spray.json._

  val color = Color("CadetBlue", 95, 158, 160)
  val tone1 = Tone("grey", 1)
  val tone2 = Tone("sepia", 2)
  val toneList:List[Tone] = List(tone1, tone2)
  val pen1 = Pencil("tm", 0)
  val pen2 = Pencil("h1", 1)
  val pen3 = Pencil("h3", 2)
  val pen4 = Pencil("h4", 3)
  val penList1 = List(pen1, pen2)
  val penList2 = List(pen3, pen4)

  val tMap:Map[String, List[Pencil]] = Map("aColl" -> penList1, "bColl" -> penList2 )
  val json1 = Pallete(owner = null, color = color, toneList=toneList, pMap = Map[String, List[Pencil]]()).toJson
  println(json1)
  val result = json1.convertTo[Pallete]
  println(result)

}