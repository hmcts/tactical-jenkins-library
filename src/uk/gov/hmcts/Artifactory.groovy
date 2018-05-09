package uk.gov.hmcts

import groovy.json.JsonSlurper

class Artifactory implements Serializable {

  def steps
  def artifactoryUrl
  def artifactoryServer


  Artifactory(steps) {
    this.steps = steps
    this.artifactoryServer = steps.getArtifactoryServer(artifactoryServerID: 'artifactory.reform')
    this.artifactoryUrl = artifactoryServer.getUrl()
  }

  /**
   * Creates a property in the artifactory
   *
   * @param path artifact path in the repository
   * @param repository what repository the artifact is in
   * @param property what property key to create
   * @param value what property value to create
   */
  def createProperty(path, repository, property, value) {
    loadCreatePropertyFile()

    steps.withCredentials([[$class: 'UsernamePasswordBinding', credentialsId: 'ArtifactoryDeploy', variable: 'ARTIFACTORY_AUTH']]) {
      steps.sh("./create-property.sh ${path} ${repository} ${property} ${value}")
    }
  }

  private loadCreatePropertyFile() {
    loadFile("uk/gov/hmcts/artifactory/create-property.sh", "./create-property.sh")
    steps.sh("chmod +x ./create-property.sh")
  }

  /**
   * Promotes the latest eligible RPM to the team production yum repo
   * @param productName the product i.e divorce
   * @param appName the app i.e. track-your-appeal
   * @return{ git_tag, ansible_tag, full_rpm_name, rpm_version } as JSON
   */
  def promoteLatestEligibleRPMtoProductionRepo(productName, appName) {
    loadPromoteRPMFile()

    def promotionResponse
    steps.withCredentials([[$class: 'UsernamePasswordBinding', credentialsId: 'ArtifactoryDeploy', variable: 'ARTIFACTORY_AUTH']]) {
      promotionResponse = steps.sh(returnStdout: true, script: """
      ./promote-rpm.sh ${productName} ${appName}
      """)
    }


    forceIndex(this.artifactoryUrl, "${productName}-production", appName)
    return steps.readJSON(text: promotionResponse)
  }

  def findLatestDeployed(env, productName, appName, repo="local") {
    return queryProperty(productName, appName, "${productName}-${repo}", "${env}.deployed")
  }

  /**
  * Finds the latest eligible package version for production
  * Looks for an rpm in the <product>-production repo that has the property test.testOK
  * @param productName the product i.e divorce
  * @param appName the app i.e. track-your-appeal
  **/
  def findLatestEligibleVersionForProduction(productName, appName){
    return queryProperty(productName, appName, "${productName}-production", "test.testOK")
  }

  /**
  * Finds the latest eligible package version for test
  * Looks for an rpm in the <product>-local repo that has the property dev.testOK
  * @param productName the product i.e divorce
  * @param appName the app i.e. track-your-appeal
  **/
  def findLatestEligibleVersionForTest(productName, appName){
    return queryProperty(productName, appName, "${productName}-local", "dev.testOK")
  }

  /**
   * Finds the latest eligible package deployed to test
   * Looks for an rpm in the <product>-local repo that has the property test.deployed
   * @param productName the product i.e divorce
   * @param appName the app i.e. track-your-appeal
   **/
  def findLatestVersionDeployedToTest(productName, appName){
    return queryProperty(productName, appName, "${productName}-local", "test.deployed")
  }

  /**
   * Finds the latest eligible package deployed to dev
   * Looks for an rpm in the <product>-local repo that has the property dev.deployed
   * @param productName the product i.e divorce
   * @param appName the app i.e. track-your-appeal
   **/
  def findLatestVersionDeployedToDev(productName, appName){
    return queryProperty(productName, appName, "${productName}-local", "dev.deployed")
  }

  private queryProperty(productName, appName, repo, property){
    def query = buildSearchAQL(repo, productName, appName, property)

    def results = getAQLSearchResults(query)

    if(results.size() > 0) {

      def pkg = results.first()
      def match = (pkg.name =~ /(${productName}-${appName})-(.*)\.x86_64\.rpm/)

      if (match != null) {
        def version = match[0][2]
        return [
          version: version,
          name: pkg.name
        ]
      }
      return null
    }
    return null
  }

  private getAQLSearchResults(query) {
    def response = steps.httpRequest url: "${artifactoryUrl}/api/search/aql", requestBody: query, httpMode: 'POST', acceptType: 'APPLICATION_JSON', authentication: 'ArtifactoryDeploy'

    return new JsonSlurper().parseText(response.content).results
  }

  private buildSearchAQL(repo, productName, appName, property) {
    return  """items.find({
                            "repo" : "${repo}"}, {"name":{"\$match":"${productName}-${appName}*"},
                                "@${property}":{"\$match":"true"}
                            }
                            )
                            .sort({"\$desc": ["created"]})
                            .limit(1)
    """

  }

  private loadPromoteRPMFile() {
    loadFile("uk/gov/hmcts/artifactory/promote-rpm.sh", "./promote-rpm.sh")
    steps.sh("chmod +x ./promote-rpm.sh")
    loadFile("uk/gov/hmcts/artifactory/name-query.aql.template", "./name-query.aql.template")
    loadFile("uk/gov/hmcts/artifactory/full-rpm-query.aql.template", "./full-rpm-query.aql.template")
  }

  private loadFile(fileName, destFilePath) {
    def file = steps.libraryResource fileName
    steps.writeFile file: destFilePath, text: file
  }

  def forceIndex(serverUrl, repoKey, appName) {
    steps.withCredentials([[$class: 'UsernamePasswordBinding', credentialsId: 'ArtifactoryDeploy', variable: 'ARTIFACTORY_AUTH']]) {

      steps.sh """
        curl -sS --fail -u ${steps.env.ARTIFACTORY_AUTH} -X POST "${serverUrl}/api/yum/${repoKey}?${appName}&async=0"
      """
    }
  }

}
