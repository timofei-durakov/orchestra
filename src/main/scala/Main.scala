
import java.io.File
import spray.can.Http

import net.jcazevedo.moultingyaml._

import org.orchestra.common.config.TestTypeConfig
import org.orchestra.common.config.TestTypeConfigYamlProtocol._

import org.orchestra.libvirt.LibvirtMigrationTest
import org.orchestra.openstack.OpenstackMigrationTest

import scala.io.Source

object Main {

  def dispatchTestType(data: String): Unit ={

    val config = data.parseYaml.convertTo[TestTypeConfig]

    if (config.`type` == "openstack") {
      OpenstackMigrationTest.start(data)
    } else if( config.`type` == "libvirt"){
      LibvirtMigrationTest.start(data)
    } else throw new Exception("Unknown test type: {}".format(config.`type`))
  }

  def main(args: Array[String]): Unit = {
    //TODO: change to some existing lib
    if (args.isEmpty) {
      println("application.conf is required")
      return
    }
    val confFile = args(0)

    //TODO: check if it's possible to use akka extensions instead
    val data = Source.fromFile(new File(confFile)).mkString

    dispatchTestType(data)

    println("Orchestra system is terminated")
  }
}
