pipeline {
    agent any
    
    tools {
        maven 'M3'
    }
    
    stages {
        stage('Checkout') {
            steps {
                // This will checkout from the configured SCM
                checkout scm
                echo '✅ Repository checked out'
            }
        }
        
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                archiveArtifacts 'target/*.jar'
                echo '✅ Build completed'
            }
        }
        
        stage('Deploy') {
            steps {
                sh '''
                    # Stop any existing containers
                    docker stop audit-app mysql-db 2>/dev/null || true
                    docker rm audit-app mysql-db 2>/dev/null || true
                    
                    # Build Docker image
                    docker build -t audit-app:latest .
                    
                    # Start MySQL
                    docker run -d --name mysql-db \
                      -e MYSQL_ROOT_PASSWORD=root \
                      -e MYSQL_DATABASE=auditDb \
                      -p 3307:3306 \
                      mysql:8.0
                    
                    echo "Waiting for MySQL..."
                    sleep 30
                    
                    # Start Application
                    docker run -d --name audit-app \
                      -p 8081:8080 \
                      -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/auditDb \
                      -e SPRING_DATASOURCE_USERNAME=root \
                      -e SPRING_DATASOURCE_PASSWORD=root \
                      audit-app:latest
                    
                    sleep 20
                    
                    # Verify
                    curl -f http://localhost:8081/actuator/health && echo "✅ Deployed!" || docker logs audit-app
                '''
            }
        }
    }
    
    post {
        success {
            echo '🎉 Success! App: http://localhost:8081'
        }
        failure {
            echo '❌ Failed! Check logs above.'
        }
    }
}