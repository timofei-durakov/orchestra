import org.scalatest._
import net.jcazevedo.moultingyaml._

import org.orchestra.config.Config
import org.orchestra.config.ConfigYamlProtocol._

class ConfigSpec extends FlatSpec with Matchers {

  "A yaml config" should "parse default config with all options defined" in {
    val data =
      """
      |cloud:
      |  username: "admin"
      |  projectname: "demo"
      |  password: "secret"
      |  auth_url: "http://172.16.166.12:5000/v2.0/tokens"
      |run_number: 1
      |backend:
      |  influx_host: "http://monit-ent.vm.mirantis.net:8086"
      |  database: "openstack"
      |  callback_host: "172.16.18.8"
      |  callback_port: 9999
      |app_config:
      |  log_level: "INFO"
      |scenarios:
      |   simple:
      |     vm_template:
      |       flavorRef: "2"
      |       networkRef: "c2b1c65d-3333-43cf-b678-23224fe59fcb"
      |       imageRef: "2044ba6d-f636-4d1d-bc23-2162d7c76b9c"
      |       key_name: "orchestra-vn"
      |       name_template: "vm_%d"
      |     pre_config:
      |       nova_max_downtime: 500
      |       nova_compress: true
      |       nova_autoconverge: true
      |       nova_concurrent_migrations: 1
      |     load_config:
      |       vm_workers: 2
      |       malloc_mem_mb: 512
      |     id: 1
      |     parallel: 1
      |     repeat: 1
      |     playbook_path: "./ansible"
      |     hosts:
      |     - 172.16.166.10
      |     - 172.16.166.11
      |     on_finish:
      |     - shutdown_telegraph
      |     on_sync_events:
      |     - start_telegraph
      |     - load_test
      |     steps:
      |     - build
      |     - create_floating_ip
      |     - wait_for:
      |         state: "ACTIVE"
      |     - associate_floating_ip
      |     - wait_for_floating_ip_associate
      |     - start_ping:
      |         frequency: 0.2
      |         sync: true
      |     - sync_execution
      |     - live_migrate
      |     - wait_for:
      |         state: "ACTIVE"
      |     - stop_ping
      |     - delete_instance
      |     - wait_for_floating_ip_disassociate
      |     - delete_floating_ip
      |""".stripMargin

    val config = data.parseYaml.convertTo[Config]

    config.cloud.username should be ("admin")
  }

  it should "throw DeserializationException if an empty config is provided" in {
    a [DeserializationException] should be thrownBy {
      "".parseYaml.convertTo[Config]
    }
  }
}
