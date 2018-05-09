/*
 * extractFromNodePackage
 *
 * Extracts a parameter from a package.json. Useful for getting the package name
 * or version you need to publish against. Assumes it is called from a directory
 * with a package.json in it.
 */
def call(param) {
  return sh(
    returnStdout: true,
    script: "cat package.json | sed -n 's/.*\"${param}\": \"\\(.*\\)\",/\\1/p' | tr -d '\\n'"
  )
}

