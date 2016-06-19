# Ansible playbooks for starting/stopping telegraf and live migration monitoring

## Start

Run:
```sh
	$ ansible-playbook start.yml -i hosts --extra-vars '{"lm_run":"1","lm_scenario":"foo"}'
```

## Stop

Run:
```sh
	$ ansible-playbook stop.yml -i hosts
```

## Update nova flags and restart n-cpu

Default parameters:

```yml
    nova_compress: false
    nova_autoconverge: false
    nova_concurrent_migrations: 0
```

Run:

```sh
	$ ansible-playbook nova_flags.yml -i hosts --extra-vars '{"nova_compress":true, "nova_autoconverge":false, "nova_concurrent_migrations": 1}'

	$ ansible-playbook nova_flags.yml -i hosts --extra-vars '{"nova_concurrent_migrations": 10}'

	$ ansible-playbook nova_flags.yml -i hosts --extra-vars '{"nova_compress":true}'
```
