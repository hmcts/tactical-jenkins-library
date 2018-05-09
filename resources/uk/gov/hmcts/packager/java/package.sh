#!/usr/bin/env bash
set -e

dot="$(cd "$(dirname "$([ -L "$0" ] && $READLINK_CMD -f "$0" || echo "$0")")"; pwd)"
cd "${dot}/../.."
usage() {
  echo "Usage: $(basename ${0}) project_name app_name path_to_jar app_type path_to_config_file"
}
require_file () {
  ls $1 >/dev/null 2>&1 || {
    echo >&2 "$1 not found at $(pwd). Aborting."
    exit 1
  }
}
require () {
  hash $1 2>/dev/null || {
    echo >&2 "$1 is not installed.  Aborting."
    exit 1
  }
}
require_arg () {
  test -n "$1" || {
    echo >&2 "$2 not provided. Aborting."
    usage
    exit 1
  }
  eval "$2=$1"
}

determine_config_file_name() {
    if [ ${APP_TYPE} == "dropwizard" ] ; then
        CONFIG_FILE_NAME="config.yml"
        FRAMEWORK_ARGS="server ${CONFIG_FILE_NAME}"
    elif [ ${APP_TYPE} == "springboot" ] ; then
        CONFIG_FILE_NAME="application.$(echo ${CONFIG_FILE_LOCATION} | rev | cut -d '.' -f 1 | rev)"
        FRAMEWORK_ARGS="--spring.config.location=${CONFIG_FILE_NAME}"
    else
        echo -e >&2 "Invalid app type provided.\nValid values are: dropwizard, springboot\n Aborting"
        exit 1
    fi
}

require_arg "${1}" "PROJECT_NAME"
require_arg "${2}" "APP_NAME"
require_arg "${3}" "JAR_LOCATION"
require_arg "${4}" "APP_TYPE"
require_arg "${5}" "CONFIG_FILE_LOCATION"
require_arg "${6}" "CREATE_TMP_DIR"

require fpm
require envsubst
require awk
require unzip
require grep
require cut
require rev

determine_config_file_name

export PROJECT_NAME APP_NAME FRAMEWORK_ARGS
mkdir -p java-rpm-packaging/bin/service

USERNAME=${APP_USER:-webapp}
export USERNAME

envsubst '${PROJECT_NAME} ${APP_NAME} ${USERNAME} ${FRAMEWORK_ARGS}' < java-rpm-packaging/bin/service.systemd.template \
            > java-rpm-packaging/bin/service/${PROJECT_NAME}-${APP_NAME}

VERSION="$(unzip -p ${JAR_LOCATION} META-INF/MANIFEST.MF | grep 'Implementation-Version' | cut -d ':' -f2 | awk '{$1=$1};1')"
VERSION="${VERSION//[[:space:]]/}"
build="${BUILD_NUMBER:-$RANDOM}"
fpm -n "${PROJECT_NAME}-${APP_NAME}"                        `# Name of the package` \
    -v "${VERSION}"                                         `# Version of the package` \
    --package "java-rpm-packaging/"                         `# The package file path to output.` \
    --iteration "$build"                                    `# Ensure each build is unique` \
    -s dir                                                  `# Source type: directory` \
    -t rpm                                                  `# Target type: RPM` \
    --rpm-user "${USERNAME}"                                   `# User who owns the files in the package` \
    --rpm-os "linux"                                        `# The operating system to target this rpm for.` \
    --maintainer '<noreply@hmcts.net>'                      `# Email address for who is responsible` \
    --description 'Java application'                     `# Set the description of the package` \
    --directories "/opt/${PROJECT_NAME}/${APP_NAME}"        `# Mark the target deployment folder as owned by the rpm-user` \
    --depends "java-1.8.0-openjdk-headless"                          `# Install java` \
    --rpm-attr 644,root,root,:/lib/systemd/system/${PROJECT_NAME}-${APP_NAME}.service  `# Make root own startup file` \
    --template-scripts                                      `# Enable erb templates` \
    --template-value name=${PROJECT_NAME}-${APP_NAME}       `# Variable definition for script` \
    --template-value project=${PROJECT_NAME}                `# Variable definition for script` \
    --template-value shortname=${APP_NAME}                  `# Variable definition for script` \
    --template-value config_file_name=${CONFIG_FILE_NAME}                  `# Variable definition for script` \
    --template-value username=${USERNAME}                    `# Variable definition for script` \
    --template-value app_type=${APP_TYPE}                    `# Variable definition for script` \
    --template-value create_tmp_dir=${CREATE_TMP_DIR}        `# Variable definition for script` \
	  --before-install java-rpm-packaging/bin/before-install.sh.erb            `# Run before-install script` \
    --after-install java-rpm-packaging/bin/after-install.sh.erb              `# Run post-install script` \
    --before-upgrade java-rpm-packaging/bin/before-upgrade.sh.erb            `# Run before-upgrade script` \
    --after-upgrade java-rpm-packaging/bin/after-upgrade.sh.erb            `# Run after-upgrade script` \
    ${JAR_LOCATION}=/opt/${PROJECT_NAME}/${APP_NAME}/${APP_NAME}.jar       `# Include the jar file in the package` \
    ${CONFIG_FILE_LOCATION}=/opt/${PROJECT_NAME}/${APP_NAME}/${CONFIG_FILE_NAME}       `# Include the config file in the package` \
    java-rpm-packaging/bin/service/${PROJECT_NAME}-${APP_NAME}=/lib/systemd/system/${PROJECT_NAME}-${APP_NAME}.service

