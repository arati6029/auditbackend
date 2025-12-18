pipeline {
    agent any
    
    environment {
        // Docker Compose project name to avoid conflicts
        COMPOSE_PROJECT_NAME = "auditapp-${BUILD_NUMBER}"
        // Set Docker host to use the host's Docker daemon
        DOCKER_HOST = 'unix:///var/run/docker.sock'
        // Set Docker Compose file explicitly
        COMPOSE_FILE = 'docker-compose.yml'
    }

    options {
        // Timeout after 30 minutes
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo '✅ Repository checked out successfully'
                
                // Verify Docker and Docker Compose are available
                sh '''
                    echo "=== System Information ==="
                    echo "Working directory: $(pwd)"
                    echo "Docker version:"
                    docker --version || echo "Docker not found"
                    echo "Docker Compose version:"
                    docker-compose --version || echo "Docker Compose not found"
                '''
            }
        }

        stage('Build Application') {
            steps {
                echo '🔨 Building Spring Boot application...'
                sh 'mvn clean package -DskipTests'
                echo '✅ Application built successfully'
                
                // Archive the built JAR file
                archiveArtifacts 'target/*.jar'
            }
        }

        stage('Build and Start Containers') {
            steps {
                echo '🐳 Building and starting Docker containers...'
                sh '''
                    # Stop and remove any existing containers
                    docker-compose down --remove-orphans || true
                    
                    # Build and start containers in detached mode
                    docker-compose up --build -d
                    
                    # Wait for services to be ready
                    echo "Waiting for services to be ready..."
                    sleep 10
                    
                    # Show running containers
                    echo "=== Running Containers ==="
                    docker-compose ps
                '''
                echo '✅ Containers started successfully'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '🔍 Verifying deployment...'
                script {
                    // Wait for application to be ready with timeout
                    def maxAttempts = 10
                    def attempt = 1
                    
                    while (attempt <= maxAttempts) {
                        try {
                            def health = sh(
                                script: 'curl -s -f http://localhost:8081/actuator/health || echo "unhealthy"',
                                returnStdout: true
                            ).trim()
                            
                            if (health.contains('"status":"UP"')) {
                                echo "✅ Application is healthy!"
                                break
                            }
                            
                            if (attempt >= maxAttempts) {
                                error("❌ Application did not become healthy after ${maxAttempts} attempts")
                            }
                            
                            echo "⏳ Waiting for application to be ready (attempt ${attempt}/${maxAttempts})..."
                            sleep 10
                            attempt++
                            
                        } catch (Exception e) {
                            if (attempt >= maxAttempts) {
                                echo "❌ Error checking application health: ${e.message}"
                                echo "=== Container logs ==="
                                sh 'docker-compose logs --tail=50'
                                error("Failed to verify deployment")
                            }
                            sleep 10
                            attempt++
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "=== Build Status: ${currentBuild.currentResult} ==="
            echo "Build URL: ${BUILD_URL}"
            
            // Always clean up containers
            script {
                echo "Cleaning up Docker resources..."
                sh '''
                    docker-compose down --remove-orphans || true
                    docker system prune -f || true
                '''
            }
            
            // Archive test results if any
            junit '**/target/surefire-reports/**/*.xml'
            
            // Clean workspace
            sh '''
                # Clean up workspace
                echo "Cleaning workspace..."
                find . -mindepth 1 -delete || true
            '''
        }
        
        success {
            echo "🎉 Pipeline executed successfully!"
            echo "Application URL: http://localhost:8081"
            echo "MySQL Port: 3307"
            echo ""
            echo "To view logs: docker-compose logs -f"
            echo "To stop: docker-compose down"
        }
        
        failure {
            echo "❌ Pipeline failed!"
            echo "=== Last 50 lines of logs ==="
            sh 'docker-compose logs --tail=50 || true'
        }
    }
}