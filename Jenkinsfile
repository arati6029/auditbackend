pipeline {
    agent any
    
    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
        // Credentials ID from Jenkins
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Repository: ${env.GIT_URL}"
                    echo "Branch: ${env.GIT_BRANCH}"
                }
            }
        }
        
        stage('Debug Info') {
            steps {
                script {
                    echo "=== Environment Information ==="
                    sh '''
                        echo "Java version:"
                        java -version 2>&1
                        echo "Maven version:"
                        mvn --version 2>&1 || echo "Maven not installed"
                        echo "Docker availability:"
                        which docker || echo "docker not in PATH"
                        echo "Workspace contents:"
                        ls -la
                    '''
                }
            }
        }
        
        stage('Build Application') {
            agent {
                docker {
                    image 'maven:3.8.4-openjdk-11'
                    args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock'
                    reuseNode true
                }
            }
            steps {
                script {
                    echo "Building application inside Maven container..."
                    
                    // Check for Maven wrapper
                    if (fileExists('mvnw')) {
                        sh 'chmod +x mvnw && ./mvnw clean compile package -DskipTests'
                    } else {
                        sh 'mvn clean compile package -DskipTests'
                    }
                    
                    // Verify build
                    def jarFiles = findFiles(glob: 'target/*.jar')
                    if (jarFiles) {
                        echo "✅ Build successful: ${jarFiles[0].name}"
                        archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    } else {
                        error "❌ No JAR file found after build!"
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/**/*.xml'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image using Docker Pipeline plugin..."
                    
                    // Ensure Dockerfile exists
                    if (!fileExists('Dockerfile')) {
                        writeFile file: 'Dockerfile', text: '''
                            FROM openjdk:11-jre-slim
                            WORKDIR /app
                            COPY target/*.jar app.jar
                            EXPOSE 8080
                            HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
                              CMD curl -f http://localhost:8080/actuator/health || exit 1
                            ENTRYPOINT ["java", "-jar", "app.jar"]
                        '''
                        echo "Created Dockerfile"
                        sh 'cat Dockerfile'
                    }
                    
                    // Build using Docker Pipeline
                    docker.withRegistry("https://${REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
                        def customImage = docker.build("${DOCKER_IMAGE}", "--no-cache .")
                        
                        echo "✅ Image built: ${DOCKER_IMAGE}"
                        
                        // Tag as latest
                        customImage.push("latest")
                        
                        // Also tag with branch name
                        def branchName = env.GIT_BRANCH.replace('/', '-')
                        customImage.push(branchName)
                        
                        // Save image info
                        sh """
                            echo "Built Image: ${DOCKER_IMAGE}" > docker-image-info.txt
                            echo "Tags: latest, ${branchName}" >> docker-image-info.txt
                            docker images | grep "${IMAGE_NAME}" >> docker-image-info.txt
                        """
                        archiveArtifacts artifacts: 'docker-image-info.txt'
                    }
                }
            }
        }
        
        stage('Test Docker Image') {
            steps {
                script {
                    echo "Testing Docker image..."
                    
                    docker.image("${DOCKER_IMAGE}").withRun('-p 8081:8080 --name test-container') { c ->
                        sh """
                            echo "Container ID: ${c.id}"
                            
                            # Wait for container to start
                            sleep 10
                            
                            # Check container status
                            docker ps --filter "id=${c.id}"
                            
                            # Health check
                            max_attempts=10
                            for i in \$(seq 1 \$max_attempts); do
                                if curl -s http://localhost:8081/actuator/health 2>&1 | grep -q '"status":"UP"'; then
                                    echo "✅ Container is healthy!"
                                    break
                                fi
                                echo "⏳ Waiting for container... (attempt \$i/\$max_attempts)"
                                sleep 5
                            done
                            
                            # Get logs
                            echo "=== Container Logs ==="
                            docker logs ${c.id} --tail 20
                        """
                    }
                    
                    echo "✅ Image test completed"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Pushing to Docker Hub..."
                    
                    docker.withRegistry("https://${REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
                        def customImage = docker.image("${DOCKER_IMAGE}")
                        
                        // Push both tags
                        customImage.push()
                        customImage.push('latest')
                        
                        echo "✅ Pushed: ${DOCKER_IMAGE}"
                        echo "✅ Pushed: ${DOCKER_USER}/${IMAGE_NAME}:latest"
                    }
                }
            }
        }
        
        stage('Deploy to Local Docker') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Deploying application..."
                    
                    // Stop and remove existing container
                    sh """
                        docker stop ${CONTAINER_NAME} 2>/dev/null || true
                        docker rm ${CONTAINER_NAME} 2>/dev/null || true
                        docker network create audit-network 2>/dev/null || true
                    """
                    
                    // Run new container using docker.image
                    docker.image("${DOCKER_USER}/${IMAGE_NAME}:latest").run(
                        "--name ${CONTAINER_NAME} " +
                        "-p 8080:8080 " +
                        "--network audit-network " +
                        "--restart unless-stopped " +
                        "-d"
                    )
                    
                    // Health check
                    sh '''
                        echo "Waiting for deployment..."
                        sleep 15
                        
                        # Health check with retry
                        for i in {1..10}; do
                            if curl -s --fail http://localhost:8080/actuator/health >/dev/null 2>&1; then
                                echo "✅ Deployment successful!"
                                echo "Application URL: http://localhost:8080"
                                break
                            fi
                            echo "⏳ Checking deployment... (attempt $i/10)"
                            sleep 5
                        done
                        
                        # Show container info
                        echo "=== Deployment Info ==="
                        docker ps --filter "name=${CONTAINER_NAME}"
                        docker logs ${CONTAINER_NAME} --tail 10
                    '''
                }
            }
        }
        
        stage('Integration Tests') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "Running integration tests..."
                    sh '''
                        # Test the deployed application
                        if curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
                            echo "✅ Integration test passed"
                        else
                            echo "❌ Integration test failed"
                            exit 1
                        fi
                    '''
                }
            }
        }
    }
    
    post {
        always {
            echo "Pipeline ${currentBuild.currentResult}"
            script {
                // Cleanup test containers
                sh '''
                    echo "=== Cleaning up ==="
                    docker stop test-container 2>/dev/null || true
                    docker rm test-container 2>/dev/null || true
                    docker container prune -f 2>/dev/null || true
                '''
                
                // Archive artifacts
                archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true
                junit '**/target/surefire-reports/**/*.xml'
                
                // Save Docker logs
                sh '''
                    if docker ps -a | grep -q ${CONTAINER_NAME}; then
                        docker logs ${CONTAINER_NAME} > ${CONTAINER_NAME}-logs.txt 2>&1
                    fi
                '''
                archiveArtifacts artifacts: '*-logs.txt', allowEmptyArchive: true
            }
        }
        success {
            script {
                echo "🎉 Pipeline succeeded!"
                // Optional: Send notification
                // emailext body: "Build ${BUILD_NUMBER} completed successfully!\n\n${BUILD_URL}", subject: "Pipeline Success: ${JOB_NAME}", to: 'team@example.com'
            }
        }
        failure {
            script {
                echo "❌ Pipeline failed!"
                sh '''
                    echo "=== Debug Information ==="
                    echo "Docker containers:"
                    docker ps -a 2>/dev/null || echo "Docker not available"
                    echo "Docker images:"
                    docker images 2>/dev/null || echo "Docker not available"
                    echo "System info:"
                    df -h 2>/dev/null || echo "df not available"
                    free -h 2>/dev/null || echo "free not available"
                '''
            }
        }
        cleanup {
            // Clean workspace to save disk space
            cleanWs(cleanWhenNotBuilt: false, deleteDirs: true)
        }
    }
}