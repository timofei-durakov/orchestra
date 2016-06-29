package org.orchestra.libvirt.config

import org.orchestra.common.InfluxDB


final case class Influx(
                          host: String,
                          port: Int,
                          database: String)
final case class Callback(
                           host: String,
                           port: Int)

final case class Libvirt(
                          source:String,
                          destination:String)
final case class LoadConfig(
                             vm_workers: Int,
                             malloc_mem_mb: Int)

final case class Env(
                      influx: Influx,
                      callback: Callback,
                      libvirt: Libvirt,
                      playbook_path: String)

final case class Step(name: String)

final case class Test(
                       before_all: List[Step],
                       test_run: List[Step],
                       after_all: List[Step])




final case class MigrationConfig(
                                  qemu_monitor_commands: List[String],
                                  virsh_migrate_commandline_args: String)


final case class Scenario(
                           id: Int,
                           repeat: Int,
                           load: LoadConfig,
                           migrate: MigrationConfig)

final case class Config(
                         env: Env,
                         test: Test,
                         scenarios: Map[String, Scenario])
