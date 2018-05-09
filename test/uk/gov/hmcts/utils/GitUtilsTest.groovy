package uk.gov.hmcts.utils

import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Files

class GitUtilsTest extends Specification {

  @Ignore("Ignored since it fails on Jenkins - to be fixed later")
  def "should return branch name for given path"() {
    setup:
      def repositoryPath = createTempRepository()

    expect:
      GitUtils.branchName(repositoryPath) == 'master'
  }

  private static String createTempRepository() {
    def path = Files.createTempDirectory('test-repository')

    def commands = [
      "git init $path",
      "git -C $path config user.name 'Tester'",
      "git -C $path config user.email 'tester@example.com'",
      "touch $path/VERSION.txt",
      "git -C $path add VERSION.txt",
      "git -C $path commit -m 'Test'"
    ]

    commands.each { it.execute() }

    path
  }

}
