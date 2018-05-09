package uk.gov.hmcts.utils

class GitUtils {

  static String branchName(String path) {
    "git -C $path rev-parse --abbrev-ref HEAD".execute().text.trim()
  }
}
