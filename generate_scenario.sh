#!/bin/bash

if [[ $# -eq 0 ]] ; then
    echo "usage: ${0} name flavor downtime mem workers"
    exit 0
fi

name=$1
flavor=$2
downtime=$3
mem=$4
workers=$5


cat <<EOF
---
  cloud:
    username: "admin"
    projectname: "demo"
    password: "secret"
    auth_url: "http://172.16.166.12:5000/v2.0/tokens"
  run_number: 1
  backend:
    influx_host: "http://monit-ent.vm.mirantis.net:8086"
    database: "openstack"
  scenarios:
     simple:
       load_config:
         vm_workers: $workers
         malloc_mem_mb: $mem
       vm_template:
         flavorRef: "$flavor"
         networkRef: "c2b1c65d-3333-43cf-b678-23224fe59fcb"
         imageRef: "2044ba6d-f636-4d1d-bc23-2162d7c76b9c"
         key_name: "orchestra-vn"
         name_template: "vm_%d"
       pre_config:
         nova_max_downtime: $downtime
         nova_compress: true
         nova_autoconverge: true
         nova_concurrent_migrations: 1
       id: $name
       parallel: 1
       repeat: 1
       playbook_path: "/home/vova/Local/migration-monitor/ansible"
       hosts:
       - 172.16.166.10
       - 172.16.166.11
       on_finish:
       - shutdown_telegraph
       on_sync_events:
       - start_telegraph
       - load_test
       steps:
       - build
       - create_floating_ip
       - wait_for:
           state: "ACTIVE"
       - associate_floating_ip
       - wait_for_floating_ip_associate
       - start_ping:
           frequency: 0.2
           sync: true
       - sync_execution
       - live_migrate
       - wait_for:
           state: "ACTIVE"
       - stop_ping
       - delete_instance
       - wait_for_floating_ip_disassociate
       - delete_floating_ip
EOF
