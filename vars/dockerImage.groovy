import uk.gov.hmcts.Artifactory

/**
 * Build and optionally publish Docker image to Reform repository
 * <p>
 * Docker image is built using Dockerfile located in the directory specified by context parameter (defaults to current directory).
 * <p>
 * Version of the image is assigned automatically based on the Jenkins job name and current build number (e.g. PR-15-01 if pull request are configured).
 * <p>
 * When push option is selected images will get pushed to the Reform Docker Registry (see: {@link dockerImage#getRegistry()} and {@link dockerImage#getRegistryCredentialId()}).
 * <p>
 * <b>Example:</b>
 *
 * <pre>
 *   dockerImage imageName: 'cmc/claim-store-api', tags: ['ROC-512']
 *   dockerImage imageName: 'cmc/claim-store-database', context: 'docker/database', tags: ['ROC-512']
 * </pre>
 *
 * @param args arguments:
 *  <ul>
 *      <li>imageName - (string; required) name of the image, not prefixed with repository name (e.g. 'cmc/claim-store-api')</li>
 *      <li>imageVersion - (string; default = <commitSHA>) default version to build</li>
 *      <li>context - (string; default = '.') directory used as Docker context, path may be relative or absolute</li>
 *      <li>push - (boolean; default = false) indicates whether to push images to the Reform Docker Registry</li>
 *      <li>tags - (string[]; default = []) additional tags to be created and pushed to the Reform Docker Registry</li>
 *      <li>dockerArgs - (string[]; default = []) additional args to be passed to the docker cli during build</li>
 *      <li>pushToLatestOnMaster - (boolean; default = true) Push this image to the latest tag if on master</li>
 *  </ul>
 */
def call(Map args = [:]) {
  def config = [
    imageVersion: defaultImageVersion,
    context     : '.',
    push        : true,
    tags        : [],
    dockerArgs   : [],
    pushToLatestOnMaster: true,
  ] << args

  validate(config)
  buildImage(config)

  return config.imageVersion
}

private validate(Map args) {
  if (args.imageName == null) throw new Exception('Docker image name is required')
  if (args.context == null) throw new Exception('Docker context is required')
  if (args.imageVersion == null) throw new Exception('Image version is required')
}

private buildImage(Map args) {
  def image = docker.build(
    "${registry}/${args.imageName}:${args.imageVersion}",
    "${getBuildOptions(args)} ${args.context}"
  )

  if (args.push) {
    docker.withRegistry("http://${registry}", registryCredentialId) {
      image.push() // pushes default tag with build number

      tagImageWithBuildProperties(args.imageName, args.imageVersion)

      if (env.BRANCH_NAME == 'master' && args.pushToLatestOnMaster) {
        args.tags << 'latest'
      }

      for (tag in args.tags.unique()) {
        if (tag != null) {
          image.push(tag) // push additional tags
        }
      }
    }
  }

  image
}

private String getRegistry() {
  'docker.artifactory.reform.hmcts.net'
}

private String getRegistryCredentialId() {
  'ArtifactoryDeploy'
}

private String getDefaultImageVersion() {
  sh(returnStdout: true, script: "git rev-parse HEAD").trim()
}

private String getBuildOptions(Map args) {
  def options = ['--force-rm', '--pull', "--cache-from ${registry}/${args.imageName}:latest"].plus(args.dockerArgs)
  if (env.http_proxy) {
    options << "--build-arg http_proxy=${env.http_proxy}"
  }
  if (env.https_proxy) {
    options << "--build-arg https_proxy=${env.https_proxy}"
  }
  if (env.no_proxy) {
    options << "--build-arg no_proxy=${env.no_proxy}"
  }
  options.join(' ')
}

private tagImageWithBuildProperties(imageName, imageVersion) {
  def artifactory = new Artifactory(this)

  def path = "${imageName}/${imageVersion}/manifest.json"
  def repoKey = 'docker-local'

  artifactory.createProperty(path, repoKey, 'build.number', env.BUILD_ID)
  artifactory.createProperty(path, repoKey, 'build.branch', env.CHANGE_BRANCH ? env.CHANGE_BRANCH : env.BRANCH_NAME)
}
