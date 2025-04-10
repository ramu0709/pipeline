pipeline {
    agent any

    tools {
        maven 'Maven 3.9.9'
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"

        NEXUS_URL = 'http://40.81.225.71:8081/'
        NEXUS_REPOSITORY = 'maven-releases'
        NEXUS_CREDENTIAL_ID = 'nexus-credentials'

        SONARQUBE_URL = 'http://40.81.225.71:9000/'
        SONARQUBE_TOKEN = credentials('sonarqube-token')

        DOCKER_REGISTRY = 'localhost:5000'
        APP_NAME = 'java-app'
        APP_VERSION = '1.0.0'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn verify jacoco:report'
            }
            post {
                always {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**'
                    )
                }
            }
        }

        stage('Code Quality Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh """
                    mvn sonar:sonar \
                        -Dsonar.projectKey=${APP_NAME} \
                        -Dsonar.projectName=${APP_NAME} \
                        -Dsonar.host.url=${SONARQUBE_URL} \
                        -Dsonar.login=${SONARQUBE_TOKEN} \
                        -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                        -Dsonar.exclusions=**/test/** \
                        -Dsonar.java.coveragePlugin=jacoco
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish to Nexus') {
            steps {
                script {
                    def pom = readMavenPom file: "pom.xml"
                    def artifact = findFiles(glob: "target/*.jar")[0].path

                    if (fileExists(artifact)) {
                        nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            protocol: 'http',
                            nexusUrl: "${NEXUS_URL.replace('http://', '')}",
                            groupId: pom.groupId,
                            version: pom.version,
                            repository: NEXUS_REPOSITORY,
                            credentialsId: NEXUS_CREDENTIAL_ID,
                            artifacts: [
                                [artifactId: pom.artifactId, classifier: '', file: artifact, type: pom.packaging],
                                [artifactId: pom.artifactId, classifier: '', file: "pom.xml", type: "pom"]
                            ]
                        )
                    } else {
                        error "*** JAR file not found: ${artifact}"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'mkdir -p docker && cp target/*.jar docker/'

                sh """
                docker build -t ${APP_NAME}:${APP_VERSION} docker \
                    --build-arg JAR_FILE=\$(ls docker/*.jar | xargs -n 1 basename) \
                    --build-arg USER=ramu
                """
            }
        }

        stage('Push Docker Image') {
            steps {
                sh """
                docker tag ${APP_NAME}:${APP_VERSION} ${DOCKER_REGISTRY}/${APP_NAME}:${APP_VERSION}
                docker push ${DOCKER_REGISTRY}/${APP_NAME}:${APP_VERSION}
                """
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                sh """
                docker run -d --rm --name ${APP_NAME} \
                    -p 8080:8080 \
                    -u ramu \
                    ${DOCKER_REGISTRY}/${APP_NAME}:${APP_VERSION}
                """
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo '✅ Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
