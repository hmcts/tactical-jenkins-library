package uk.gov.hmcts

class Versioner implements Serializable {
  def steps

  Versioner(steps) {
    this.steps = steps
  }

  def addNodeVersionInfo() {
    steps.sh '''
     echo "version: $(node -pe 'require("./package.json").version')" >> version
     echo "number: ${BUILD_NUMBER}" >> version
     echo "commit: $(git rev-parse --short HEAD)" >> version
     echo "date: $(date)" >> version
    '''
  }

  def addJavaVersionInfo() {
    steps.sh '''
      mkdir -p src/main/resources/META-INF
      echo "build.version=$(./gradlew -q printVersion)" >> src/main/resources/META-INF/build-info.properties
      echo "build.number=${BUILD_NUMBER}" >> src/main/resources/META-INF/build-info.properties
      echo "build.commit=$(git rev-parse --short HEAD)" >> src/main/resources/META-INF/build-info.properties
      echo "build.date=$(date)" >> src/main/resources/META-INF/build-info.properties
    '''
  }

  def addJavaVersionInfoWithMaven() {
    steps.sh '''
      mkdir -p src/main/resources/META-INF
      echo "build.version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version 2>/dev/null | grep -Ev '(^\\[|Download\\w+:)')" >> src/main/resources/META-INF/build-info.properties
      echo "build.number=${BUILD_NUMBER}" >> src/main/resources/META-INF/build-info.properties
      echo "build.commit=$(git rev-parse --short HEAD)" >> src/main/resources/META-INF/build-info.properties
      echo "build.date=$(date)" >> src/main/resources/META-INF/build-info.properties
    '''
  }

}

