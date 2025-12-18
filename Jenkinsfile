pipeline {
    agent any
    
    options {
        timestamps()
    }
    
    environment {
        // Your environment variables
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '🔍 Starting Checkout stage: Cloning repository...'
                checkout scm
                echo '✅ Repository cloned successfully'
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    // Verify Docker is available
                    def dockerCmd = isUnix() ? 'docker' : 'docker.exe'
                    def dockerAvailable = sh(script: "command -v ${dockerCmd} >/dev/null 2>&1", returnStatus: true) == 0
                    
                    if (!dockerAvailable) {
                        error('❌ Docker is not available. Please ensure Docker is installed and in the PATH.')
                    }
                    
                    // Test Docker
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        sh "docker --version"
                        sh "docker ps"  // This will fail if Docker daemon is not running
                    }
                }
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
        
        stage('Build and Push Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    // Build the Docker image
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        def customImage = docker.build("${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}", ".")
                        
                        // Push the image
                        customImage.push()
                        customImage.push('latest')
                    }
                }
                echo '✅ Docker image built and pushed successfully'
            }
        }

        stage('Run Container') {
            steps {
                echo '🚀 Starting Docker container...'
                script {
                    // Stop and remove any existing container
                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm ${CONTAINER_NAME} || true"
                    
                    // Run the container
                    docker.withRegistry('https://registry.hub.docker.com', 'docker-hub-credentials') {
                        def container = docker.image("${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}").run(
                            "--name ${CONTAINER_NAME} -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod"
                        )
                    }
                    
                    // Health check
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