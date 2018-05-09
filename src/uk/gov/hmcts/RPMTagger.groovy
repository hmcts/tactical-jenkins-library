package uk.gov.hmcts

class RPMTagger implements Serializable {

  Artifactory artifactory

  String appName
  String artifactName
  String repoName

  RPMTagger(steps, String appName, String artifactName, String repoName) {
    this.artifactory = new Artifactory(steps)
    this.appName = appName
    this.artifactName = artifactName
    this.repoName = repoName
  }

  def tagGitVersion(String gitVersion) {
    artifactory.createProperty("${appName}/${artifactName}", repoName, "git-tag", gitVersion)
  }

  def tagDeploymentSuccessfulOn(String environment) {
    artifactory.createProperty("${appName}/${artifactName}", repoName, environment + ".deployed", "true")
  }

  def tagTestingPassedOn(String environment) {
    artifactory.createProperty("${appName}/${artifactName}", repoName, environment + ".testOK", "true")
  }

  def tagAnsibleCommit(String tag) {
    artifactory.createProperty("${appName}/${artifactName}", repoName, "ansible-tag", tag)
  }
}
