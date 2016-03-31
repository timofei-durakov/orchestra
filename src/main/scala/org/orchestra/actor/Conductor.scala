package org.orchestra.actor

import akka.actor._
import akka.pattern.pipe
import spray.client.pipelining._

import org.orchestra.config.{VmTemplate, Cloud}

import org.orchestra.actor.model._
import spray.http.{HttpResponse, HttpRequest}
import spray.httpx.SprayJsonSupport
import spray.json._

import scala.concurrent.Future

/**
  * Created by tdurakov on 20.03.16.
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
      try {
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
        val securityGroups = if(map.contains("security_groups")) map("security_groups").convertTo[List[SecurityGroup]] else null
        val status = map("status").convertTo[String]
        val tenantId = map("tenant_id").convertTo[String]
        val updated = map("updated").convertTo[String]
        val userId = map("user_id").convertTo[String]
        new ServerDetails(diskConfig, availabilityZone, host, hypervisorHostName, instanceName, powerState.toInt, taskState,
          vmState, launchedAt, terminatedAt, accessIPv4, accessIPv6, addresses,
          configDrive, created, flavor, hostId, id, image, keyName,
          links, metadata, name, volumesAttached,
          progress, securityGroups, status, tenantId, updated, userId)
      } catch {
        case e:Throwable => {
          println(e.getMessage)
          null
        }
      }

    }
  }

  implicit val detailServersResponseFormat = jsonFormat1(DetailServersResponse)
  implicit val networkFormat = jsonFormat1(Network)
  implicit val createServerFormat = jsonFormat8(CreateServer)
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

object InstanceConductorActor {
  def props(id: Int, cloud: Cloud, vmTemplate: VmTemplate, scenario: List[String],
            runNumber: Int, scenarioId: Int, influx:ActorRef, countdownLath: ActorRef): Props = Props(
    new InstanceConductorActor(id, cloud, vmTemplate, scenario, runNumber, scenarioId, influx, countdownLath))
}

class InstanceConductorActor(id: Int, cloud: Cloud, vmTemplate: VmTemplate, scenario: List[String], runNumber: Int,
                             scenarioId: Int, influx: ActorRef, countdownLatch: ActorRef)
  extends Actor with NovaJsonSupport with ActorLogging {
  var access = None: Option[Access]
  var auth = None: Option[ActorRef]
  var currentStep = None: Option[Int]
  var serverId = None: Option[String]
  var statusToWait = None: Option[String]
  var requestedOperation = None: Option[String]
  var floatingIP = None: Option[String]
  var floatingIPId = None: Option[String]
  var ping = None: Option[ActorRef]
  var endpoint = None: Option[String]
  val instanceName = vmTemplate.name_template.format(id)
  var domain_id: String = null

  import context.dispatcher
//  import context.system

  def init = {
    auth = Some(context.actorOf(AuthActor.props(cloud), "auth"))
    auth.get ! "auth"
  }

  def initComputeEndpoint = {
    val novaService = access.get.serviceCatalog.find((s: Service) => s.`type` == "compute")
    //NOTE: expected to have only one endpoint object here
    val compute_endpoint = novaService.get.endpoints.head
    endpoint = Some(compute_endpoint.publicURL)
  }

  def processNextStep = {
    if (currentStep.isEmpty)
      currentStep = Some(0)
    else
      currentStep = Some(currentStep.get + 1)

    if (currentStep.get == scenario.length) {
      context.system.log.info("shutting down worker for {}", instanceName)
      context.children.foreach((a: ActorRef) => context stop a)
      context stop self
    } else {
      context.system.log.info("next step is about to be triggered {} for instance {}", scenario(currentStep.get), instanceName)
      self ! scenario(currentStep.get)
    }

  }

  def build = {
    context.system.log.info("build operation started for server {}", instanceName)
    val network = List(Network(vmTemplate.networkRef))
    val createServer = CreateServer(instanceName, vmTemplate.imageRef, vmTemplate.az, vmTemplate.flavorRef, network, vmTemplate.key_name)
    val pipeline: HttpRequest => Future[CreateServerResponseWrapper] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
        ~> unmarshal[CreateServerResponseWrapper]
      )
    val response: Future[CreateServerResponseWrapper] = pipeline(Post(endpoint.get + "/servers", CreateServerRequest(createServer)))
    response.pipeTo(self)
  }

  def handleCreatedServer(response: CreateServerResponse) = {
    context.system.log.info("response received for server creation {}", instanceName)
    serverId = Some(response.id)
    self ! "processNextStep"
  }

  def create_floating_ip = {
    context.system.log.info("floating ip creation started for server {}", instanceName)
    val pipeline: HttpRequest => Future[FloatingIPResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
        ~> unmarshal[FloatingIPResponse]
      )
    val response: Future[FloatingIPResponse] = pipeline(Post(endpoint.get + "/os-floating-ips"))
    response.pipeTo(self)
  }

  def handleCreatedFloatingIP(response: FloatingIP) = {
    context.system.log.info("response received for floating ip creation, server {} floatingIP {}", instanceName, response.ip)
    floatingIP = Some(response.ip)
    floatingIPId = Some(response.id)
    self ! "processNextStep"
  }

  def delete_floating_ip = {
    context.system.log.info("floating ip deletion started for server {}", instanceName)
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
        ~> unmarshal[HttpResponse]
      )
    val response: Future[HttpResponse] = pipeline(Delete(endpoint.get + "/os-floating-ips/" + floatingIPId.get))
    requestedOperation = Some("delete_floating_ip")
    response.pipeTo(self)
  }

  def waitForActive = {
    import context.system
    system.log.debug("wait for active while current task status is {} for server {}", statusToWait.getOrElse("None"),
      instanceName)
    if (statusToWait.isEmpty) {
      statusToWait = Some("ACTIVE")
    }
    val response = getDetails
    response.pipeTo(self)
  }

  def verifyStatus(status: DetailServerResponse) = {
    import context.system
    system.log.debug("in verifyStatus method statusToWait={} receivedStatus={} for server {}", statusToWait.get,
      status.server.status, instanceName)
    if (domain_id == null)
      domain_id = status.server.`OS-EXT-SRV-ATTR:instance_name`
    if (status.server.status == statusToWait.get) {
      statusToWait = None
      self ! "processNextStep"
    } else {
      waitForActive
    }
  }

  private def getDetails = {
    context.system.log.debug("server details request is about to start for server {}", instanceName)
    val pipeline: HttpRequest => Future[DetailServerResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
        ~> unmarshal[DetailServerResponse]
      )
    val response: Future[DetailServerResponse] = pipeline(Get(endpoint.get + "/servers/" + serverId.get))
    response
  }

  def handleDetailList(response: DetailServersResponse) = {
    context.system.log.info("servers details received")
    response.servers.foreach((d: ServerDetails) => println(d.id + " " + d.status))
  }

  def details = {
    val response = getDetails
    response.pipeTo(self)
  }

  def liveMigrate = {
    context.system.log.info("live migration is triggered for server {}", instanceName)
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
      )
    val liveMigration = LiveMigrationRequest(LiveMigration())
    val response: Future[HttpResponse] = pipeline(Post(endpoint.get + "/servers/" + serverId.get + "/action", liveMigration))
    requestedOperation = Some("live_migration")
    response.pipeTo(self)
  }

  def delete = {
    context.system.log.info("delete is triggered for server {}", instanceName)
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
      )

    val response: Future[HttpResponse] = pipeline(Delete(endpoint.get + "/servers/" + serverId.get))
    requestedOperation = Some("delete")
    response.pipeTo(self)
  }

  def associate_floating_ip = {
    context.system.log.info("associate floating ip is triggered for server {} ip {}", instanceName, floatingIP)
    val floatingIpAdress = FloatingIPAddress(address = floatingIP.get)
    val addFloatingIpRequest = AddFloatingIPRequest(addFloatingIp = floatingIpAdress)
    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader("X-Auth-Token", access.get.token.id)
        ~> sendReceive
        ~> unmarshal[HttpResponse]
      )
    val response: Future[HttpResponse] = pipeline(Post(endpoint.get + "/servers/" + serverId.get + "/action",
      addFloatingIpRequest))
    requestedOperation = Some("associate_floating_ip")
    response.pipeTo(self)
    context.parent ! floatingIpAdress
  }

  def list = {
    context.system.log.info("servers detailed list is triggered")
    val pipeline: HttpRequest => Future[DetailServersResponse] =
      (
        addHeader("X-Auth-Token", access.get.token.id)
          ~> sendReceive
          ~> unmarshal[DetailServersResponse]
        )
    val response: Future[DetailServersResponse] = pipeline(Get(endpoint + "/servers/detail"))

    response.pipeTo(self)
  }

  def handleHttpResponse(response: HttpResponse) = {
    context.system.log.info("http response receved with code {} for server {} current requested operation is {}",
      response.status.intValue, instanceName, requestedOperation.get)
    if (response.status.intValue == 202 || response.status.intValue == 200 ||
    response.status.intValue == 204) {
      requestedOperation = None
      self ! "processNextStep"
    }
  }

  def pingStart = {
    context.system.log.info("ping for server {} is triggered", instanceName)
    ping = Some(context.actorOf(PingActor.props(domain_id, instanceName, floatingIP.get, runNumber, scenarioId, influx),
      "ping"))
    ping.get ! "start"
  }

  def pingStop = {
    context.system.log.info("ping termination for server {} is triggered", instanceName)
    ping.get ! "stop"
    self ! "processNextStep"
  }

  def syncExecution = {
    context.system.log.info("sync with other workers for server {} is triggered", instanceName)
    countdownLatch ! self
  }

  def receive = {
    case x: AuthResponse => {
      access = Some(x.access)
      initComputeEndpoint
      processNextStep
    }
    case "build" => build
    case "processNextStep" => processNextStep
    case "start" => init
    case "create_floating_ip" => create_floating_ip
    case "delete_floating_ip" => delete_floating_ip
    case "associate_floating_ip" => associate_floating_ip
    case "live_migrate" => liveMigrate
    case "wait_for_active" => waitForActive
    case "delete" => delete
    case "start_ping" => pingStart
    case "stop_ping" => pingStop
    case "details" => details
    case "sync_execution" => syncExecution
    case response: CreateServerResponseWrapper => handleCreatedServer(response.server)
    case response: FloatingIPResponse => handleCreatedFloatingIP(response.floating_ip)
    case response: DetailServerResponse => verifyStatus(response)
    case response: DetailServersResponse => handleDetailList(response)
    case result: HttpResponse => handleHttpResponse(result)
    case _ => context.system.log.info("unexpected message received")
  }
}
