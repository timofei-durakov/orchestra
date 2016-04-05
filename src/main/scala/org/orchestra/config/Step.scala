package org.orchestra.config

/**
  * Created by tdurakov on 05.04.16.
  */
trait Step

final case class Build(availability_zone: Option[String]) extends Step

final case class CreateFloatingIp() extends Step

final case class WaitFor(state: String) extends Step

final case class AssociateFloatingIp() extends Step

final case class WaitForFloatingIpAssociate() extends Step

final case class StartPing(frequency: Double, sync: Boolean) extends Step

final case class SyncExecution() extends Step

final case class LiveMigrate() extends Step

final case class StopPing() extends Step

final case class DeleteInstance() extends Step

final case class WaitForFloatingIpDisassociate() extends Step

final case class DeleteFloatingIp() extends Step

final case class CreateServerStep(availability_zone: Option[String])