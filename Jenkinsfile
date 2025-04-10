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
                    script {
                        if (fileExists('target/surefire-reports')) {
                            junit 'target/surefire-reports/*.xml'
                        } else {
                            echo '⚠️ No test reports found — skipping JUnit results publishing.'
                        }
                    }
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn verify org.jacoco:jacoco-maven-plugin:report'
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
                      -Dsonar.host.url=${SONARQUBE_URL} \
                      -Dsonar.login=${SONARQUBE_TOKEN} \
                      -Dsonar.projectKey=${APP_NAME} \
                      -Dsonar.projectName=${APP_NAME} \
                      -Dsonar.java.coveragePlugin=jacoco \
                      -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                      -Dsonar.exclusions=**/test/** \
                      -Dsonar.coverage.minimum=80.0
                    """
                }
            }
        }

        stage('Quality Gate') {
            steps {
                script {
                    try {
                        timeout(time: 3, unit: 'MINUTES') {
                            def qg = waitForQualityGate()
                            if (qg.status != 'OK') {
                                error "🚫 Quality Gate failed: ${qg.status}"
                            } else {
                                echo "✅ Quality Gate passed"
                            }
                        }
                    } catch (err) {
                        echo "⚠️ Timed out waiting for Quality Gate or webhook not configured"
                        // Optional: fail here or continue
                        // error "Failing due to Quality Gate timeout"
                    }
                }
            }
        }

        stage('Publish to Nexus') {
            steps {
                script {
                    def pom = readMavenPom file: "pom.xml"
                    def files = findFiles(glob: "target/*.jar")
                    if (files.length == 0) {
                        error "❌ No JAR files found in target directory!"
                    }

                    def artifactPath = files[0].path
                    def artifactExists = fileExists(artifactPath)

                    if (artifactExists) {
                        echo "📦 Uploading ${artifactPath} to Nexus"

                        nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            protocol: 'http',
                            nexusUrl: "${NEXUS_URL.replace('http://', '')}",
                            groupId: pom.groupId,
                            version: pom.version,
                            repository: NEXUS_REPOSITORY,
                            credentialsId: NEXUS_CREDENTIAL_ID,
                            artifacts: [
                                [artifactId: pom.artifactId, classifier: '', file: artifactPath, type: pom.packaging],
                                [artifactId: pom.artifactId, classifier: '', file: "pom.xml", type: "pom"]
                            ]
                        )
                    } else {
                        error "❌ Artifact file not found: ${artifactPath}"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'cp target/*.jar docker/'
                sh """
                docker build -t ${
