// @Library('Jenkins-Shared-Library') _

pipeline {
    agent { label 'windows' }

    environment {
        GIT_REPOSITORY_URL                  = "https://github.com/mjhfvi/DockerExamples.git"
        DOCKER_FILE                         = "dockerfile.nginx"
        DOCKER_REPOSITORY                   = "mjhfvi/demo"
        STAGE_BUILD_DOCKER_IMAGE            = "true"
        STAGE_PUSH_IMAGE_TO_ARTIFACTORY     = "false"
        STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO  = "false"
        ARTIFACTORY_SERVER                  = "192.168.50.50:8082"

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
        stage('Git Clone') {
            steps {
                timeout(activity: true, time: 5) {
                    script {
                        try {
                            git branch: 'main', credentialsId: 'GitHub-Access-Credentials', url: "${env.GIT_REPOSITORY_URL}"
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97mStep ${env.STAGE_NAME} Done\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is a Failure, Sending Notifications"
                        StageGitClone = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is Successfully, Sending Notifications"
                        StageGitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Build Docker Image') { when { expression { env.STAGE_BUILD_DOCKER_IMAGE.toBoolean() && StageGitClone == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    print("Docker Image Building")
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            docker.build("${ARTIFACTORY_SERVER}" + "/${env.DOCKER_REPOSITORY}", "-f ${env.DOCKER_FILE} --memory=100m .")
                        // docker.build("${env.DOCKER_REPOSITORY}", "-f ${env.DOCKER_FILE} --no-cache --memory=100m .")
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97mStep ${env.STAGE_NAME} Done\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is a Failure, Sending Notifications"
                        BuildDockerImage = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is Successfully, Sending Notifications"
                        BuildDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        // stage ('Artifactory configuration') {
        //     steps {
        //         rtServer (
        //             id: "ARTIFACTORY_SERVER",
        //             url: "http://192.168.50.50:8082",
        //             credentialsId: 'JFrog-Access-Credentials'
        //         )
        //     }
        // }

        stage ('Push Image to Artifactory') { when { expression { env.STAGE_PUSH_IMAGE_TO_ARTIFACTORY.toBoolean() && BuildDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            rtDockerPush(
                                serverId: "ARTIFACTORY_SERVER",     // Obtain an Artifactory server instance, defined in Jenkins --> Manage Jenkins --> Configure System:
                                image: "${ARTIFACTORY_SERVER}" + "/${env.DOCKER_REPOSITORY}",
                                host: "tcp://0.0.0.0:2375",                   // On Linux can be omitted or null
                                targetRepo: 'docker-local',
                                properties: 'project-name=docker1;status=stable'    // Attach custom properties to the published artifacts:
                            )
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"

                            def cmdOutput = "${ERROR}.trim()"
                            if (cmdOutput.contains("ouldn't execute docker task")) {
                            bat ("echo \033[41m\033[97m\033[1mthis is a catch error in jenkins\033[0m")}
                            // else if (cmdOutput.contains("docker build failed with exit code 1")) {
                            // error "Command failed with error : ${ERROR}. Retrying ...."}
                            // bat ("echo \033[41m\033[97m\033[1mthis is a catch error in jenkins\033[0m")
                            currentBuild.result = 'FAILURE'

                        } finally {
                            echo "\033[42m\033[97mStep ${env.STAGE_NAME} Done\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is a Failure, Sending Notifications"
                        PushImagetoArtifactory = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is Successfully, Sending Notifications"
                        PushImagetoArtifactory = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Publish Build Artifactory Info') { when { expression { env.STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO.toBoolean() && PushImagetoArtifactory == 'SUCCESS' } }
            steps {
                script {
                    try {
                        rtPublishBuildInfo (
                            serverId: "ARTIFACTORY_SERVER"
                        )
                    } catch (ERROR) {
                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                    } finally {
                        echo "\033[42m\033[97mStep ${env.STAGE_NAME} Done\033[0m"
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is a Failure, Sending Notifications"
                        PublishBuildArtifactoryInfo = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "The ${env.STAGE_NAME} Build is Successfully, Sending Notifications"
                        PublishBuildArtifactoryInfo = 'SUCCESS'
                    }
                }
            }
        }

        stage('Git Secrets Scan') { // https://github.com/awslabs/git-secrets
            steps {
                script {
                    // Execute Git Secrets scan
                    def secretsOutput = bat(script: 'git secrets --scan -r', returnStdout: true).trim()

                    // Display output in Jenkins console
                    echo "Git Secrets Scan Output:"
                    echo secretsOutput

                    // Save output to a file
                    writeFile file: 'git-secrets-report.txt', text: secretsOutput

                    // Optionally fail the build if secrets are found
                    if (secretsOutput) {
                        error "Secrets found in repository!"
                    }
                }
            }
        }

        stage('Gitleaks Scan') {
            steps {
                script {
                    // Execute Gitleaks scan
                    def gitleaksOutput = bat(script: './gitleaks --repo-path . --report=/tmp/gitleaks-report.json', returnStdout: true).trim()

                    // Display output in Jenkins console
                    echo "Gitleaks Scan Output:"
                    echo gitleaksOutput

                    // Archive the report
                    archiveArtifacts artifacts: '/tmp/gitleaks-report.json', allowEmptyArchive: true
                }
            }
        }

        // stage('GitGuardian Scan') {
        //     agent {
        //         docker { image 'gitguardian/ggshield:latest' }
        //     }
        //     environment {
        //         GITGUARDIAN_API_KEY = credentials('GitGuardian-Access-Credentials')
        //     }
        //     steps {
        //         bat 'ggshield secret scan {ARTIFACTORY_SERVER}" + "/${env.DOCKER_REPOSITORY}'
        //     }
        // }

    //     stage('SonarQube Security Analysis') {
    //         steps {
    //             // Assuming SonarQube Scanner plugin is installed
    //             sonarqubeScanner serverUrl: '<sonarqube-url>', token: '<sonarqube-token>', analysisMode: 'JVM', tasks: ['sonar:qualitygate:fail'] // Configure settings
    //         }
    //     }

    }

    post { //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
        aborted { // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
            echo "The Build is Aborted, Sending Email Notifications"
            addBadge(icon: "delete.gif", text: "Build Aborted")
        }
        unstable { // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
            echo "The Build is Unstable, Sending Notifications"
            addBadge(icon: "warning.gif", text: "Build Unstable")
        }
        failure { // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
            echo "The Build is a Failure, Sending Notifications"
            addBadge(icon: "error.gif", text: "Build Failure")
        }
        success { // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
            echo "The Build is Successfully, Sending Notifications"
            addBadge(icon: "success.gif", text: "Build Success")
        }
        always {
            echo "Running Always Post"
            cleanWs() // Clean workspace after each run
        }
    }
}
