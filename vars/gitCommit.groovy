/**
 * Returns the git commit of the current checked out repo
 */
def call() {
  return sh(returnStdout: true, script: "git rev-parse HEAD").trim()
}
