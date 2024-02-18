pipeline {
    agent { label 'windows' }

    environment {
        GIT_REPO_URL    = 'https://github.com/mjhfvi/DockerExamples.git'
        DOCKER_FILE     = 'dockerfile.nginx'
        STAGE_GIT_CLONE = 'true'
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

        stage('Shared Libraries') {
            echo helloWorld.sayHello('World')
        }

        stage('Git Clone') { when { expression { env.STAGE_GIT_CLONE.toBoolean() } } } {// && WindowsBuildStage == 'SUCCESS'
            currentBuild.description = 'GitClone'
            steps {
                git branch: 'main', credentialsId: 'GitHub-Access-Credentials', url: '${env.GIT_REPO_URL}'
            }
        }

    //     stage('Build Docker Image') {
    //         steps {
    //             timeout(time: 2, unit: 'MINUTES') {
    //                 print("Docker Image Building for Windows")
    //                 sh 'docker build -t <repository-name>:<image-tag> -f ${docker_file}' // Adjust repository and tag
    //             }
    //         }
    //     }

    //     stage('Push Docker Image') {
    //         steps {
    //             timeout(time: 2, unit: 'MINUTES') {
    //                 sh 'docker login -u <artifactory-username> -p <artifactory-token> <artifactory-url>'
    //                 sh 'docker push <repository-name>:<image-tag>'
    //             }
    //         }
    //     }

    //     stage('SonarQube Security Analysis') {
    //         steps {
    //             // Assuming SonarQube Scanner plugin is installed
    //             sonarqubeScanner serverUrl: '<sonarqube-url>', token: '<sonarqube-token>', analysisMode: 'JVM', tasks: ['sonar:qualitygate:fail'] // Configure settings
    //         }
    //     }

    //     stage('Post-Processing (Optional)') {
    //         when {
    //             expression {
    //                 // Add conditions for running this stage based on security analysis results or other criteria
    //                 return true // Replace with your logic
    //             }
    //         }
    //         steps {
    //             // Additional steps like sending notifications, deploying the image, etc.
    //         }
    //     }
    // }

        post {
            always {
                cleanWs() // Clean workspace after each run
            }
        }
    }
}
