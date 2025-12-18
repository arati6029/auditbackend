pipeline {
    agent any
    
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
                sh 'docker-compose build'
                echo '✅ Docker image built successfully'
            }
        }
        
        stage('Run Containers') {
            steps {
                sh '''
                    # Stop any existing containers
                    docker-compose down 2>/dev/null || true
                    
                    # Start containers
                    docker-compose up -d
                    
                    echo "Waiting for services to start..."
                    sleep 30
                '''
                echo '✅ Containers started successfully'
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Checking if application is running..."
                    
                    # Check if containers are running
                    docker-compose ps
                    
                    # Check application health
                    if curl -f http://localhost:8081/actuator/health 2>/dev/null; then
                        echo "✅ Application is healthy!"
                    else
                        echo "❌ Application health check failed"
                        docker-compose logs app
                        exit 1
                    fi
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
                echo "To view logs: docker-compose logs -f"
                echo "To stop: docker-compose down"
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