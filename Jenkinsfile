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
            echo 'Running tests...'
            sh 'mvn test -Dspring.test.skip=true -Dmaven.test.failure.ignore=true'
        }
    }
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
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
