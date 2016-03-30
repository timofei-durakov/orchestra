package org.orchestra.config

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

/**
  * Created by tdurakov on 20.03.16.
  */

trait ConfigYamlProtocol extends DefaultYamlProtocol {
  implicit val cloudFormat = yamlFormat4(Cloud)
  implicit val vmTemplateFormat = yamlFormat6(VmTemplate)
  implicit val scenarioFormat = yamlFormat9(Scenario)
  implicit val backendFormat = yamlFormat2(Backend)
  implicit val configFormat = yamlFormat4(Config)
}


final case class Cloud(username: String, projectname: String, password: String, auth_url: String)

final case class VmTemplate(az: Option[String], flavorRef: String, networkRef: String, imageRef: String, key_name: String, name_template: String)

final case class Scenario(vm_template: VmTemplate, id: Int, parallel: Int, repeat: Int, playbook_path: String, hosts: List[String], on_finish: List[String], on_sync_events: List[String], steps: List[String])

final case class Backend(influx_host: String, database: String)

final case class Config(cloud: Cloud,  run_number: Int, backend: Backend, scenarios: Map[String, Scenario])
