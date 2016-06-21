# Orchestra

[![Build Status](https://travis-ci.org/timofei-durakov/orchestra.svg?branch=master)](https://travis-ci.org/timofei-durakov/orchestra)

## TODOs

- remove environment-specific parameters from scenario config: `playbook_path`, `key_name`

- use either underscore_case or camelCase for config params

## Scenario generator

Generate several scenarios via bash (100.yaml .. 105.yaml, 2 mem workers, 1500 nova max downtime, 300+10*i Mb):

	$ for i in {0..5}; do ./generate_scenario.sh $(expr $i + 100) 2 1500 $(expr 300 + $i \* 10) 2 > $(expr $i + 100).yaml; done

Run them:

	$ for i in {100..105}; do sbt "run ${i}.yaml"; done

--
