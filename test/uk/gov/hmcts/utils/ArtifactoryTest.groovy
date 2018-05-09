package uk.gov.hmcts.utils

import groovy.json.JsonOutput
import spock.lang.Shared
import spock.lang.Specification
import uk.gov.hmcts.Artifactory

class ArtifactoryTest extends Specification {

  @Shared steps
  @Shared artifactoryServer
  @Shared artifactory


  def setup() {
    steps = Mock(JenkinsStepMock)
    artifactoryServer = Mock(ArtifactoryServerMock)

    steps.getArtifactoryServer(_) >> artifactoryServer
    artifactoryServer.getUrl() >> "https://artifactory"

    artifactory = new Artifactory(steps)

  }


  def "should return the version when there is a valid package"() {

    when:
      steps.httpRequest(_) >> [
        'content': new JsonOutput().toJson(
          [
            results:
              [
                [name: 'foo-product-frontend-1.0.0-417.x86_64.rpm']
              ]
          ])
      ]

      def pkg = artifactory.findLatestEligibleVersionForTest('foo-product', 'frontend')

    then:
      pkg.version == "1.0.0-417"
      pkg.name == 'foo-product-frontend-1.0.0-417.x86_64.rpm'

  }

  def "should return null when there are no valid packages"() {

    when:
      steps.httpRequest(_) >> [
        'content': new JsonOutput().toJson(
          [
            results:
              [

              ]
          ])
      ]

      def pkg = artifactory.findLatestEligibleVersionForTest('foo-product', 'frontend')

    then:
      pkg == null

  }

}


interface JenkinsStepMock {
  ArtifactoryServerMock getArtifactoryServer(artifactoryServerId)

  Object httpRequest(HashMap)
}

interface ArtifactoryServerMock {
  String getUrl()
}
