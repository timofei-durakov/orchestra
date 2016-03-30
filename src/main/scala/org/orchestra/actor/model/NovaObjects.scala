package org.orchestra.actor.model

/**
  * Created by tdurakov on 19.03.16.
  */

final case class SecurityGroup(name: String)

final case class Link(href: String, rel: String)

final case class Image(id: String, links: List[Link])

final case class Flavor(id: String, links: List[Link])

final case class NetworkAddress(`OS-EXT-IPS-MAC:mac_addr`: Option[String], `OS-EXT-IPS:type`: Option[String],
                                addr: String, version: Int)

final case class ServerDetails(`OS-DCF:diskConfig`: String, `OS-EXT-AZ:availability_zone`: String,
                        `OS-EXT-SRV-ATTR:host`: String, `OS-EXT-SRV-ATTR:hypervisor_hostname`: String,
                        `OS-EXT-SRV-ATTR:instance_name`: String, `OS-EXT-STS:power_state`: Int,
                        `OS-EXT-STS:task_state`: String, `OS-EXT-STS:vm_state`: String,
                        `OS-SRV-USG:launched_at`: String, `OS-SRV-USG:terminated_at`: String,
                        accessIPv4: String, accessIPv6: String, addresses: Map[String, List[NetworkAddress]],
                        config_drive: String, created: String, flavor: Flavor, hostId: String,
                        id: String, image: Image, key_name: String, links: List[Link],
                        metadata: Map[String, String], name: String, `os-extended-volumes:volumes_attached`: List[String],
                        progress: Int, security_groups: List[SecurityGroup], status: String, tenant_id: String,
                        updated: String, user_id: String)

final case class DetailServersResponse(servers:List[ServerDetails])

final case class DetailServerResponse(server: ServerDetails)

final case class Network(uuid: String)

final case class CreateServer(name:String, imageRef: String, availability_zone: Option[String], flavorRef: String, networks: List[Network],
                              key_name: String, min_count: Int = 1, max_count: Int = 1)

final case class CreateServerRequest(server: CreateServer)


final case class CreateServerResponse(`OS-DCF:diskConfig`: Option[String], adminPass: String, id: String,
                                      links: List[Link], security_groups: List[SecurityGroup])

final case class CreateServerResponseWrapper(server: CreateServerResponse)

final case class LiveMigration(host: Option[String] = None, block_migration: Boolean = false, disk_over_commit: Boolean = false)
final case class LiveMigrationRequest(`os-migrateLive`: LiveMigration)
final case class FloatingIP(fixed_ip: Option[String], id: String, instance_id: Option[String], ip: String, pool: String)
final case class FloatingIPResponse(floating_ip: FloatingIP)
final case class FloatingIPAddress(address: String)
final case class AddFloatingIPRequest(addFloatingIp: FloatingIPAddress)

