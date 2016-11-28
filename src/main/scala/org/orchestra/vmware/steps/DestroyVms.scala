package org.orchestra.vmware.steps

import java.net.URL

import akka.actor.ActorRef
import com.typesafe.scalalogging.Logger
import com.vmware.vim25.mo._
import org.orchestra.vmware.config._
import org.slf4j.LoggerFactory

import scala.util.control.Breaks.{break, breakable}


final case class DestroyVmsStep() extends BaseStep

object DestroyVms extends Step {
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  override def run(monitor: ActorRef, ansible: ActorRef, env: Env): Unit = {
    log.info("Start destroying VMs")
    var serviceInstance = new ServiceInstance(new URL(env.cloud.hostname), env.cloud.username, env.cloud.password, true)
    var rootFolder = serviceInstance.getRootFolder
    var hosts = new InventoryNavigator(rootFolder)
      .searchManagedEntities("HostSystem")
      .map(x => x.asInstanceOf[HostSystem])

    for (host <- hosts) {
      breakable {
        if (host.getName == env.monitoring_node.hostname) {
          log.info("Skip destroying VMs for monitoring host %s".format(host.getName))
          break
        }
      }
      log.info("Start destroying VMs on host %s".format(host.getName))
      var vms = host.getVms
      for (vm <- vms) {
        var destroyTask = vm.destroy_Task()
        if(destroyTask.waitForTask() == Task.SUCCESS) {
          log.info("VM %s successfully destroyed".format(vm.getName))
        }
        else {
          var taskInfo = destroyTask.getTaskInfo
          log.error("Error occurred during destroying VM %s. Task info: %s".format(
            vm.getName, destroyTask.getTaskInfo))
        }
      }
    }
    serviceInstance.getServerConnection.logout()

    monitor ! "processNextStep"
  }
}
