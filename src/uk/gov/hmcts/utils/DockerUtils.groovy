package uk.gov.hmcts.utils

class DockerUtils {

  static toTagName(String value) {
    if (value == null || value.trim() == '') {
      throw new Exception('Cannot create tag from null or blank string')
    }

    value
      .replaceAll(/\//, '.')
      .replaceAll(/(?i)[^.0-9A-Z]/, '-')
      .toUpperCase()
  }
}
