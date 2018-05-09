/*
 * enforceNodeVersionBump
 *
 * Fails the build if you haven't bumped the version number of a package.json
 */
def call(Map args = [:]) {
  def config = [
    packageName: extractFromNodePackage('name'),
    version: extractFromNodePackage('version'),
  ] << args

  stage('Version Check') {
    withNPMRC {
      checkVersionIsBumped(config)
    }
  }
}

private checkVersionIsBumped(args) {
  packageInfo = checkNodeVersionPublished(args)

  echo "${args.packageName}:" +
       "  Published: ${packageInfo.lastPublishedVersion}" +
       "  Proposed: ${args.version}"

  if (packageInfo.published) {
    echo "Package ${args.packageName} needs it's version bumped \n" +
         "  Last published version: ${packageInfo.lastPublishedVersion} \n" +
         "  Current version: ${args.version}"

    error "Version needs bumped, ${args.packageName}:${lastPublishedVersion} already published"
  }

}
