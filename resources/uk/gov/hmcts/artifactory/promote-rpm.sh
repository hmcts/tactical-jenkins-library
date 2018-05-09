#!/bin/bash

usage() {
    echo "Copies rpm from srcPath to targetPath"
    echo "Usage: $0 productName appName"
    exit 1
}

getTopLevelProperty() {
  local property=${1}
  local searchResults=${2}

  echo ${searchResults} | jq '.results[0].'${property} | sed -e 's/^"//' -e 's/"$//'
}

getMetadataProperty() {
  local property=${1}
  local searchResults=${2}

  echo ${searchResults} | jq '.results[0].properties[]| select(.key=="'${property}'").value' | sed -e 's/^"//' -e 's/"$//'
}

logInfo() {
  echo -e "INFO: $@" 1>&2
}

logError() {
 echo -e "ERROR: $@" 1>&2
}

if [ -z "$2" ]; then
    usage
fi

set -eu

export productName="$1"
export appName="$2"
export targetRepo="${productName}-production"
export srcRepo="${productName}-local"

export artifactoryServer="https://artifactory.reform.hmcts.net"

logInfo "Determining full RPM name"
envsubst '${srcRepo} ${appName} ${productName}' < name-query.aql.template > name-query.aql

rpmSearchResults=$(curl -u ${ARTIFACTORY_AUTH} -s -XPOST \
  -H "content-type: text/plain" \
  -d @name-query.aql \
  ${artifactoryServer}/artifactory/api/search/aql)

numberOfResults=$(echo ${rpmSearchResults} | jq '.results | length')

if [ "${numberOfResults}" -eq 0 ]; then
  logError "Did not find any RPMs eligible for promotion"
  logError "Search criteria was: \n\tproductName - ${productName} \n\tappName - ${appName} \n\tsrcRepo - ${srcRepo}"
  exit 1
fi

export fullRpmName=$(getTopLevelProperty 'name' "${rpmSearchResults}")
rpmVersion=$(echo ${fullRpmName} | sed -e "s/^${productName}-${appName}-//" -e "s/.x86_64.rpm//")

logInfo "RPM Name: ${fullRpmName}"

envsubst '${srcRepo} ${fullRpmName}' < full-rpm-query.aql.template > full-rpm-query.aql

unset http_proxy

# Artifactory seems to have a bug where if you include a limit in your
# query then it limits properties to one result as well, so we need to do two queries :(
fullRpmSearchResults=$(curl -u ${ARTIFACTORY_AUTH} -s -XPOST \
  -H "content-type: text/plain" \
  -d @full-rpm-query.aql \
  ${artifactoryServer}/artifactory/api/search/aql)

gitTag=$(getMetadataProperty 'git-tag' "${fullRpmSearchResults}")
ansibleTag=$(getMetadataProperty 'ansible-tag' "${fullRpmSearchResults}")

logInfo "RPM built from git-tag: ${gitTag}"
logInfo "Ansible tag is: ${ansibleTag}"

logInfo "Promoting RPM: ${fullRpmName} from ${srcRepo} to ${targetRepo}"

curl -u "${ARTIFACTORY_AUTH}" -X POST \
 -H "Accept: application/vnd.org.jfrog.artifactory.storage.CopyOrMoveResult+json" \
 "${artifactoryServer}/artifactory/api/copy/${srcRepo}/${appName}/${fullRpmName}?to=/${targetRepo}/${appName}/${fullRpmName}&suppressLayouts=1" 1>&2

# Return any values to caller
cat <<EOF
{
   "git_tag": "${gitTag}",
   "ansible_tag": "${ansibleTag}",
   "full_rpm_name": "${fullRpmName}",
   "rpm_version": "${rpmVersion}"
}
EOF
