package org.orchestra.config

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

/**
  * Created by tdurakov on 20.03.16.
  */

trait ConfigYamlProtocol extends DefaultYamlProtocol {
  implicit val cloudFormat = yamlFormat4(Cloud)
  implicit val vmTemplateFormat = yamlFormat4(VmTemplate)
  implicit val scenarioFormat = yamlFormat2(Scenario)
  implicit val configFormat = yamlFormat3(Config)
}


final case class Cloud(username: String, projectname: String, password: String, auth_url: String)
final case class VmTemplate(flavorRef: String, networkRef: String, imageRef: String, name_template: String)
final case class Scenario(parallel: Int, steps:List[String])
final case class Config (cloud: Cloud, vm_template: VmTemplate, scenarios:Map[String, Scenario])
