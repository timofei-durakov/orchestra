package org.orchestra.libvirt

import akka.actor.{Actor, Props}
import org.orchestra.common.Reaper
import org.orchestra.libvirt.config.{Callback, Env, Scenario, Test}


object ScenarioExecutor {
  def props(scenario: Scenario, test: Test, env: Env): Props =
    Props(new ScenarioExecutor(scenario, test, env))
}

class ScenarioExecutor(scenario: Scenario, test: Test, env: Env) extends Actor {

  val reaper = context.actorOf(Reaper.props, name = "reaper")
  val testSteps = context.actorOf(LibvirtTestSteps.props(reaper, env), name = "steps")


  var testRun = 0
  var started = false
  var currentSection = "before_all"
  var currentStep = 0

  def start: Unit = {
    context.system.log.info("== before all")
    testSteps ! "start"
  }

  def process: Unit = {

    if(currentSection == "before_all") {

      if(currentStep < test.before_all.length){
        testSteps ! (test.before_all(currentStep), testRun, scenario)
        currentStep += 1
      } else {
        context.system.log.info("= test run")
        currentSection = "test_run"
        currentStep = 0;
        process
      }

    } else if(currentSection == "test_run") {

      if(currentStep < test.test_run.length){
        testSteps ! (test.test_run(currentStep), testRun, scenario)
        currentStep += 1
      } else {
        currentStep = 0;
        testRun += 1;

        if(testRun == scenario.repeat){
          currentSection = "after_all"
          context.system.log.info("== after all")

        } else {
          context.system.log.info("= test run")
        }
        process
      }

    } else if(currentSection == "after_all") {
      if(currentStep < test.after_all.length){
        testSteps ! (test.after_all(currentStep), testRun, scenario)
        currentStep += 1
      } else {
        testSteps ! "end"
      }
    }
  }

  def receive = {
    case "start" => start
    case "done" => process
    case a:Any => context.system.log.warning("unexpected message received => {}", a)
  }
}

