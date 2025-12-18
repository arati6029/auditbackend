pipeline {
    agent any
    
    options {
        timestamps()
    }
    
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
        // Force Docker to use Windows containers
        DOCKER_CLI_EXPERIMENTAL = "enabled"
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
                    // Check if running on Windows
                    def isWindows = isUnix() ? false : true
                    
                    if (isWindows) {
                        // On Windows, ensure Docker Desktop is running with Windows containers
                        def dockerInfo = bat(script: 'docker info', returnStatus: true)
                        if (dockerInfo != 0) {
                            error('❌ Docker is not running. Please start Docker Desktop and ensure it is using Windows containers.')
                        }
                        
                        // Set Docker to use Windows containers
                        bat 'docker version'
                        bat 'docker ps'  // This will help verify Docker is working
                    } else {
                        // For Linux/Unix
                        def dockerAvailable = sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) == 0
                        if (!dockerAvailable) {
                            error('❌ Docker is not available. Please ensure Docker is installed and in the PATH.')
                        }
                        sh 'docker --version'
                        sh 'docker ps'
                    }
                }
            }
        }
        
        stage('Build Application') {
            steps {
                echo '🔨 Starting Build: Compiling application...'
                script {
                    if (isUnix()) {
                        sh 'mvn clean package -DskipTests'
                    } else {
                        bat 'mvn clean package -DskipTests'
                    }
                }
                echo '✅ Build completed successfully'
            }
        }
        
        stage('Build and Push Docker Image') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    // For Windows, ensure the Dockerfile is configured for Windows containers
                    def buildContext = "."
                    if (isUnix()) {
                        sh "docker build -t ${DOCKER_IMAGE} ${buildContext}"
                    } else {
                        bat "docker build -t ${DOCKER_IMAGE} ${buildContext}"
                    }
                    
                    // Tag for latest
                    if (isUnix()) {
                        sh "docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${IMAGE_NAME}:latest"
                    } else {
                        bat "docker tag ${DOCKER_IMAGE} ${DOCKER_USER}/${IMAGE_NAME}:latest"
                    }
                    
                    // Login and push
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )]) {
                        if (isUnix()) {
                            sh "echo $DOCKER_PASSWORD | docker login -u $DOCKER_USERNAME --password-stdin"
                            sh "docker push ${DOCKER_IMAGE}"
                            sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:latest"
                        } else {
                            bat "echo %DOCKER_PASSWORD% | docker login -u %DOCKER_USERNAME% --password-stdin"
                            bat "docker push ${DOCKER_IMAGE}"
                            bat "docker push ${DOCKER_USER}/${IMAGE_NAME}:latest"
                        }
                    }
                }
                echo '✅ Docker image built and pushed successfully'
            }
        }
        
        stage('Run Container') {
            steps {
                echo '🚀 Starting Docker container...'
                script {
                    def runCmd = "--name ${CONTAINER_NAME} -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod"
                    
                    // Stop and remove any existing container
                    if (isUnix()) {
                        sh "docker stop ${CONTAINER_NAME} || true"
                        sh "docker rm ${CONTAINER_NAME} || true"
                        sh "docker run -d ${runCmd} ${DOCKER_IMAGE}"
                    } else {
                        bat "docker stop ${CONTAINER_NAME} || exit 0"
                        bat "docker rm ${CONTAINER_NAME} || exit 0"
                        bat "docker run -d ${runCmd} ${DOCKER_IMAGE}"
                    }
                    
                    // Health check
                    echo '🩺 Checking application health...'
                    def healthCheck = isUnix() ? 
                        """
                        max_attempts=10
                        for i in \$(seq 1 \$max_attempts); do
                            if curl -s --fail http://localhost:8080/actuator/health >/dev/null 2>&1; then
                                echo "✅ Application is healthy!"
                                exit 0
                            fi
                            echo "⏳ Waiting for application to start... (attempt \$i/\$max_attempts)"
                            sleep 5
                        done
                        echo "❌ Application failed to start within the expected time"
                        exit 1
                        """ :
                        """
                        @echo off
                        set max_attempts=10
                        set attempt=1
                        :retry
                        curl -s --fail http://localhost:8080/actuator/health >nul 2>&1
                        if %ERRORLEVEL% EQU 0 (
                            echo ✅ Application is healthy!
                            exit /b 0
                        )
                        echo ⏳ Waiting for application to start... (attempt %attempt%/%max_attempts%)
                        if %attempt% GEQ %max_attempts% (
                            echo ❌ Application failed to start within the expected time
                            exit /b 1
                        )
                        set /a attempt+=1
                        timeout /t 5 >nul
                        goto :retry
                        """
                    
                    if (isUnix()) {
                        sh healthCheck
                    } else {
                        bat healthCheck
                    }
                }
                echo '🚀 Container started successfully'
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