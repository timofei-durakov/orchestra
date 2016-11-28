package org.orchestra.vmware.config

import net.jcazevedo.moultingyaml._
import org.orchestra.openstack.config.AppConfig
import org.orchestra.openstack.config.ConfigYamlProtocol.yamlFormat1
import org.orchestra.vmware.steps._


object ConfigYamlProtocol extends DefaultYamlProtocol {

  implicit val appConfigFormat = yamlFormat1(AppConfig)

  implicit val cloudFormat = yamlFormat3(Cloud)

  implicit val monitoringNodeFormat = yamlFormat3(MonitoringNode)

  implicit object scenarioFormat extends YamlFormat[Scenario] {
    override def read(yaml: YamlValue): Scenario = {
      val map = yaml.asYamlObject.fields
      val steps = map(YamlString("steps")).asInstanceOf[YamlArray]

      val stepList = steps.elements.map {
        case y: YamlString => parseYamlStringAsStep(y)
        case _ => throw new IllegalArgumentException("Unexpected type for step received")
      }
      Scenario(stepList.toList)
    }

    def parseYamlStringAsStep(yamlString: YamlString): BaseStep = {
      yamlString.value match {
//        case "upload_vm_templates" => UploadVmTemplatesStep().asInstanceOf[BaseStep]
//        case "prepare_network" => PrepareNetworkStep().asInstanceOf[BaseStep]
//        case "clone_vms" => CloneVmsStep().asInstanceOf[BaseStep]
//        case "boot_vms" => BootVmsStep().asInstanceOf[BaseStep]
//        case "wait_for_instance_become_available" => WaitForInstanceBecomeAvailableStep().asInstanceOf[BaseStep]
        case "start_migration_monitor" => StartMigrationMonitorStep().asInstanceOf[BaseStep]
//        case "start_downtimer" => StartDowntimerStep().asInstanceOf[BaseStep]
        case "vmotion_migrate" => VmotionMigrateStep().asInstanceOf[BaseStep]
        case "stop_migration_monitor" => StopMigrationMonitorStep().asInstanceOf[BaseStep]
//        case "stop_downtimer" => StartDowntimerStep().asInstanceOf[BaseStep]
        case "destroy_vms" => DestroyVmsStep().asInstanceOf[BaseStep]
      }
    }

    override def write(obj: Scenario): YamlValue = {
      //TODO dump parameters
      YamlObject()
    }
  }

  implicit val envFormat = yamlFormat3(Env)

  implicit val configFormat = yamlFormat3(Config)
}
