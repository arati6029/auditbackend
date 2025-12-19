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
    agent any // or specify a label with Docker support
    steps {
        script {
            // Use Docker Pipeline plugin
            docker.withRegistry('https://index.docker.io/v1/', 'docker-hub-credentials') {
                // Build the image
                def dockerImage = docker.build("${DOCKER_IMAGE}:${DOCKER_TAG}")
                
                // Tag as latest
                dockerImage.push()
                dockerImage.push('latest')
            }
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