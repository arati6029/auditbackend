pipeline {
    agent any
    
    options {
        timestamps()
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
                echo '🔍 Starting Checkout stage: Cloning repository...'
                checkout scm
                echo '✅ Repository cloned successfully'
            }
        }
        
        stage('Setup Docker') {
            steps {
                echo '🐳 Starting Docker Setup: Cleaning up existing containers...'
                script {
                    sh '''
                        docker stop auditapplication || true
                        docker rm auditapplication || true
                        docker system prune -f || true
                    '''
                    sh 'docker --version'
                }
                echo '✅ Docker setup completed'
            }
        }
        
        stage('Build Application') {
            steps {
                echo '🔨 Starting Build: Compiling application...'
                script {
                    sh 'mvn clean package -DskipTests'
                }
                echo '✅ Build completed successfully'
            }
        }
        
        stage('Test') {
            steps {
                echo '🧪 Starting Tests: Running test suite...'
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
                echo '✅ Tests completed successfully'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    sh "docker build -t ${DOCKER_REGISTRY} ."
                    sh "docker tag ${DOCKER_REGISTRY} ${DOCKER_USER}/${IMAGE_NAME}:latest"
                }
                echo '✅ Docker image built successfully'
            }
        }

        stage('Run Docker Container') {
            steps {
                echo '🚀 Starting Docker container...'
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
                    echo '🩺 Checking application health...'
                    sh '''
                        max_attempts=10
                        attempt=1
                        while [ $attempt -le $max_attempts ]; do
                            if curl -s --fail http://localhost:8080/actuator/health > /dev/null 2>&1; then
                                echo "✅ Application is healthy!"
                                break
                            fi
                            echo "⏳ Waiting for application to start... (attempt $attempt/$max_attempts)"
                            sleep 5
                            attempt=$((attempt+1))
                        done
                        
                        if [ $attempt -gt $max_attempts ]; then
                            echo "❌ Application failed to start within the expected time"
                            exit 1
                        fi
                    '''
                    echo '🚀 Container started successfully'
                }
            }
        }
    }
    
    post {
        always {
            echo "🏁 Pipeline execution completed. Status: ${currentBuild.currentResult}"
            echo "🔗 Build URL: ${BUILD_URL}"
            script {
                // Archive test results
                junit '**/target/surefire-reports/**/*.xml'
                // Clean up workspace
                cleanWs()
            }
        }
    }
}