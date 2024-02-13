#!/usr/bin/env bash

set -eu

exec java ${JAVA_OPTS} -jar *-allinone.jar server *.yaml
