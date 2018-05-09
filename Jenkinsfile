#!groovy
properties(
  [[$class: 'GithubProjectProperty', projectUrlStr: 'https://git.reform.hmcts.net/reform/jenkins-library/'],
   pipelineTriggers([
     [$class: 'GitHubPushTrigger'],
     [$class: 'hudson.triggers.TimerTrigger', spec  : 'H 1 * * *']
   ])]
)

@Library('Reform') _

node {
  try {
    stage('Checkout') {
      deleteDir()
      checkout scm
    }

    stage('Build') {
      sh "./gradlew clean build -x test"
    }

    stage('Test') {
      sh "./gradlew test"
    }
  } catch (err) {
    notifyBuildFailure channel: '#jenkins-notifications'
    throw err
  }

  notifyBuildFixed channel: '#development'
}
