playbook_path=$1
inventory=$2

export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/bin/ansible-playbook $playbook_path/stop.yml -i $inventory