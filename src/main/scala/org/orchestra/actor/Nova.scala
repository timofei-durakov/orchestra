package org.orchestra.actor

import akka.actor.{ActorRef, Props, Actor, ActorLogging}
import akka.pattern.pipe
import org.orchestra.actor.model._
import org.orchestra.config.VmTemplate
import spray.client.pipelining._
import spray.http.{HttpResponse, HttpRequest}
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.concurrent.Future

/**
  * Created by tdurakov on 19.03.16.
  */

trait NovaJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val linkFormat = jsonFormat2(Link)
  implicit val flavorFormat = jsonFormat2(Flavor)
  implicit val imageFormat = jsonFormat2(Image)
  implicit val networkAddressFormat = jsonFormat4(NetworkAddress)
  implicit val securityGroupFormat = jsonFormat1(SecurityGroup)

  implicit object serverJsonFormat extends RootJsonFormat[ServerDetails] {
    def write(s: ServerDetails) = JsObject("OS-DCF:diskConfig" -> JsString(s.`OS-DCF:diskConfig`),
      "OS-EXT-AZ:availability_zone" -> JsString(s.`OS-EXT-AZ:availability_zone`),
      "OS-EXT-SRV-ATTR:host" -> JsString(s.`OS-EXT-SRV-ATTR:host`),
      "OS-EXT-SRV-ATTR:hypervisor_hostname" -> JsString(s.`OS-EXT-SRV-ATTR:hypervisor_hostname`),
      "OS-EXT-SRV-ATTR:instance_name" -> JsString(s.`OS-EXT-SRV-ATTR:instance_name`),
      "OS-EXT-STS:power_state" -> JsNumber(s.`OS-EXT-STS:power_state`),
      "OS-EXT-STS:task_state" -> JsString(s.`OS-EXT-STS:task_state`),
      "OS-EXT-STS:vm_state" -> JsString(s.`OS-EXT-STS:vm_state`),
      "OS-SRV-USG:launched_at" -> JsString(s.`OS-SRV-USG:launched_at`),
      "OS-SRV-USG:terminated_at" -> JsString(s.`OS-SRV-USG:terminated_at`),
      "accessIPv4" -> JsString(s.accessIPv4),
      "accessIPv6" -> JsString(s.accessIPv6),
      "addresses" -> s.addresses.toJson,
      "config_drive" -> JsString(s.config_drive),
      "created" -> JsString(s.created),
      "flavor" -> s.flavor.toJson,
      "hostId" -> JsString(s.hostId),
      "id" -> JsString(s.id),
      "image" -> s.image.toJson,
      "key_name" -> (if (s.key_name == null) JsNull else JsString(s.key_name)),
      "links" -> JsArray(s.links.map((l: Link) => l.toJson).toVector),
      "metadata" -> s.metadata.toJson,
      "name" -> JsString(s.name),
      "os-extended-volumes:volumes_attached" -> s.`os-extended-volumes:volumes_attached`.toJson,
      "progress" -> JsNumber(s.progress),
      "security_groups" -> s.security_groups.toJson,
      "status" -> JsString(s.status),
      "tenant_id" -> JsString(s.tenant_id),
      "updated" -> JsString(s.updated),
      "user_id" -> JsString(s.user_id))

    def read(value: JsValue) = {
      val map = value.asJsObject.fields
      val diskConfig = map("OS-DCF:diskConfig").convertTo[String]
      val availabilityZone = map("OS-EXT-AZ:availability_zone").convertTo[String]
      val host = if (map("OS-EXT-SRV-ATTR:host").isInstanceOf[JsNull.type]) null else map("OS-EXT-SRV-ATTR:host").convertTo[String]
      val hypervisorHostName = if (map("OS-EXT-SRV-ATTR:hypervisor_hostname").isInstanceOf[JsNull.type]) null else map("OS-EXT-SRV-ATTR:hypervisor_hostname").convertTo[String]
      val instanceName = map("OS-EXT-SRV-ATTR:instance_name").convertTo[String]
      val powerState = map("OS-EXT-STS:power_state").convertTo[Int]
      val taskState = if (map("OS-EXT-STS:task_state").isInstanceOf[JsNull.type]) null else map("OS-EXT-STS:task_state").convertTo[String]
      val vmState = map("OS-EXT-STS:vm_state").convertTo[String]
      val launchedAt = if (map("OS-SRV-USG:launched_at").isInstanceOf[JsNull.type]) null else map("OS-SRV-USG:launched_at").convertTo[String]
      val terminatedAt = if (map("OS-SRV-USG:terminated_at").isInstanceOf[JsNull.type]) null else map("OS-SRV-USG:terminated_at").convertTo[String]
      val accessIPv4 = map("accessIPv4").convertTo[String]
      val accessIPv6 = map("accessIPv6").convertTo[String]
      val addresses = map("addresses").convertTo[Map[String, List[NetworkAddress]]]
      val configDrive = map("config_drive").convertTo[String]
      val created = map("created").convertTo[String]
      val flavor = map("flavor").convertTo[Flavor]
      val hostId = map("hostId").convertTo[String]
      val id = map("id").convertTo[String]
      val image = map("image").convertTo[Image]
      val keyName = (if (map("key_name").isInstanceOf[JsNull.type]) null else map("key_name").convertTo[String])
      val links = map("links").convertTo[List[Link]]
      val metadata = map("metadata").convertTo[Map[String, String]]
      val name = map("name").convertTo[String]
      val volumesAttached = map("os-extended-volumes:volumes_attached").convertTo[List[String]]
      val progress = map("progress").convertTo[Int]
      val securityGroups = map("security_groups").convertTo[List[SecurityGroup]]
      val status = map("status").convertTo[String]
      val tenantId = map("tenant_id").convertTo[String]
      val updated = map("updated").convertTo[String]
      val userId = map("user_id").convertTo[String]
      new ServerDetails(diskConfig, availabilityZone, host, hypervisorHostName, instanceName, powerState.toInt, taskState,
        vmState, launchedAt, terminatedAt, accessIPv4, accessIPv6, addresses,
        configDrive, created, flavor, hostId, id, image, keyName,
        links, metadata, name, volumesAttached,
        progress, securityGroups, status, tenantId, updated, userId)
    }
  }

  implicit val detailServersResponseFormat = jsonFormat1(DetailServersResponse)
  implicit val networkFormat = jsonFormat1(Network)
  implicit val createServerFormat = jsonFormat6(CreateServer)
  implicit val createServerRequestFormat = jsonFormat1(CreateServerRequest)
  implicit val createServerResponseFormat = jsonFormat5(CreateServerResponse)
  implicit val createServerResponseWrapperFormat = jsonFormat1(CreateServerResponseWrapper)

  implicit object liveMigrationFormat extends RootJsonFormat[LiveMigration] {
    def write(lm: LiveMigration) = JsObject(
      "host" -> (if (lm.host.isEmpty) JsNull else JsString(lm.host.get)),
      "block_migration" -> JsBoolean(lm.block_migration),
      "disk_over_commit" -> JsBoolean(lm.disk_over_commit)
    )

    def read(value: JsValue) = {
      val map = value.asJsObject.fields
      val host = if (map.contains("host")) Some(map("host").convertTo[String]) else None: Option[String]
      val block_migration = map("block_migration").convertTo[Boolean]
      val disk_over_commit = map("disk_over_commit").convertTo[Boolean]
      new LiveMigration(host, block_migration, disk_over_commit)
    }
  }

  implicit val liveMigrationRequestFormat = jsonFormat1(LiveMigrationRequest)
  implicit val detailServerResponseFormat = jsonFormat1(DetailServerResponse)
  implicit val floatingIPFormat = jsonFormat5(FloatingIP)
  implicit val floatingIPResponseFormat = jsonFormat1(FloatingIPResponse)
  implicit val floatingIPAddressFormat = jsonFormat1(FloatingIPAddress)
  implicit val AddFloatingIPRequestFormat = jsonFormat1(AddFloatingIPRequest)
}

object NovaActor {
  def props(instanceName: String, endpoint: String, token: String, vmTemplate: VmTemplate): Props =
    Props(new NovaActor(instanceName, endpoint, token, vmTemplate))
}


class NovaActor(instanceName: String, endpoint: String, var token: String, val vmTemplate: VmTemplate) extends Actor
  with NovaJsonSupport with ActorLogging {

  var serverId = None: Option[String]
  var statusToWait = None: Option[String]
  var requestedOperation = None: Option[String]
  var floatingIP = None: Option[String]
  var ping = None: Option[ActorRef]
  import context.dispatcher

  def list: Unit = {
    val pipeline: HttpRequest => Future[DetailServersResponse] =
      (
        addHeader("X-Auth-Token", token)
          ~> sendReceive
          ~> unmarshal[DetailServersResponse]
        )
    val response: Future[DetailServersResponse] = pipeline(Get(endpoint + "/servers/detail"))

    response.pipeTo(self)
  }


  def create = {
    val network = List(Network(vmTemplate.networkRef))

    val createServer = CreateServer(instanceName, vmTemplate.imageRef, vmTemplate.flavorRef, network)
    val pipeline: HttpRequest => Future[CreateServerResponseWrapper] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
        ~> unmarshal[CreateServerResponseWrapper]
      )
    val response: Future[CreateServerResponseWrapper] = pipeline(Post(endpoint + "/servers", CreateServerRequest(createServer)))
    response.pipeTo(self)
  }

  def liveMigration = {
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
      )
    val liveMigration = LiveMigrationRequest(LiveMigration(block_migration = true))
    val response: Future[HttpResponse] = pipeline(Post(endpoint + "/servers/" + serverId.get + "/action", liveMigration))
    requestedOperation = Some("live_migration")
    response.pipeTo(self)
  }

  def delete = {
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
      )

    val response: Future[HttpResponse] = pipeline(Delete(endpoint + "/servers/" + serverId.get))
    requestedOperation = Some("delete")
    response.pipeTo(sender())
  }

  def details = {
    val response = getDetails
    response.pipeTo(sender())
  }

  def createFloatingIP = {
    val pipeline: HttpRequest => Future[FloatingIPResponse] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
        ~> unmarshal[FloatingIPResponse]
      )
    val response: Future[FloatingIPResponse] = pipeline(Post(endpoint + "/os-floating-ips"))
    response.pipeTo(self)
  }

  def associateFloatingIP = {
    val floatingIpAdress = FloatingIPAddress(address = floatingIP.get)
    val addFloatingIpRequest = AddFloatingIPRequest(addFloatingIp = floatingIpAdress)
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
        ~> unmarshal[HttpResponse]
      )
    val response: Future[HttpResponse] = pipeline(Post(endpoint + "/servers/" + serverId.get + "/action",
      addFloatingIpRequest))
    requestedOperation = Some("associate_floating_ip")
    response.pipeTo(self)
  }

  def manageCreatedFloatingIp(createdFloatingIP: FloatingIP) = {
    floatingIP = Some(createdFloatingIP.ip)
    context.parent ! "processNextStep"
  }

  private def getDetails = {
    val pipeline: HttpRequest => Future[DetailServerResponse] = (
      addHeader("X-Auth-Token", token)
        ~> sendReceive
        ~> unmarshal[DetailServerResponse]
      )
    val response: Future[DetailServerResponse] = pipeline(Get(endpoint + "/servers/" + serverId.get))
    response
  }

  def dispatchCreatedInstance(createdInstance: CreateServerResponseWrapper) = {
    serverId = Some(createdInstance.server.id)
    context.parent ! "processNextStep"
  }

  def wait_for_active = {
    import context.system
    system.log.info("wait for active while current task status is {}", statusToWait.getOrElse("None"))
    if (statusToWait.isEmpty) {
      statusToWait = Some("ACTIVE")
    }
    val response = getDetails
    response.pipeTo(self)
  }

  def verifyStatus(status: DetailServerResponse) = {
    import context.system
    system.log.info("in verifyStatus method statusToWait={} receivedStatus={}", statusToWait.get, status.server.status)
    if (status.server.status == statusToWait.get) {
      statusToWait = None
      context.parent ! "processNextStep"
    } else {
      wait_for_active
    }
  }

  def dispatchResponse(response: HttpResponse) = {
    if (response.status.intValue == 202 || response.status.intValue == 200) {
      requestedOperation = None
      context.parent ! "processNextStep"
    }
  }
  def pingStart = {
    ping = Some(context.actorOf(PingActor.props(floatingIP.get), "ping"))
    ping.get ! "start"
    context.parent ! "processNextStep"
  }

  def pingStop = {
    ping.get ! "stop"
    context.parent ! "processNextStep"
  }

  def receive = {
    case "detailed-list" => list
    case "create" => create
    case "create_floating_ip" => createFloatingIP
    case "associate_floating_ip" => associateFloatingIP
    case "delete" => delete
    case "live-migration" => liveMigration
    case "details" => details
    case "wait_for_active" => wait_for_active
    case "ping_init" => pingStart
    case "ping_stop" => pingStop
    case result: CreateServerResponseWrapper => dispatchCreatedInstance(result)
    case result: DetailServerResponse => verifyStatus(result)
    case result: FloatingIPResponse => manageCreatedFloatingIp(result.floating_ip)
    case result: HttpResponse => dispatchResponse(result)
  }
}