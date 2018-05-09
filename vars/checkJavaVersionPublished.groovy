/**
 * <p>checkJavaVersionPublished
 *
 * <p>Returns true if an artifact of given Java artifact is already published to Artifactory
 *
 * @param args a map with <code>group</code>, <code>artifact</code> and <code>version</code> entries which describe
 *  the artifact to check for
 */
def call(Map<String, String> args = [:]) {
  requireNonBlank(args.group, "group")
  requireNonBlank(args.artifact, "artifact name")
  requireNonBlank(args.version, "version")

  def response = httpGetArtifact(args.group, args.artifact, args.version)

  return response.status == 200
}

def requireNonBlank(String value, String parameterName) {
  if (value == null || value.isEmpty()) {
    throw new IllegalArgumentException("${parameterName} was required in the input map, but blank value was retrieved")
  }
}

String httpGetArtifact(String group, String artifact, String version) {
  return httpRequest (
    httpMode: 'GET',
    url: "${artifactoryBaseUrl}/libs-release/uk/gov/hmcts/reform/${group}/${artifact}/${version}/",
    validResponseCodes: '200, 404'
  )
}

String getArtifactoryBaseUrl() {
  def artifactoryServer = Artifactory.server 'artifactory.reform'
  return artifactoryServer.getUrl()
}
