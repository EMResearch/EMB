#!/usr/bin/env sh

if test /var/run/secrets/nais.io/vault/vault_token;
then
    export VAULT_TOKEN=$(cat /var/run/secrets/nais.io/vault/vault_token)
fi