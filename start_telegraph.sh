set -x
lm_run=$1
lm_scenario=$2
playbook_path=$3
inventory=$4

export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/Cellar/ansible/1.9.4/bin/ansible-playbook $playbook_path/start.yml -i $inventory --extra-vars "{\"lm_run\": \"$lm_run\",\"lm_scenario\":\"$lm_scenario\"}"
