pipeline {
    agent any  // Will use any available agent
    
    environment {
        DOCKER_IMAGE = "arati6029/audit-backend"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git branch'
                sh 'git rev-parse HEAD'
            }
        }
        
        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'chmod +x mvnw'
                        sh './mvnw clean package -DskipTests'
                    } else {
                        bat 'mvnw.cmd clean package -DskipTests'
                    }
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
        
        stage('Build and Push Docker Image') {
            // when {
            //     // Only run this stage if Docker is available
            //     expression { isUnix() && isDockerAvailable() }
            // }
            environment {
                DOCKER_CONFIG = credentials('docker-hub-credentials')
            }
            steps {
                script {
                    // Login to Docker Hub
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh "echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin"
                    }
                    
                    // Build and tag Docker image
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                    
                    // Push to Docker Hub
                    sh "docker push ${DOCKER_IMAGE}:${DOCKER_TAG}"
                    sh "docker push ${DOCKER_IMAGE}:latest"
                    
                    // Clean up
                    sh 'docker logout'
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}

// Helper function to check if Docker is available
def isDockerAvailable() {
    try {
        if (isUnix()) {
            return sh(script: 'command -v docker >/dev/null 2>&1 && docker --version', returnStatus: true) == 0
        }
    } catch (Exception e) {
        echo "Docker check failed: ${e.message}"
    }
    return false
}