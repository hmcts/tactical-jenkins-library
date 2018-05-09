/*
 * checkNodeVersionPublished
 *
 * Returns true if Node package version is already published
 */
def call(Map args = [:]) {
  def config = [
    packageName: extractFromNodePackage('name'),
    version: extractFromNodePackage('version'),
  ] << args

  return checkVersionIsPublished(config)
}

private checkVersionIsPublished(config) {
  lastPublishedVersion = sh(
    returnStdout: true,
    script: "npm show ${config.packageName} version | tr -d '\\n'"
  )

  return [
    published: lastPublishedVersion == config.version,
    lastPublishedVersion: lastPublishedVersion
  ]
}
