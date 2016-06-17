package org.orchestra.config

import net.jcazevedo.moultingyaml._

/**
  * Created by tdurakov on 20.03.16.
  */

trait ConfigYamlProtocol extends DefaultYamlProtocol {
  implicit val cloudFormat = yamlFormat4(Cloud)
  implicit val vmTemplateFormat = yamlFormat5(VmTemplate)
  implicit val envConfigFormat = yamlFormat3(EnvConfig)
  implicit val backendFormat = yamlFormat2(Backend)
  implicit object scenarioFormat extends YamlFormat[Scenario] {
    override def read(yaml: YamlValue): Scenario = {
      val map = yaml.asYamlObject.fields
      val vmTemplate = map(YamlString("vm_template")).convertTo[VmTemplate]
      val preConfig = map(YamlString("pre_config")).convertTo[EnvConfig]
      val id = map(YamlString("id")).convertTo[Int]
      val parallel = map(YamlString("parallel")).convertTo[Int]
      val repeat = map(YamlString("repeat")).convertTo[Int]
      val playbook_path = map(YamlString("playbook_path")).convertTo[String]
      val hosts = map(YamlString("hosts")).convertTo[List[String]]
      val on_finish = map(YamlString("on_finish")).convertTo[List[String]]
      val on_sync_events = map(YamlString("on_sync_events")).convertTo[List[String]]
      val steps = map(YamlString("steps")).asInstanceOf[YamlArray]
      val stepList = steps.elements.map {
        case y: YamlString => parseYamlStringAsStep(y)
        case y: YamlObject => parseYamlObjectAsStep(y)
        case _ => throw new IllegalArgumentException("Unexpected type for step received")

      }
      Scenario(vmTemplate, preConfig, id, parallel, repeat, playbook_path, hosts, on_finish, on_sync_events,
        stepList.toList)
    }

    def parseYamlObjectAsStep(yamlObject: YamlObject): Step = {
      val pair = yamlObject.fields.head
      val step_name = pair._1.asInstanceOf[YamlString].value
      val params = pair._2.asYamlObject
      step_name match {
        case "build" => {
          if (params.fields.isEmpty) {
            Build(None)
          } else {
            val az = params.fields(YamlString("availability_zone")).asInstanceOf[YamlString].value
            Build(Some(az))
          }
        }
        case "wait_for" => {
          val target_state = params.fields(YamlString("state")).asInstanceOf[YamlString].value
          WaitFor(target_state)
        }
        case "start_ping" => {
          val frequency = params.fields(YamlString("frequency")).asInstanceOf[YamlNumber[Double]].value
          val sync = params.fields(YamlString("sync")).asInstanceOf[YamlBoolean].boolean
          StartPing(frequency, sync)
        }
      }
    }

    def parseYamlStringAsStep(yamlString: YamlString): Step = {
      yamlString.value match {
        case "build" => Build(None).asInstanceOf[Step]
        case "create_floating_ip" => CreateFloatingIp().asInstanceOf[Step]
        case "associate_floating_ip" => AssociateFloatingIp().asInstanceOf[Step]
        case "wait_for_floating_ip_associate" => WaitForFloatingIpAssociate().asInstanceOf[Step]
        case "sync_execution" => SyncExecution().asInstanceOf[Step]
        case "live_migrate" => LiveMigrate().asInstanceOf[Step]
        case "stop_ping" => StopPing().asInstanceOf[Step]
        case "delete_instance" => DeleteInstance().asInstanceOf[Step]
        case "wait_for_floating_ip_disassociate" => WaitForFloatingIpDisassociate().asInstanceOf[Step]
        case "delete_floating_ip" => DeleteFloatingIp().asInstanceOf[Step]
      }
    }

    override def write(obj: Scenario): YamlValue = {
      YamlObject(YamlString("vm_template") -> obj.vm_template.toYaml,
        YamlString("pre_config") -> obj.pre_config.toYaml,
        YamlString("id") -> YamlNumber(obj.id),
        YamlString("parallel") -> YamlNumber(obj.parallel),
        YamlString("repeat") -> YamlNumber(obj.repeat),
        YamlString("playbook_path") -> YamlString(obj.playbook_path),
        YamlString("hosts") -> obj.hosts.toYaml,
        YamlString("on_finish") -> obj.on_finish.toYaml,
        YamlString("on_sync_events") -> obj.on_sync_events.toYaml
        //TODO(tdurakov): dump steps too
        )
    }
  }
  implicit val configFormat = yamlFormat4(Config)
}

final case class Cloud(username: String, projectname: String, password: String, auth_url: String)

final case class VmTemplate(flavorRef: String, networkRef: String, imageRef: String, key_name: String, name_template: String)

final case class EnvConfig(nova_compress: Boolean = false, nova_autoconverge: Boolean = false, nova_concurrent_migrations: Int = 1)

final case class Scenario(vm_template: VmTemplate, pre_config: EnvConfig, id: Int, parallel: Int, repeat: Int, playbook_path: String, hosts: List[String], on_finish: List[String], on_sync_events: List[String], steps: List[Step])

final case class Backend(influx_host: String, database: String)

final case class Config(cloud: Cloud, run_number: Int, backend: Backend, scenarios: Map[String, Scenario])

