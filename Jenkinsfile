pipeline {
    agent any
    tools {
        // Make sure Maven is configured in Jenkins Global Tool Configuration
        maven 'M3'  // This should match the Maven installation name in Jenkins
    }
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Build your application
                    echo 'Building the application...'
                    // Example: sh 'mvn clean package' for Maven
                    // or sh 'npm install' for Node.js
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    // Run tests
                    echo 'Running tests...'
                    // Example: sh 'mvn test' or 'npm test'
                     sh '''
    mvn clean test -Dspring.profiles.active=test \
        -Dspring.datasource.url=jdbc:h2:mem:testdb \
        -Dtestcontainers.enabled=false
'''
                }
            }
            post {
                always {
                    // Archive test results
                    junit '**/target/surefire-reports/*.xml' // Update path based on your test reports
                    archiveArtifacts '**/target/surefire-reports/**'
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                expression { env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' }
            }
            steps {
                script {
                    // Build Docker image
                    echo 'Building Docker image...'
                    // sh "docker build -t ${DOCKER_IMAGE} ."
                }
            }
        }
        
        stage('Deploy') {
            when {
                expression { env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' }
            }
            steps {
                script {
                    // Deploy your application
                    echo 'Deploying application...'
                    // Add your deployment steps here
                }
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
            // Send notification on failure
            // mail to: 'team@example.com', subject: "Failed Pipeline: ${currentBuild.fullDisplayName}", body: "Check the pipeline run at ${env.BUILD_URL}"
        }
    }
}
