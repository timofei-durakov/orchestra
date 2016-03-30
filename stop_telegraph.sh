playbook_path=$1
inventory=$2

export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/Cellar/ansible/1.9.4/bin/ansible-playbook $playbook_path/stop.yml -i $inventory