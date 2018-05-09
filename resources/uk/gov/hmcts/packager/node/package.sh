#!/usr/bin/env bash
set -e

dot="$(cd "$(dirname "$([ -L "$0" ] && $READLINK_CMD -f "$0" || echo "$0")")"; pwd)"
cd $dot/../..
usage() {
   echo "Usage: $(basename ${0}) project_name app_name"
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
require_arg "${1}" "PROJECT_NAME"
require_arg "${2}" "APP_NAME"

require fpm
require yarn
require envsubst
require_file package.json

export PROJECT_NAME APP_NAME
mkdir -p node-rpm-packaging/bin/service

USERNAME=${APP_USER:-webapp}
export USERNAME

envsubst < node-rpm-packaging/bin/service.systemd.template > node-rpm-packaging/bin/service/${PROJECT_NAME}-${APP_NAME}

# Ensure dev dependencies are present for any setup needed
yarn install --check-files
yarn run setup
rm -rf node_modules/
# Ensure only production dependencies get packaged into the RPM
yarn install --production

VERSION="$(node -pe 'require("./package.json").version')"
build="${BUILD_NUMBER:-$RANDOM}"
fpm -n "${PROJECT_NAME}-${APP_NAME}"                        `# Name of the package` \
    -v "${VERSION}"                                         `# Version of the package` \
    --package "node-rpm-packaging/"                         `# The package file path to output.` \
    --iteration "$build"                                    `# Ensure each build is unique` \
    -s dir                                                  `# Source type: directory` \
    -t rpm                                                  `# Target type: RPM` \
    --rpm-user "${USERNAME}"                                   `# User who owns the files in the package` \
    --maintainer '<noreply@hmcts.net>'                      `# Email address for who is responsible` \
    --description 'Node js application'                     `# Set the description of the package` \
    --directories "/opt/${PROJECT_NAME}/${APP_NAME}"        `# Mark the target deployment folder as owned by the rpm-user` \
    --depends "nodejs >= 2:8.8.0-1nodesource"               `# Install node` \
    --exclude "**.rpm"                                      `# Exclude the build artefacts so we dont incept` \
    --exclude "*.git*"                                      `# Exclude git directories` \
    --exclude ".vagrant/"                                   `# Exclude vagrant directories` \
    --exclude "*/test/**"                                   `# Exclude test directories` \
    --exclude "node-rpm-packaging/"                         `# Exclude packaging files` \
    --exclude "bin/*.sh"                                    `# Exclude build script` \
    --exclude "*.md"                                        `# Exclude development artifacts` \
    --rpm-attr 644,root,root,:/lib/systemd/system/${PROJECT_NAME}-${APP_NAME}.service  `# Make root own startup file` \
    --template-scripts                                      `# Enable erb templates` \
    --template-value name=${PROJECT_NAME}-${APP_NAME}       `# Variable definition for script` \
    --template-value shortname=${APP_NAME}                  `# Variable definition for script` \
    --template-value username=${USERNAME}                    `# Variable definition for script` \
	  --before-install node-rpm-packaging/bin/before-install.sh.erb            `# Run before-install script` \
    --after-install node-rpm-packaging/bin/after-install.sh.erb              `# Run post-install script` \
    --before-upgrade node-rpm-packaging/bin/before-upgrade.sh.erb            `# Run before-upgrade script` \
    --after-upgrade node-rpm-packaging/bin/after-upgrade.sh.erb            `# Run after-upgrade script` \
    .=/opt/${PROJECT_NAME}/${APP_NAME}                      `# Include the curent folder in the package` \
    node-rpm-packaging/bin/service/${PROJECT_NAME}-${APP_NAME}=/lib/systemd/system/${PROJECT_NAME}-${APP_NAME}.service

