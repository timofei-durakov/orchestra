package org.orchestra.vmware.steps

import java.net.URL

import akka.actor.ActorRef
import com.vmware.vim25._
import com.vmware.vim25.mo._
import org.orchestra.vmware.config._

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging._

import scala.util.control.Breaks._

final case class VmotionMigrateStep() extends BaseStep

object VmotionMigrate extends Step {
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  override def run(monitor: ActorRef, ansible: ActorRef, env: Env): Unit = {
    log.info("Start live migrating VMs")
    var serviceInstance = new ServiceInstance(new URL(env.cloud.hostname), env.cloud.username, env.cloud.password, true)
    var rootFolder = serviceInstance.getRootFolder
    var hosts = new InventoryNavigator(rootFolder)
      .searchManagedEntities("HostSystem")
      .map(x => x.asInstanceOf[HostSystem])

    for (host <- hosts) {
      breakable {
        if (host.getName == env.monitoring_node.hostname) {
          log.info("Skip migration for monitoring host %s".format(host.getName))
          break
        }
      }
      log.info("Start VMotioning VMs from host %s".format(host.getName))

      var vsanHostDecommissionMode = new VsanHostDecommissionMode
      vsanHostDecommissionMode.setObjectAction("noAction") // "evacuateAllData" or "ensureObjectAccessibility"

      var hostMaintenanceSpec = new HostMaintenanceSpec
      hostMaintenanceSpec.setVsanMode(vsanHostDecommissionMode)
      var enterMaintenanceTask = host.enterMaintenanceMode(0, true, hostMaintenanceSpec)
      log.info("Start entering in maintenance mode host %s".format(host.getName))

      var vms = host.getVms
      for (vm <- vms) {
        vmotionVm(vm)
      }
      log.info("VMs migration for host %s has been completed".format(host.getName))

      if (enterMaintenanceTask.waitForTask() == Task.SUCCESS) {
        log.info("Host %s successfully entered in maintenance mode".format(host.getName))
      }
      else {
        log.error("Enter in maintenance mode for host %s failed. Task info: %s".format(
          host.getName, enterMaintenanceTask.getTaskInfo))
      }

      exitMaintenanceMode(host)
    }

    serviceInstance.getServerConnection.logout()
    log.info("Live migration completed")

    monitor ! "processNextStep"
  }

  private def vmotionVm(vm: VirtualMachine): Unit = {
    log.info("Start live migration for VM %s".format(vm.getName))
    var vmotionTask = vm.migrateVM_Task(
      vm.getResourcePool,
      null,
      VirtualMachineMovePriority.highPriority,
      VirtualMachinePowerState.poweredOn)

    if(vmotionTask.waitForTask() == Task.SUCCESS) {
      log.info("VM %s successfully VMotioned from host".format(vm.getName))
    }
    else {
      log.error("VMotion failed for VM %s. Task info: %s".format(vm.getName, vmotionTask.getTaskInfo))
    }
  }

  private def exitMaintenanceMode(host: HostSystem): Unit = {
    log.info("Starting exit from maintenance mode for host %s".format(host.getName))
    var exitMaintenanceTask = host.exitMaintenanceMode(0)
    if(exitMaintenanceTask.waitForTask() == Task.SUCCESS) {
      log.info("Host %s exited from maintenance mode".format(host.getName))
    }
    else {
      log.error("Exit from maintenance mode for host %s failed. Task info: %s".format(
        host.getName, exitMaintenanceTask.getTaskInfo))
    }
  }
}
