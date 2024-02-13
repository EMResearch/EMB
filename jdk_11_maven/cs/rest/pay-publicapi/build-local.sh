#!/bin/bash

set -e

cd "$(dirname "$0")"

mvn -DskipITs clean verify
if [ "$(uname -m)" == "arm64" ]; then
  docker build -t governmentdigitalservice/pay-publicapi:local -f m1/arm64.Dockerfile .
else
  docker build -t governmentdigitalservice/pay-publicapi:local .
fi
