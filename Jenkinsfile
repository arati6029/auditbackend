pipeline {
    agent any
    
    environment {
        // Docker Hub credentials (configure these in Jenkins credentials store)
        DOCKER_CREDENTIALS = credentials('docker-hub-credentials')
        DOCKER_IMAGE = "your-dockerhub-username/audit-backend"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM
                checkout scm
                
                // Show current branch and commit
                sh 'git branch'
                sh 'git rev-parse HEAD'
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Build the application with Maven
                    sh './mvnw clean package -DskipTests'
                    
                    // Archive the JAR file
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    
                    // Store test results
                    junit '**/surefire-reports/**/*.xml'
                }
            }
        }
        
        stage('Build and Push Docker Image') {
            steps {
                script {
                    // Login to Docker Hub
                    sh "echo $DOCKER_CREDENTIALS_PSW | docker login -u $DOCKER_CREDENTIALS_USR --password-stdin"
                    
                    // Build Docker image
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
        
        stage('Deploy') {
            steps {
                script {
                    // Stop and remove existing container if running
                    sh 'docker stop audit-backend || true'
                    sh 'docker rm audit-backend || true'
                    
                    // Run the new container
                    sh """
                    docker run -d \
                        --name audit-backend \
                        -p 8080:8080 \
                        -e SPRING_PROFILES_ACTIVE=prod \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                }
            }
        }
    }
    
    post {
        always {
            // Clean up workspace
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
            // You can add notification here (e.g., email, Slack, etc.)
        }
    }
}
