package org.orchestra.vmware.config

import org.orchestra.vmware.steps.BaseStep

final case class AppConfig(
  log_level: String)

final case class Cloud(hostname: String,
                       username: String,
                       password: String)

final case class MonitoringNode(hostname: String,
                                username: String,
                                password: String)

final case class Scenario(
  steps: List[BaseStep])

final case class Env(
  playbook_path: String,
  cloud: Cloud,
  monitoring_node: MonitoringNode)

final case class Config(
  app_config: AppConfig,
  env: Env,
  scenarios: Map[String, Scenario])
