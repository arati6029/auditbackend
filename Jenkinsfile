pipeline {
    agent any
    
    environment {
        // Docker Compose project name to avoid conflicts
        COMPOSE_PROJECT_NAME = "auditapp-${BUILD_NUMBER}"
        // Set Docker Compose file explicitly
        COMPOSE_FILE = 'docker-compose.yml'
    }

    tools {
        maven 'M3'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo '✅ Repository checked out successfully'
                
                // Verify system information
                sh '''
                    echo "=== System Information ==="
                    echo "Working directory: $(pwd)"
                    echo "User: $(whoami)"
                    echo "Groups: $(groups)"
                    echo "Docker info:"
                    docker info || echo "Docker not accessible"
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

       stage('Docker Operations') {
            steps {
                script {
                    echo "=== Building and Starting Containers ==="
                    
                    // Cleanup old containers from previous builds
                    sh 'docker-compose down --remove-orphans || true'
                    
                    // Build the image and start services in detached mode
                    // Ensure your docker-compose.yml is in the root directory
                    sh 'docker-compose up --build -d'
                    
                    echo "=== Running Containers ==="
                    sh 'docker ps'
                }
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
                    # Try with sudo if needed
                    docker-compose down --remove-orphans 2>/dev/null || \
                    sudo docker-compose down --remove-orphans 2>/dev/null || true
                    
                    # Clean up Docker resources
                    docker system prune -f 2>/dev/null || \
                    sudo docker system prune -f 2>/dev/null || true
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