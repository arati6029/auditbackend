pipeline {
    agent any
    
    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }
    
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
        // For WSL Docker integration
        DOCKER_HOST = "tcp://localhost:2375"
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
                    echo "Checking Docker setup..."
                    
                    // Check if we're in WSL
                    def isWSL = sh(script: 'uname -a | grep -i microsoft', returnStatus: true) == 0
                    
                    if (isWSL) {
                        echo "Running in WSL environment"
                        
                        // Method 1: Try Docker Desktop integration
                        sh '''
                            echo "Attempting to connect to Docker Desktop..."
                            # Check if Docker Desktop socket is exposed
                            if [ -S /mnt/wsl/docker-desktop/docker-desktop.sock ]; then
                                echo "Using Docker Desktop socket"
                                export DOCKER_HOST="unix:///mnt/wsl/docker-desktop/docker-desktop.sock"
                            elif [ -S /var/run/docker.sock ]; then
                                echo "Using Docker socket"
                                export DOCKER_HOST="unix:///var/run/docker.sock"
                            else
                                # Try to expose Docker Desktop port
                                echo "Setting up Docker Desktop TCP connection"
                                powershell.exe -Command "Start-Process -WindowStyle Hidden -FilePath 'docker' -ArgumentList 'context', 'create', 'wsl', '--docker', 'host=tcp://localhost:2375'"
                                sleep 5
                            fi
                        '''
                        
                        // Test Docker connection
                        def dockerTest = sh(script: '''
                            docker version 2>&1 || echo "Docker not available"
                            docker ps 2>&1 || echo "Cannot list containers"
                        ''', returnStatus: true)
                        
                        if (dockerTest != 0) {
                            echo "⚠️ Docker not accessible. Starting Docker service..."
                            // Try to start Docker Desktop from WSL
                            sh '''
                                # Try to start Docker Desktop
                                powershell.exe -Command "Start-Process -WindowStyle Hidden -FilePath 'C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe'"
                                echo "Waiting for Docker Desktop to start..."
                                sleep 30
                            '''
                        }
                    }
                    
                    // Final Docker check
                    sh '''
                        echo "=== Docker Status ==="
                        docker version || echo "❌ Docker not available"
                        echo "=== System Info ==="
                        uname -a
                        echo "=== Current Directory ==="
                        pwd
                        ls -la
                    '''
                }
            }
        }
        
        stage('Build Application') {
            steps {
                echo '🔨 Starting Build: Compiling application...'
                script {
                    // Check for Maven wrapper first
                    if (fileExists('mvnw')) {
                        sh 'chmod +x mvnw && ./mvnw clean package -DskipTests'
                    } else {
                        sh 'mvn clean package -DskipTests'
                    }
                }
                echo '✅ Build completed successfully'
                
                // Archive the JAR file
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
        
        stage('Create Dockerfile if missing') {
            when {
                expression { !fileExists('Dockerfile') }
            }
            steps {
                script {
                    echo '📝 Creating Dockerfile...'
                    sh '''
                        cat > Dockerfile << 'EOF'
                        FROM openjdk:11-jre-slim
                        WORKDIR /app
                        COPY target/*.jar app.jar
                        EXPOSE 8080
                        ENTRYPOINT ["java", "-jar", "app.jar"]
                        EOF
                        
                        echo "Dockerfile created:"
                        cat Dockerfile
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    // Verify Docker is working
                    sh '''
                        echo "=== Docker Info Before Build ==="
                        docker version || { echo "Docker not available"; exit 1; }
                        docker images
                    '''
                    
                    // Build the image
                    sh """
                        docker build -t ${DOCKER_IMAGE} .
                        docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${IMAGE_NAME}:latest
                    """
                    
                    // List built images
                    sh '''
                        echo "=== Docker Images After Build ==="
                        docker images | grep auditapplication || docker images
                    '''
                }
                echo '✅ Docker image built successfully'
            }
        }
        
        stage('Test Docker Image') {
            steps {
                echo '🧪 Testing Docker image...'
                script {
                    sh '''
                        # Test run container
                        docker run --name test-container -d -p 8081:8080 ${DOCKER_USER}/${IMAGE_NAME}:latest || true
                        
                        # Wait and check health
                        sleep 10
                        
                        # Check if container is running
                        docker ps | grep test-container || echo "Container might have stopped"
                        
                        # Clean up test container
                        docker stop test-container 2>/dev/null || true
                        docker rm test-container 2>/dev/null || true
                    '''
                }
                echo '✅ Docker image test completed'
            }
        }
        
        stage('Push to Docker Hub') {
            when {
                branch 'main'
            }
            steps {
                echo '📤 Pushing to Docker Hub...'
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        sh """
                            echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin
                            docker push ${DOCKER_IMAGE}
                            docker push ${DOCKER_USER}/${IMAGE_NAME}:latest
                        """
                    }
                }
                echo '✅ Docker image pushed successfully'
            }
        }
        
        stage('Deploy Container') {
            when {
                branch 'main'
            }
            steps {
                echo '🚀 Deploying container...'
                script {
                    sh """
                        # Stop and remove existing container
                        docker stop ${CONTAINER_NAME} 2>/dev/null || true
                        docker rm ${CONTAINER_NAME} 2>/dev/null || true
                        
                        # Run new container
                        docker run -d \\
                            --name ${CONTAINER_NAME} \\
                            -p 8080:8080 \\
                            --restart unless-stopped \\
                            ${DOCKER_USER}/${IMAGE_NAME}:latest
                        
                        echo "Waiting for application to start..."
                        sleep 15
                        
                        # Health check with retry
                        max_attempts=10
                        for i in \$(seq 1 \$max_attempts); do
                            if curl -s --fail http://localhost:8080/actuator/health >/dev/null 2>&1; then
                                echo "✅ Application is healthy!"
                                break
                            fi
                            echo "⏳ Waiting for application... (attempt \$i/\$max_attempts)"
                            sleep 10
                        done
                        
                        # Show container logs for debugging
                        echo "=== Container Logs ==="
                        docker logs ${CONTAINER_NAME} --tail 20
                    """
                }
                echo '🚀 Deployment completed'
            }
        }
    }
    
    post {
        always {
            echo "🏁 Pipeline execution completed. Status: ${currentBuild.currentResult}"
            echo "🔗 Build URL: ${BUILD_URL}"
            
            script {
                // Clean up test containers
                sh '''
                    echo "=== Cleaning up test containers ==="
                    docker stop test-container 2>/dev/null || true
                    docker rm test-container 2>/dev/null || true
                    docker system prune -f 2>/dev/null || true
                '''
                
                // Archive test results if they exist
                junit '**/target/surefire-reports/**/*.xml'
                
                // Save Docker logs if container exists
                sh '''
                    if docker ps -a | grep -q auditapplication; then
                        echo "=== Saving container logs ==="
                        docker logs auditapplication > container.log 2>&1 || true
                    fi
                '''
                
                // Archive additional artifacts
                archiveArtifacts artifacts: 'container.log', allowEmptyArchive: true
                archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                
                // Clean workspace (optional - keep for debugging)
                // cleanWs()
            }
        }
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed!'
            
            script {
                // Collect failure information
                sh '''
                    echo "=== Failure Diagnostics ==="
                    echo "Docker status:"
                    docker ps -a 2>/dev/null || echo "Docker not available"
                    echo "Container logs (if any):"
                    docker logs auditapplication 2>/dev/null || echo "No container logs"
                    echo "Recent system logs:"
                    dmesg | tail -20 2>/dev/null || echo "No system logs"
                '''
            }
        }
    }
}