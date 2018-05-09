/*
 * withNPMRC
 *
 * Configures the correct npmrc for talking to Artifactory and cleans up
 * afterwards
 */
def call(Map args = [:], block) {
  def config = [
    scope: packageScope()
  ] << args

  try {
    generateNPMRC(config)
    block.call()
  } finally {
    cleanupNPMRC()
  }
}

private packageScope() {
  return (extractFromNodePackage('name') =~ '^@(.*)/.*$')[0][1]
}

private backupExistingNPMRC() {
  sh '(ls .npmrc 2>/dev/null && mv .npmrc .npmrc.bak ) || true'
}

private cleanupNPMRC() {
  sh '(rm .npmrc 2>/dev/null || true) && (mv .npmrc.bak .npmrc 2>/dev/null || true)'
}

private generateNPMRC(config) {
  backupExistingNPMRC()
  artifactory = [[
    $class: 'UsernamePasswordMultiBinding',
    credentialsId: 'ArtifactoryDeploy',
    usernameVariable: 'USERNAME',
    passwordVariable: 'PASSWORD']]
  withCredentials(artifactory) {
    sh "curl -u$USERNAME:$PASSWORD 'https://artifactory.reform.hmcts.net/artifactory/api/npm/npm-local/auth/${config.scope}' > .npmrc"
  }
}
