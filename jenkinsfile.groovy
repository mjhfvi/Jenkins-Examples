pipeline {
    agent any

    environment {
        GIT_REPO_URL    = "github.com/mjhfvi/DockerExamples.git"
        DOCKER_FILE     = dockerfile.nginx
    }

    parameters {
        // string ( name: 'BRANCH_TO_RUN', defaultValue: 'dev', description: 'Name of Branch to run', trim: false )
        choice ( name: 'BRANCH_TO_RUN', choices: ['dev', 'Releases/1.0'], description: 'Select a Branch to run')
        // string ( name: 'DOCKER_IMAGE_NODE_VERSION', defaultValue: '16.16.0', description: 'Node Version to Run in Docker.', trim: false )
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
        // retry(3)
    }

    triggers {
        GenericTrigger (
            genericVariables: [ [key: 'ref', value: '$.ref'] ],
            token: 'pipeline_token',
            causeString: 'Triggered by $.actor.displayName for BitBucket Project $ref from $branch',
            printContributedVariables: true,
            printPostContent: true
        )
    }

    stages {
        stage('Git Clone') {
            steps {
                git branch: 'main', credentialsId: 'github-credentials', url: https://${git_repo_url}
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t <repository-name>:<image-tag> -f ${docker_file}' // Adjust repository and tag
            }
        }

        stage('Push Docker Image') {
            steps {
                sh 'docker login -u <artifactory-username> -p <artifactory-token> <artifactory-url>'
                sh 'docker push <repository-name>:<image-tag>'
            }
        }

        stage('SonarQube Security Analysis') {
            steps {
                // Assuming SonarQube Scanner plugin is installed
                sonarqubeScanner serverUrl: '<sonarqube-url>', token: '<sonarqube-token>', analysisMode: 'JVM', tasks: ['sonar:qualitygate:fail'] // Configure settings
            }
        }

        stage('Post-Processing (Optional)') {
            when {
                expression {
                    // Add conditions for running this stage based on security analysis results or other criteria
                    return true // Replace with your logic
                }
            }
            steps {
                // Additional steps like sending notifications, deploying the image, etc.
            }
        }
    }

    post {
        always {
            cleanWs() // Clean workspace after each run
        }
    }
}
