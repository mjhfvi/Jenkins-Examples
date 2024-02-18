// @Library('Jenkins-Shared-Library') _

pipeline {
    agent { label 'windows' }

    environment {
        GIT_REPOSITORY_URL      = "https://github.com/mjhfvi/DockerExamples.git"
        DOCKER_FILE             = 'dockerfile.nginx'
        DOCKER_REPOSITORY       = 'mjhfvi/demo'
        STAGE_BUILD_DOCKER_IMAGE         = 'true'

    }

    // parameters {
    //     // string ( name: 'BRANCH_TO_RUN', defaultValue: 'dev', description: 'Name of Branch to run', trim: false )
    //     // choice ( name: 'BRANCH_TO_RUN', choices: ['dev', 'Releases/1.0'], description: 'Select a Branch to run')
    //     // string ( name: 'DOCKER_IMAGE_NODE_VERSION', defaultValue: '16.16.0', description: 'Node Version to Run in Docker.', trim: false )
    // }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
        // retry(3)
    }

    // triggers {
    //     GenericTrigger (
    //         genericVariables: [ [key: 'ref', value: '$.ref'] ],
    //         token: 'pipeline_token',
    //         causeString: 'Triggered by $.actor.displayName for BitBucket Project $ref from $branch',
    //         printContributedVariables: true,
    //         printPostContent: true
    //     )
    // }

    stages {

        // stage('Shared Libraries') {
        //     steps{
        //         // echo HelloWorld.sayHello('World')
        //         HelloWorld()
        //     }
        // }

        stage('Git_Clone') {
            steps {
                timeout(activity: true, time: 5) {
                    try {
                        git branch: 'main', credentialsId: 'GitHub-Access-Credentials', url: "${env.GIT_REPOSITORY_URL}"
                    } catch (error) {
                        echo "Failed: ${error}"
                    } finally {
                        print ("Step Git Clone Done")
                    }
                }
            }
        }
// currentStage.getCurrentResult() == "UNSTABLE"    stageResults.Git_Clone = "SUCCESS"
        stage('Build Docker Image') { when { expression { env.STAGE_BUILD_DOCKER_IMAGE.toBoolean() && stageResults.Git_Clone == 'SUCCESS' } }
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    print("Docker Image Building for Windows")
                    sh 'docker build -t ${ENV.DOCKER_REPOSITORY} -f ${env.DOCKER_FILE}'
                }
            }
        }

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

    }

    post {      //  fixed, regression, unsuccessful, changed, always
        aborted {
            print("The Build is Aborted, Sending Email Notifications")
            script{
                print("Sending Aborted Notifications")
                // manager.addShortText("Build Aborted")
            }
        }
        unstable {
            print("The Build is Unstable, Sending Notifications")
            script{
                print("Sending Unstable Notifications")
                // manager.addShortText("Build Unstable")
            }
        }
        failure {
            print("The Build is a Failure, Sending Notifications")
            script{
                print("Sending Failure Notifications")
                // manager.addShortText("Build Failure")
            }
        }
        success {                           // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
            print("The Build is Successfully, Sending Notifications")
            script{
                print("Sending Successfully Notifications")
                // manager.addShortText("Build Successfully")
            }
        }
        always {
            print("Running Always Post")
            cleanWs() // Clean workspace after each run
        }
    }
}
