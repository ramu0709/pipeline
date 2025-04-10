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
                        sourcePattern: '**/src/main/java'
