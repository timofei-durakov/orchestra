package org.orchestra.libvirt.config

import net.jcazevedo.moultingyaml.{DefaultYamlProtocol, YamlArray, YamlFormat, YamlObject, YamlString, YamlValue}

/**
  * Created by vova on 6/26/16.
  */
object ConfigYamlProtocol  extends DefaultYamlProtocol {

  implicit val influxFormat = yamlFormat3(Influx)
  implicit val callbackFormat = yamlFormat2(Callback)
  implicit val libvirtFormat = yamlFormat2(Libvirt)

  implicit val envFormat = yamlFormat4(Env)


  implicit val testFormat = new YamlFormat[Test] {
    def read(value: YamlValue) = {
      val ys = YamlString
      val toStep = (x: YamlValue) => Step(x.convertTo[String])
      value.asYamlObject.getFields(
        ys("before_all"),
        ys("test_run"),
        ys("after_all")) match {
          case Seq(YamlArray(ba), YamlArray(tr), YamlArray(aa)) =>
            Test(
              ba.map(toStep).toList,
              tr.map(toStep).toList,
              aa.map(toStep).toList)
          case _ => throw new Exception("Test parse failed.")
        }
    }
    def write(obj: Test) = YamlObject()
  }

  implicit val loadConfigFormat = yamlFormat2(LoadConfig)
  implicit val migrationFormat = yamlFormat3(MigrationConfig)
  implicit val scenarioFormat = yamlFormat4(Scenario)

  implicit val configFormat = yamlFormat3(Config)
}
