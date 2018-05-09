#!/bin/bash

usage() {
    echo "Adds a property to an artifact"
    echo "Usage: $0 path repoKey propertyKey propertyValue"
    exit 1
}

if [ -z "$4" ]; then
    usage
fi

set -eu

export path="${1}"
export repoKey="${2}"
export propertyKey="${3}"
export propertyValue="${4}"

export artifactoryServer="https://artifactory.reform.hmcts.net"

curl -sS --fail -u "${ARTIFACTORY_AUTH}" -H "Content-Type: application/json" \
  -XPOST \
  -d '{"multiValue":false,"property":{"name":"'${propertyKey}'"},"selectedValues":["'${propertyValue}'"]}' \
  "${artifactoryServer}/artifactory/api/artifactproperties?path=${path}&repoKey=${repoKey}&recursive=false"
echo
