pipeline {
    agent any
    
    tools {
        maven 'M3'
    }
    
    // Remove or override DOCKER_HOST environment variable
    environment {
        DOCKER_HOST = 'unix:///var/run/docker.sock'
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
                echo '✅ Code checked out successfully'
            }
        }
        
        stage('Build Spring Boot App') {
            steps {
                sh 'mvn clean package -DskipTests'
                echo '✅ Spring Boot application built successfully'
            }
            post {
                success {
                    archiveArtifacts 'target/*.jar'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                sh '''
                    # Use docker build instead of docker-compose build
                    docker build -t audit-application:latest .
                    echo "✅ Docker image built successfully"
                '''
            }
        }
        
        stage('Stop Existing Containers') {
            steps {
                sh '''
                    # Stop and remove existing containers
                    docker stop audit-application mysql-db 2>/dev/null || true
                    docker rm audit-application mysql-db 2>/dev/null || true
                    
                    # Remove network if exists
                    docker network rm audit-network 2>/dev/null || true
                '''
                echo '✅ Cleaned up existing containers'
            }
        }
        
        stage('Run Containers') {
            steps {
                sh '''
                    # Create network
                    docker network create audit-network 2>/dev/null || true
                    
                    # Run MySQL container
                    docker run -d \
                      --name mysql-db \
                      --network audit-network \
                      -e MYSQL_ROOT_PASSWORD=root \
                      -e MYSQL_DATABASE=auditDb \
                      -p 3307:3306 \
                      -v mysql-data:/var/lib/mysql \
                      mysql:8.0
                    
                    echo "✅ MySQL container started"
                    
                    # Wait for MySQL to be ready
                    sleep 20
                    
                    # Run Spring Boot application container
                    docker run -d \
                      --name audit-application \
                      --network audit-network \
                      -p 8081:8080 \
                      -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/auditDb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
                      -e SPRING_DATASOURCE_USERNAME=root \
                      -e SPRING_DATASOURCE_PASSWORD=root \
                      -e JWT_SECRET_KEY=bXlTdXBlclNlY3JldEtleTEyMzQ1Njc4OTAxMjM0NTY3OA== \
                      -e JWT_EXPIRATION_TIME=3600000 \
                      audit-application:latest
                    
                    echo "✅ Application container started"
                    sleep 20
                '''
                echo '✅ Containers started successfully'
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Checking if containers are running..."
                    docker ps --filter "name=audit"
                    
                    echo "Checking application health..."
                    max_attempts=10
                    attempt=1
                    
                    while [ $attempt -le $max_attempts ]; do
                        if curl -f http://localhost:8081/actuator/health 2>/dev/null; then
                            echo "✅ Application is healthy!"
                            exit 0
                        fi
                        
                        echo "Attempt $attempt/$max_attempts: Waiting for application..."
                        sleep 10
                        attempt=$((attempt+1))
                    done
                    
                    echo "❌ Application health check failed"
                    docker logs audit-application
                    exit 1
                '''
                echo '✅ Deployment verified successfully'
            }
        }
    }
    
    post {
        always {
            echo "=== Deployment Summary ==="
            sh '''
                echo "Application URL: http://localhost:8081"
                echo "MySQL Port: 3307"
                echo ""
                echo "To view application logs: docker logs audit-application"
                echo "To view database logs: docker logs mysql-db"
                echo "To stop all: docker stop audit-application mysql-db"
            '''
            cleanWs()
        }
        
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}