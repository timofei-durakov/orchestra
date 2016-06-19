playbook_path=$1
inventory=$2

vm_workers=$3
malloc_mem_mb=$4


export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/bin/ansible-playbook $playbook_path/load.yml -i $inventory --extra-vars "{\"vm_workers\": \"$vm_workers\",\"malloc_mem_mb\":\"$malloc_mem_mb\"}"
