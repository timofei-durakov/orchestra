set -x
playbook_path=$1
inventory=$2
lm_run=$3
lm_scenario=$4

export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/bin/ansible-playbook $playbook_path/start.yml -i $inventory --extra-vars "{\"lm_run\": \"$lm_run\",\"lm_scenario\":\"$lm_scenario\"}"
