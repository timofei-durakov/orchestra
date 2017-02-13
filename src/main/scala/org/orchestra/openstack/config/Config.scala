package org.orchestra.openstack.config

import org.orchestra.common.config.TestTypeConfig

/**
  * Created by tdurakov on 20.03.16.
  */

final case class LoadConfig(
  vm_workers: Int,
  malloc_mem_mb: Int)

final case class Cloud(username: String,
                       projectname: String,
                       password: String,
                       auth_url: String)

final case class VmTemplate(
  az: Option[String],
  flavorRef: String,
  networkRef: String,
  imageRef: String,
  key_name: Option[String],
  floating_ip_pool: Option[String],
  name_template: String)

final case class EnvConfig(nova_compress: Boolean = false,
                           nova_autoconverge: Boolean = false,
                           nova_concurrent_migrations: Int = 1,
                           nova_max_downtime: Int = 100)

final case class Scenario(
  vm_template: VmTemplate,
  pre_config: Option[EnvConfig],
  id: Int,
  parallel: Int,
  repeat: Int,
  playbook_path: String,
  hosts: List[String],
  on_finish: List[String],
  on_sync_events: List[String],
  steps: List[Step],
  load_config: Option[LoadConfig])

final case class Backend(influx_host: String,
                         database: String,
                         callback_host: String,
                         callback_port: Int)

final case class AppConfig(
  log_level: String)

final case class Config(
  cloud: Cloud,
  run_number: Int,
  backend: Backend,
  app_config: AppConfig,
  scenarios: Map[String, Scenario])


