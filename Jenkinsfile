pipeline {
    agent any
    tools {
        maven 'M3'
    }
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
        DOCKER_IMAGE = "${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}"
        DOCKER_REGISTRY = "${REGISTRY}/${DOCKER_IMAGE}"  // For pushing to Docker Hub
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    sh '''
                        mvn test -Dspring.profiles.active=test \
                            -Dspring.datasource.url=jdbc:h2:mem:testdb \
                            -Dspring.datasource.driver-class-name=org.h2.Driver \
                            -Dspring.datasource.username=sa \
                            -Dspring.datasource.password= \
                            -Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
                            -Dtestcontainers.enabled=false
                    '''
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/**/*.xml'  // Archive test results
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh "docker build -t ${DOCKER_REGISTRY} ."
                    sh "docker tag ${DOCKER_REGISTRY} ${DOCKER_USER}/${IMAGE_NAME}:latest"
                }
            }
        }

        stage('Run Docker Container') {
            steps {
                script {
                    // Stop and remove existing container if it exists
                    sh "docker stop ${CONTAINER_NAME} || true"
                    sh "docker rm ${CONTAINER_NAME} || true"
                    
                    // Run the container with environment variables
                    sh """
                        docker run -d \
                            --name ${CONTAINER_NAME} \
                            -p 8080:8080 \
                            -e SPRING_PROFILES_ACTIVE=prod \
                            -e SPRING_DATASOURCE_URL=\${SPRING_DATASOURCE_URL} \
                            -e SPRING_DATASOURCE_USERNAME=\${SPRING_DATASOURCE_USERNAME} \
                            -e SPRING_DATASOURCE_PASSWORD=\${SPRING_DATASOURCE_PASSWORD} \
                            ${DOCKER_REGISTRY}
                    """
                    
                    // Health check
                    sh "sleep 10 && curl --fail http://localhost:8080/actuator/health"
                }
            }
        }
        
        stage('Push to Docker Hub') {
            when {
                branch 'main'
            }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'docker-hub-credentials', 
                        usernameVariable: 'DOCKER_HUB_USER', 
                        passwordVariable: 'DOCKER_HUB_PASSWORD'
                    )]) {
                        sh "echo ${DOCKER_HUB_PASSWORD} | docker login -u ${DOCKER_HUB_USER} --password-stdin ${REGISTRY}"
                        sh "docker push ${DOCKER_REGISTRY}"
                        sh "docker push ${DOCKER_USER}/${IMAGE_NAME}:latest"
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Archive test results and logs
            junit '**/target/surefire-reports/**/*.xml'
            archiveArtifacts '**/target/*.jar'
            
            // Clean up
            sh "docker stop ${CONTAINER_NAME} || true"
            sh "docker rm ${CONTAINER_NAME} || true"
            cleanWs()
        }
        success {
            // Send success notification
            echo 'Pipeline completed successfully!'
        }
        failure {
            // Send failure notification
            echo 'Pipeline failed!'
        }
    }
}