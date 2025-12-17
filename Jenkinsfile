pipeline {
    agent any
    
    environment {
        REGISTRY = "docker.io"
        DOCKER_USER = "arati6029"
        IMAGE_NAME = "auditapplication-app"
        CONTAINER_NAME = "auditapplication"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Setup') {
            steps {
                script {
                    sh '''
                        # Install nvm if not exists
                        if [ ! -d "$HOME/.nvm" ]; then
                            curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
                            export NVM_DIR="$HOME/.nvm"
                            [ -s "$NVM_DIR/nvm.sh" ] && \\. "$NVM_DIR/nvm.sh"
                        else
                            export NVM_DIR="$HOME/.nvm"
                            [ -s "$NVM_DIR/nvm.sh" ] && \\. "$NVM_DIR/nvm.sh"
                        fi
                        
                        # Install and use Node.js 18 (LTS)
                        nvm install 18
                        nvm use 18
                        
                        # Install Angular CLI globally
                        npm install -g @angular/cli
                        
                        # Install project dependencies
                        npm ci
                        
                        # Verify installations
                        node --version
                        npm --version
                        ng version
                    '''
                }
            }
        }
        
        stage('Lint') {
            steps {
                script {
                    sh 'ng lint'
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    sh '''
                        # Configure Karma to generate JUnit XML reports
                        echo 'Configuring test reporting...'
                        
                        # Run tests with JUnit reporter
                        ng test --watch=false --browsers=ChromeHeadless --code-coverage --reporters=junit,progress
                    '''
                }
            }
            post {
                always {
                    // Archive test results - adjust path based on your Angular test output
                    junit '**/test-results.xml'
                }
            }
        }
        
        stage('Build') {
            steps {
                script {
                    sh '''
                        # Clean previous build
                        rm -rf dist/* || true
                        
                        # Build Angular app
                        ng build --configuration=production
                        
                        # Verify build output
                        ls -la dist/
                    '''
                }
            }
        }
        
        stage('Build Docker Image') {
            when {
                expression { env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' }
            }
            steps {
                script {
                    sh '''
                        # Build Docker image
                        echo "Building Docker image ${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER}..."
                        docker build -t ${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER} .
                        docker tag ${DOCKER_USER}/${IMAGE_NAME}:${BUILD_NUMBER} ${DOCKER_USER}/${IMAGE_NAME}:latest
                    '''
                }
            }
        }
        
        stage('Deploy') {
            when {
                expression { env.BRANCH_NAME == 'main' || env.BRANCH_NAME == 'master' }
            }
            steps {
                script {
                    sh '''
                        # Stop and remove old container
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                        
                        # Run new container
                        echo "Deploying container ${CONTAINER_NAME}..."
                        docker run -d \\
                          --name ${CONTAINER_NAME} \\
                          -p 80:80 \\
                          --restart unless-stopped \\
                          ${DOCKER_USER}/${IMAGE_NAME}:latest
                    '''
                }
            }
        }
    }
    
    post {
        always {
            // Archive build artifacts
            archiveArtifacts artifacts: 'dist/**/*', fingerprint: true
            
            // Clean workspace
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}