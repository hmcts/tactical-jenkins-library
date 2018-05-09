package uk.gov.hmcts.utils

import spock.lang.Specification
import spock.lang.Unroll

class DockerUtilsSpec extends Specification {

  @Unroll
  def "should create tag '#tagName' from branch '#branchName'"() {
    expect:
      DockerUtils.toTagName(branchName) == tagName

    where:
      branchName                          || tagName
      'PR-01'                             || 'PR-01'
      'registration-page'                 || 'REGISTRATION-PAGE'
      'registration_page'                 || 'REGISTRATION-PAGE'
      'feature/registration-page'         || 'FEATURE.REGISTRATION-PAGE'
      'feature/registration_page'         || 'FEATURE.REGISTRATION-PAGE'
      'feature/ROC-001-registration-page' || 'FEATURE.ROC-001-REGISTRATION-PAGE'
      'feature/ROC-001-registration_page' || 'FEATURE.ROC-001-REGISTRATION-PAGE'
      'feature/ROC-001/registration-page' || 'FEATURE.ROC-001.REGISTRATION-PAGE'
      'feature/ROC-001/registration_page' || 'FEATURE.ROC-001.REGISTRATION-PAGE'
  }
}
