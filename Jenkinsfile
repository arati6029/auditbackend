pipeline {
    agent {
        node {
            label 'master'  // Or your specific agent label
        }
    }
    
    tools {
        maven 'M3'
    }
    
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
        DOCKER_REGISTRY = "${REGISTRY}/${DOCKER_IMAGE}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup Docker') {
            steps {
                script {
                    // Clean up any existing containers/images
                    sh '''
                        docker stop auditapplication || true
                        docker rm auditapplication || true
                        docker system prune -f || true
                    '''
                    
                    // Verify Docker is working
                    sh 'docker --version'
                }
            }
        }
        
        stage('Build Application') {
            steps {
                script {
                    
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    sh '''
                        mvn test \
                            -Dspring.profiles.active=test \
                            -Dspring.datasource.url=jdbc:h2:mem:testdb \
                            -Dspring.datasource.driver-class-name=org.h2.Driver \
                            -Dspring.datasource.username=sa \
                            -Dspring.datasource.password= \
                            -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
                            -Dtestcontainers.enabled=false
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_REGISTRY} ."
                    sh "docker tag ${DOCKER_REGISTRY} ${DOCKER_USER}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('Run Docker Container') {
            steps {
                script {
                    // Stop and remove existing container
                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm ${CONTAINER_NAME} || true"
                    
                    // Run with proper environment variable handling
                    sh """
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p 8080:8080 \
                            -e SPRING_PROFILES_ACTIVE=prod \
                            ${DOCKER_REGISTRY}
                    """
                    
                    // Wait for container to start
                    sleep 10
                    
                    // Health check with retry logic
                    sh '''
                        max_attempts=10
                        attempt=1
                        while [ $attempt -le $max_attempts ]; do
                            if curl -s --fail http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                echo "Application is healthy!"
                                break
                            fi
                            echo "Waiting for application to start... (attempt $attempt/$max_attempts)"
                            sleep 5
                            attempt=$((attempt+1))
                        done
                        
                        if [ $attempt -gt $max_attempts ]; then
                            echo "Application failed to start within the expected time"
                            exit 1
                        fi
                    '''
                }
            }
        }
        
        stage('Push to Docker Hub') {
            when {
                branch 'main'
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials', 
                        usernameVariable: 'DOCKER_HUB_USER', 
                        passwordVariable: 'DOCKER_HUB_PASSWORD'
                    )]) {
                        sh """
                            echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USER} --password-stdin ${REGISTRY}
                            docker push ${DOCKER_REGISTRY}
                            docker push ${DOCKER_USER}/${IMAGE_NAME}:latest
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            script {
                // Archive test results
                junit '**/target/surefire-reports/**/*.xml'
                
                // Archive build artifacts
                archiveArtifacts 'target/*.jar'
                
                // Clean up Docker resources
                sh """
                    docker stop ${CONTAINER_NAME} || true
                    docker rm ${CONTAINER_NAME} || true
                    docker system prune -f || true
                """
            }
        }
        cleanup {
            cleanWs()
        }
    }
}