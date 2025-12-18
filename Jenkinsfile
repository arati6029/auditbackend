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
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '🔍 Starting Checkout stage...'
                checkout scm
                echo '✅ Repository cloned successfully'
            }
        }
        
        stage('Verify Workspace') {
            steps {
                script {
                    sh '''
                        echo "=== Workspace Contents ==="
                        pwd
                        ls -la
                        echo "=== Maven Status ==="
                        which mvn || echo "Maven not in PATH"
                        java -version
                    '''
                }
            }
        }
        
        stage('Build Application') {
            steps {
                echo '🔨 Building application with Maven...'
                script {
                    // Use Maven wrapper if available, otherwise use system Maven
                    if (fileExists('mvnw')) {
                        sh 'chmod +x mvnw && ./mvnw clean package -DskipTests'
                    } else {
                        // Install Maven if not available
                        sh '''
                            if ! command -v mvn >/dev/null 2>&1; then
                                echo "Installing Maven..."
                                sudo apt-get update -qq
                                sudo apt-get install -y maven
                            fi
                            mvn clean package -DskipTests
                        '''
                    }
                }
                echo '✅ Build completed successfully'
            }
        }
        
        stage('Create Docker Assets') {
            steps {
                echo '📝 Creating Docker build assets...'
                script {
                    sh '''
                        echo "=== Creating Dockerfile ==="
                        cat > Dockerfile << 'EOF'
                        # Use multi-stage build
                        FROM maven:3.8.4-openjdk-11-slim AS builder
                        WORKDIR /app
                        COPY pom.xml .
                        RUN mvn dependency:go-offline
                        COPY src ./src
                        RUN mvn clean package -DskipTests

                        FROM openjdk:11-jre-slim
                        WORKDIR /app
                        COPY --from=builder /app/target/*.jar app.jar
                        EXPOSE 8080
                        ENTRYPOINT ["java", "-jar", "app.jar"]
                        EOF
                        
                        echo "Dockerfile created:"
                        cat Dockerfile
                        
                        echo "=== Creating .dockerignore ==="
                        cat > .dockerignore << 'EOF'
                        **/.git
                        **/.mvn
                        **/target
                        **/node_modules
                        **/dist
                        **/*.iml
                        **/.idea
                        **/*.log
                        EOF
                    '''
                }
                echo '✅ Docker assets created'
            }
        }
        
        stage('Save Artifacts') {
            steps {
                echo '💾 Saving build artifacts...'
                script {
                    // Archive the JAR file
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    
                    // Save Dockerfile
                    archiveArtifacts artifacts: 'Dockerfile', fingerprint: true
                    
                    // Create a build info file
                    sh '''
                        echo "Build Information" > build-info.txt
                        echo "================" >> build-info.txt
                        echo "Build Number: ${BUILD_NUMBER}" >> build-info.txt
                        echo "Branch: ${BRANCH_NAME}" >> build-info.txt
                        echo "Timestamp: $(date)" >> build-info.txt
                        echo "JAR File: $(ls target/*.jar)" >> build-info.txt
                        echo "Commit: $(git rev-parse HEAD)" >> build-info.txt
                    '''
                    archiveArtifacts artifacts: 'build-info.txt', fingerprint: true
                }
                echo '✅ Artifacts saved successfully'
            }
        }
        
        stage('Create Docker Build Script') {
            steps {
                echo '📦 Creating Docker build script...'
                script {
                    sh '''
                        echo "=== Creating Docker build script ==="
                        cat > build-docker.sh << 'EOF'
                        #!/bin/bash
                        echo "Docker Build Script"
                        echo "=================="
                        
                        # Check if Docker is available
                        if ! command -v docker >/dev/null 2>&1; then
                            echo "ERROR: Docker is not installed or not in PATH"
                            echo "To install Docker on Ubuntu/WSL:"
                            echo "1. sudo apt-get update"
                            echo "2. sudo apt-get install docker.io"
                            echo "3. sudo usermod -aG docker $USER"
                            echo "4. newgrp docker"
                            exit 1
                        fi
                        
                        # Check Docker daemon
                        if ! docker info >/dev/null 2>&1; then
                            echo "ERROR: Docker daemon is not running"
                            echo "For WSL2 with Docker Desktop:"
                            echo "1. Ensure Docker Desktop is running on Windows"
                            echo "2. In Docker Desktop Settings:"
                            echo "   - Enable WSL Integration"
                            echo "   - Expose daemon on tcp://localhost:2375"
                            echo "3. Export: export DOCKER_HOST=tcp://localhost:2375"
                            exit 1
                        fi
                        
                        # Build parameters
                        DOCKER_USER="arati6029"
                        IMAGE_NAME="auditapplication-app"
                        BUILD_NUMBER="$1"
                        
                        if [ -z "$BUILD_NUMBER" ]; then
                            BUILD_NUMBER="latest"
                        fi
                        
                        DOCKER_IMAGE="${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
                        
                        echo "Building Docker image: ${DOCKER_IMAGE}"
                        echo "Docker version:"
                        docker version
                        
                        # Build the image
                        docker build -t ${DOCKER_IMAGE} .
                        
                        # Tag as latest
                        docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${IMAGE_NAME}:latest
                        
                        echo "Images created:"
                        docker images | grep ${IMAGE_NAME}
                        
                        echo "To run the container:"
                        echo "  docker run -d -p 8080:8080 --name auditapplication ${DOCKER_IMAGE}"
                        
                        echo "To push to Docker Hub:"
                        echo "  docker login"
                        echo "  docker push ${DOCKER_IMAGE}"
                        echo "  docker push ${DOCKER_USER}/${IMAGE_NAME}:latest"
                        EOF
                        
                        chmod +x build-docker.sh
                        
                        echo "Script created. To build Docker image manually:"
                        echo "  ./build-docker.sh ${BUILD_NUMBER}"
                    '''
                    archiveArtifacts artifacts: 'build-docker.sh', fingerprint: true
                }
                echo '✅ Docker build script created'
            }
        }
        
        stage('Create Deploy Script') {
            steps {
                echo '🚀 Creating deployment script...'
                script {
                    sh '''
                        echo "=== Creating deployment script ==="
                        cat > deploy.sh << 'EOF'
                        #!/bin/bash
                        echo "Deployment Script"
                        echo "================"
                        
                        CONTAINER_NAME="auditapplication"
                        DOCKER_IMAGE="arati6029/auditapplication-app:latest"
                        
                        echo "Stopping existing container..."
                        docker stop ${CONTAINER_NAME} 2>/dev/null || true
                        docker rm ${CONTAINER_NAME} 2>/dev/null || true
                        
                        echo "Pulling latest image..."
                        docker pull ${DOCKER_IMAGE} || {
                            echo "WARNING: Could not pull image. Building locally..."
                            ./build-docker.sh latest
                        }
                        
                        echo "Starting container..."
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p 8080:8080 \
                            --restart unless-stopped \
                            ${DOCKER_IMAGE}
                        
                        echo "Waiting for application to start..."
                        sleep 10
                        
                        # Health check
                        max_attempts=10
                        for i in $(seq 1 $max_attempts); do
                            if curl -s --fail http://localhost:8080/actuator/health >/dev/null 2>&1; then
                                echo "✅ Application is healthy!"
                                break
                            fi
                            echo "⏳ Waiting for application... (attempt $i/$max_attempts)"
                            sleep 5
                        done
                        
                        echo "Container status:"
                        docker ps | grep ${CONTAINER_NAME}
                        
                        echo "Application URL: http://localhost:8080"
                        echo "Health check: http://localhost:8080/actuator/health"
                        EOF
                        
                        chmod +x deploy.sh
                    '''
                    archiveArtifacts artifacts: 'deploy.sh', fingerprint: true
                }
                echo '✅ Deployment script created'
            }
        }
        
        stage('Test Application') {
            steps {
                echo '🧪 Testing application...'
                script {
                    sh '''
                        echo "=== Running tests ==="
                        if [ -f "mvnw" ]; then
                            ./mvnw test
                        else
                            mvn test
                        fi
                    '''
                }
                echo '✅ Tests completed'
            }
            post {
                always {
                    junit '**/target/surefire-reports/**/*.xml'
                }
            }
        }
    }
    
    post {
        always {
            echo "🏁 Pipeline execution completed. Status: ${currentBuild.currentResult}"
            echo "🔗 Build URL: ${BUILD_URL}"
            
            script {
                // Create summary report
                sh '''
                    echo "=== Build Summary ===" > summary.txt
                    echo "Status: ${currentBuild.currentResult}" >> summary.txt
                    echo "Build: ${BUILD_NUMBER}" >> summary.txt
                    echo "Branch: ${BRANCH_NAME}" >> summary.txt
                    echo "Artifacts created:" >> summary.txt
                    ls -la target/*.jar 2>/dev/null || echo "No JAR files" >> summary.txt
                    echo "Test reports: $(find . -name '*test*.xml' | wc -l)" >> summary.txt
                '''
                archiveArtifacts artifacts: 'summary.txt', fingerprint: true
                
                // Clean workspace
                cleanWs()
            }
        }
        success {
            echo '🎉 Pipeline completed successfully!'
            echo '📋 Next steps:'
            echo '   1. Ensure Docker Desktop is running on Windows'
            echo '   2. Configure WSL integration in Docker Desktop'
            echo '   3. Run: ./build-docker.sh ${BUILD_NUMBER}'
            echo '   4. Run: ./deploy.sh'
        }
        failure {
            echo '❌ Pipeline failed!'
            echo '🔧 Troubleshooting steps:'
            echo '   1. Check Docker Desktop is running'
            echo '   2. Verify WSL integration in Docker Desktop settings'
            echo '   3. Try: export DOCKER_HOST=tcp://localhost:2375'
        }
    }
}