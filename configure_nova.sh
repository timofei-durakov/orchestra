set -x
playbook_path=$1
inventory=$2
nova_compress=$3
nova_autoconverge=$4
nova_concurrent_migrations=$5

export ANSIBLE_HOST_KEY_CHECKING=False

/usr/local/Cellar/ansible/1.9.4/bin/ansible-playbook $playbook_path/nova_flags.yml -i $inventory --extra-vars "{\"nova_compress\":$nova_compress, \"nova_autoconverge\":$nova_autoconverge, \"nova_concurrent_migrations\": $nova_concurrent_migrations}"

