pipeline {
  agent {
    docker {
      image 'maven:3-jdk-9'
      args '-v $HOME/.m2:/root/.m2 -u 0:0'
    }
  }
  stages {
    stage('Build') {
      steps {
        sh 'mvn clean verify'
      }
    }
    stage('javadoc') {
      steps {
        sh 'mvn javadoc:javadoc'
      }
    }
  }
}
