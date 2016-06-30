package org.orchestra.openstack

import org.scalatest._

import net.jcazevedo.moultingyaml._

import org.orchestra.common.config.TestTypeConfig
import org.orchestra.libvirt.config._


class LibvirtConfigSpec extends FunSpec with Matchers {
  describe("Libvirt Yaml Config"){
    describe("when all options are defined"){
      val data =
        """
          |type: libvirt
          |env:
          |  influx:
          |    host: "monit-ent.vm.mirantis.net"
          |    port: 8086
          |    database: "libvirt_dev"
          |  callback:
          |    host: "172.16.129.110"
          |    port: 9999
          |  libvirt:
          |    source: 192.168.42.11
          |    destination: 192.168.42.12
          |  playbook_path: "./ansible"
          |test:
          |  before_all:
          |    - install_prerequisites
          |  test_run:
          |    - generate_config_drive
          |    - boot_vm
          |    - start_telegraf
          |    - wait_for_call_home
          |    - migrate
          |    - stop_telegraf
          |    - drop_domain
          |    - clean_storage
          |  after_all:
          |    - nothing
          |scenarios:
          |  simple:
          |    id: 1
          |    repeat: 10
          |    load:
          |      vm_workers: 2
          |      malloc_mem_mb: 256
          |    migrate:
          |      qemu_monitor_commands:
          |        - "migrate_set_parameter x-cpu-throttle-initial 30"
          |        - migrate_set_parameter x-cpu-throttle-increment 15
          |      virsh_migrate_commandline_args: "--auto-converge"
          |""".stripMargin

      it("should parse type options as base config"){
        import org.orchestra.common.config.TestTypeConfigYamlProtocol.configFormat

        val config = data.parseYaml.convertTo[TestTypeConfig]
        config.`type` should be ("libvirt")
      }

      it("should parse options as specific config (for libvirt)"){
        import org.orchestra.libvirt.config.ConfigYamlProtocol.configFormat
        val config = data.parseYaml.convertTo[Config]

        config.env.influx.host should be ("monit-ent.vm.mirantis.net")
        config.env.influx.port should be (8086)
        config.env.influx.database should be ("libvirt_dev")

        config.env.callback.host should be ("172.16.129.110")
        config.env.callback.port should be (9999)

        config.env.libvirt.source should be ("192.168.42.11")
        config.env.libvirt.destination should be ("192.168.42.12")

        config.env.playbook_path should be ("./ansible")

        config.test.before_all should contain (Step("install_prerequisites"))

        config.test.test_run should contain (Step("generate_config_drive"))
        config.test.test_run should contain (Step("boot_vm"))
        config.test.test_run should contain (Step("start_telegraf"))
        config.test.test_run should contain (Step("wait_for_call_home"))
        config.test.test_run should contain (Step("migrate"))
        config.test.test_run should contain (Step("stop_telegraf"))
        config.test.test_run should contain (Step("drop_domain"))
        config.test.test_run should contain (Step("clean_storage"))

        config.test.after_all should contain (Step("nothing"))
      }
    }

    describe("when some options are not defined"){
      val data =
        """
          |""".stripMargin

      ignore("should use default values"){
        import org.orchestra.libvirt.config.ConfigYamlProtocol.configFormat
        val config = data.parseYaml.convertTo[Config]

        config.env.playbook_path should be ("./ansible")
      }
    }

    describe("when empty config is provided"){
      import org.orchestra.libvirt.config.ConfigYamlProtocol.configFormat

      it("it should throw DeserializationException"){
        a [DeserializationException] should be thrownBy {
          "".parseYaml.convertTo[Config]
        }
      }
    }
  }
}
