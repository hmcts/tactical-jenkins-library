package uk.gov.hmcts

class Packager implements Serializable {
  def steps
  def productName

  Packager(steps, productName) {
    this.steps = steps
    this.productName = productName
  }

  /**
   * Packages node RPMs
   * @param version @deprecated need to remove
   * @param appName the application name
   * @return the version of the built RPM
   */
  String nodeRPM(version = '', appName) {
    loadNodeFiles()

    new Versioner(steps).addNodeVersionInfo()

    steps.sh """
       export PATH=/usr/local:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin
       
       chmod +x ./node-rpm-packaging/bin/package.sh
      ./node-rpm-packaging/bin/package.sh "${productName}" "${appName}"
    """

    steps.archiveArtifacts 'node-rpm-packaging/*.rpm'

    return findRPMVersion('node', appName)
  }

  /**
   * Packages java RPMs
   * @param version @deprecated need to remove
   * @param appName the application name
   * @param jarPath path to jar
   * @param javaAppType type of java application springboot or dropwizard
   * @param configPath path to application config file
   * @param createTmpDir whether to create a tmp directory within service's directory
   * @return the version of the built RPM
   */
  String javaRPM(version = '', appName, jarPath, javaAppType, configPath, createTmpDir = true) {
    loadJavaFiles()

    steps.sh """
       export PATH=/usr/local:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin

       chmod +x ./java-rpm-packaging/bin/package.sh
      ./java-rpm-packaging/bin/package.sh "${productName}" "${appName}" "${jarPath}" "${javaAppType}" "${configPath}" "${createTmpDir}"
    """

    steps.archiveArtifacts 'java-rpm-packaging/*.rpm'

    return findRPMVersion('java', appName)
  }

  private String findRPMVersion(appType, appName) {
    return steps.sh(returnStdout: true,
      script: """
        find ${appType}-rpm-packaging/*.rpm | cut -d '/' -f2 | sed -e "s/^${productName}-${appName}-//" | sed -e "s/.x86_64.rpm//"
      """).trim()
  }

  private String findRPMName(appType, appName) {
    return steps.sh(returnStdout: true,
      script: """
        find ${appType}-rpm-packaging/*.rpm | cut -d '/' -f2
      """).trim()
  }

  def publishJavaRPM(appName) {
    return publishRPM('java', appName)
  }

  def publishNodeRPM(appName) {
    return publishRPM('node', appName)
  }

  String rpmName(String appName, String rpmVersion) {
    return "${productName}-${appName}-" + rpmVersion + ".x86_64.rpm"
  }

  private publishRPM(appType, appName) {
    def server = steps.getArtifactoryServer artifactoryServerID: 'artifactory.reform'

    def repo = "${productName}-local"
    def uploadSpec = """{
                "files": [
                            {
                                "pattern": "${appType}-rpm-packaging/*.rpm",
                                "target": "${repo}/${appName}/"
                            }
                        ]
                        }"""

    def buildInfo = steps.artifactoryUpload spec: uploadSpec, server: server
    steps.publishBuildInfo buildInfo: buildInfo, server: server
    new Artifactory(steps).forceIndex(server.getUrl(), repo, appName)
    def rpmTagger = new RPMTagger(steps, appName, findRPMName(appType, appName), productName + "-local")
    rpmTagger.tagGitVersion(steps.gitCommit())

    return buildInfo
  }


  private loadFile(appType, fileName) {
    def file = steps.libraryResource "uk/gov/hmcts/packager/${appType}/${fileName}"
    steps.writeFile file: "./${appType}-rpm-packaging/bin/${fileName}", text: file
  }

  private loadNodeFile(fileName) {
    loadFile('node', fileName)
  }

  private loadJavaFile(fileName) {
    loadFile('java', fileName)
  }

  private loadNodeFiles() {
    loadNodeFile('after-install.sh.erb')
    loadNodeFile('after-upgrade.sh.erb')
    loadNodeFile('before-install.sh.erb')
    loadNodeFile('before-upgrade.sh.erb')
    loadNodeFile('package.sh')
    loadNodeFile('service.systemd.template')
  }

  def loadJavaFiles() {
    loadJavaFile('after-install.sh.erb')
    loadJavaFile('after-upgrade.sh.erb')
    loadJavaFile('before-install.sh.erb')
    loadJavaFile('before-upgrade.sh.erb')
    loadJavaFile('package.sh')
    loadJavaFile('service.systemd.template')
  }
}

