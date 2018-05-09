package uk.gov.hmcts

class ReviewAppProvisioner implements Serializable {
  def steps
  def env

  Ansible ansible

  String organisationOwner
  String repositoryName
  String productName

  ReviewAppProvisioner(steps, env, String organisationOwner, String repositoryName, String productName) {
    this.steps = steps
    this.env = env
    this.organisationOwner = organisationOwner
    this.repositoryName = repositoryName
    this.productName = productName

    ansible = new Ansible(steps, productName)
  }

  /**
   * Deploy a review app
   *
   * @param reviewAppKey the identifier that your app will be keyed on this will be used for your app url and to ensure that only one app per pull-request starts up
   * @param reviewAppVariableKey the variable to be used to override the image in your compose file
   * @param reviewAppVariable the image version to be used
   */
  def start(String reviewAppKey, String reviewAppVariableKey, String reviewAppVariable) {
    def version = "{ story_id: $reviewAppKey, app_version_variable: $reviewAppVariableKey, app_version: $reviewAppVariable  }"

    steps.withCredentials([[$class: 'StringBinding', credentialsId: 'jenkins-github-api-text', variable: 'GITHUB_API_TOKEN']]) {
      def deploymentId = notifyGitHubDeploymentStarting(reviewAppKey)
      ansible.run(version, 'dev', 'review-app.yml')

      notifyGitHubDeploymentSucceeded(reviewAppKey, deploymentId)
    }
  }

  /**
   * Stop a review app
   *
   * @param reviewAppKey the identifier that the app to shutdown is keyed on
   */
  def stop(String reviewAppKey) {
    def version = "{ story_id: $reviewAppKey, mode: shutdown }"
    ansible.run(version, 'dev', 'review-app.yml')
  }

  private String notifyGitHubDeploymentStarting(String reviewAppKey) {
    def currentVersion = steps.sh(returnStdout: true, script: "git rev-parse HEAD").trim()

    return steps.sh(returnStdout: true, script: """
      curl -sS -X POST -H "Authorization: Token ${env.GITHUB_API_TOKEN}" -H "Content-Type: application/json" -d '{
          "ref": "${currentVersion}",
          "environment": "review-app",
          "description": "Deploying '${reviewAppKey}' to review application",
          "required_contexts" : []
        }' "https://git.reform.hmcts.net/api/v3/repos/${organisationOwner}/${repositoryName}/deployments" | python -c 'import sys, json; print json.load(sys.stdin)["id"]'
        """).trim()
  }

  private void notifyGitHubDeploymentSucceeded(String reviewAppKey, String deploymentId) {
    steps.sh """
          curl -sS -X POST -H "Authorization: Token ${env.GITHUB_API_TOKEN}" -H "Content-Type: application/json" -H "Accept: application/vnd.github.ant-man-preview+json" -d '{
          "state": "success",
          "description": "Deployed ${reviewAppKey} to review application",
          "environment_url": "http://${reviewAppKey}.review-app.${productName}.reform.hmcts.net"
          }' "https://git.reform.hmcts.net/api/v3/repos/${organisationOwner}/${repositoryName}/deployments/${deploymentId}/statuses"
        """
  }
}
