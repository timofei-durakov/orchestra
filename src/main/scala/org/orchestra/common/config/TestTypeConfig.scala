package org.orchestra.common.config

import net.jcazevedo.moultingyaml._

/**
  * Created by vova on 6/26/16.
  */

case class TestTypeConfig(`type`: String = "openstack")

object TestTypeConfigYamlProtocol extends DefaultYamlProtocol {
  implicit val configFormat = yamlFormat1(TestTypeConfig)
}
