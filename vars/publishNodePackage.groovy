/*
 * publishNodePackage
 *
 * Publishes an NPM module to artifactory. Assumes that it is called from a
 * directory that contains a package.json
 */
def call() {
  withNPMRC {
    packageInfo = checkNodeVersionPublished()
    if (packageInfo.published) {
      stage('Publish to artifactory') {
      }
      echo "Skipping, ${packageInfo.lastPublishedVersion} already published"
    } else {
      stage('Publish to artifactory') {
        sh 'npm publish --registry https://artifactory.reform.hmcts.net/artifactory/api/npm/npm-local/'
      }
    }
  }
}
