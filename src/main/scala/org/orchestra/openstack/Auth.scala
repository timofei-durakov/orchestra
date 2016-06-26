package org.orchestra.actor

import akka.actor.{Actor, Props}
import akka.pattern.pipe
import org.orchestra.actor.model.{Access, Auth, AuthRequest, AuthResponse, Endpoint, Metadata, PasswordCredentials, Role, Service, Tenant, Token, Trust, User}
import org.orchestra.config.Cloud
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future
/**
  * Created by tdurakov on 18.03.16.
  */


// collect your json format instances into a support trait:
trait AuthJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val passwordCredentialsFormat = jsonFormat2(PasswordCredentials)
  implicit val authFormat = jsonFormat2(Auth)
  implicit val authRequestFormat = jsonFormat1(AuthRequest)
  implicit val endpointFormat = jsonFormat5(Endpoint)
  implicit val trustFormat = jsonFormat4(Trust)
  implicit val metadataFormat = jsonFormat2(Metadata)
  implicit val roleFormat = jsonFormat1(Role)
  implicit val userFormat = jsonFormat5(User)
  implicit val tenantFormat = jsonFormat4(Tenant)
  implicit val tokenFormat = jsonFormat4(Token)
  implicit val serviceFormat = jsonFormat4(Service)
  implicit val accessFormat = jsonFormat5(Access)
  implicit val authResponseFormat = jsonFormat1(AuthResponse)
}

object AuthActor {

  def props(cloud: Cloud): Props = Props(new AuthActor(cloud))
}

class AuthActor(cloud: Cloud) extends Actor  with AuthJsonSupport{

  import context.dispatcher

  val passwordCredential = PasswordCredentials(username = cloud.username, password = cloud.password)

  val authObject = AuthRequest(auth=Auth(tenantName = cloud.projectname, passwordCredentials = passwordCredential))
  def auth ={
    val pipeline: HttpRequest => Future[AuthResponse] =
      (
        sendReceive
          ~> unmarshal[AuthResponse]
        )
    val response: Future[AuthResponse] = pipeline(Post(cloud.auth_url, authObject))
    println(sender())
    response.pipeTo(sender())
  }

  def read(x: AuthResponse) = {
    println(x)
  }

  def receive = {
    case "auth" => auth
    case x: AuthResponse => read(x)
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }

}
