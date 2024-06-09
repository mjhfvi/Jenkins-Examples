pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
        string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
        booleanParam(defaultValue: true, name: 'USE_STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
        booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
        booleanParam(defaultValue: true, name: 'USE_STAGE_DOCKER_CVE_SCAN', description: 'Test Security Vulnerability Exploits For Docker Image')
        booleanParam(defaultValue: true, name: 'USE_STAGE_CODE_CHECKS', description: 'Check Code for Security Vulnerabilities')
        // string(name: 'BUILD_NUMBER', value: "BUILD_NUMBER")
        // string(name: 'RUN_JOB_NODE_NAME', description: 'set node')

    }

    environment {
        GIT_REPOSITORY_URL          = "$params.SET_GIT_REPOSITORY_URL"
        GIT_BRANCH_NAME             = "main"
        // DOCKER_FILE                 = "dockerfile"
        // DOCKER_REPOSITORY           = "mjhfvi/demo-project"
        DOCKER_REPOSITORY_TAG       = "$params.DOCKER_REPOSITORY_TAG"
        // DOCKER_BUILD_FOLDER         = "develop-project/home-task-app-main"
        DOCKER_BUILD_USE_CACHE      = "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"
        // DOCKER_FROM_IMAGE           = "python:3.9.18-slim"                      // Change Only if you need to Overwrite Default Image in dockerfile
        STAGE_BUILD_DOCKER_IMAGE    = "$params.USE_STAGE_BUILD_DOCKER_IMAGE"    // "false"
        STAGE_PUSH_DOCKER_IMAGE     = "$params.USE_STAGE_PUSH_DOCKER_IMAGE"     //"false"
        STAGE_DOCKER_CVE_SCAN       = "$params.USE_STAGE_DOCKER_CVE_SCAN"        // "false"
        STAGE_CODE_VALIDATION       = "$params.USE_STAGE_CODE_VALIDATION"        // "false"
        STAGE_SECRET_LEAKS          = "$params.USE_STAGE_SECRET_LEAKS"        // "false"
        STAGE_CODE_CHECKS           = "$params.USE_STAGE_CODE_CHECKS"        // "false"
        // BUILD_NUMBER                = "$params.BUILD_NUMBER"
        // RUN_JOB_NODE_NAME           = "$params.RUN_JOB_NODE_NAME"
        // ARTIFACTORY_SERVER       = "http://localhost:8082"
        RUN_JOB_NODE_NAME           = "$params.RUN_JOB_NODE_NAME"
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    // triggers {
    //     githubPush( // Trigger that runs jobs on push notifications from GitHub.
    //         // branchFilter: 'main',           // Optional, specify branches to trigger on
    //         // triggersByBranch: true          // Trigger separate builds for each branch
    //     )
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
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            checkout([$class: 'GitSCM', branches: [[name: "${env.GIT_BRANCH_NAME}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GitHub-SSH-Access-Credentials-to-JobAssignment-Git', url: "git@github.com:mjhfvi/${env.GIT_REPOSITORY_URL}.git"]]])

                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {
                failure {
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        StageGitClone = 'FAILURE'
                    }
                }
                success {
                    script{
                        StageGitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Code Checks') { when { expression { env.STAGE_CODE_CHECKS.toBoolean() && StageGitClone == 'SUCCESS' } }
            parallel{
                stage ('Code Validation') { when { expression { env.STAGE_SECRET_LEAKS.toBoolean() } }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    // def RUN_JOB_NODE_NAME = "${NODE_NAME}"
                                    echo "Passing Working Node to Downstream: ${NODE_NAME}"
                                    echo "Passing Build Number to Downstream: ${params.JOB_BUILD_NUMBER}"

                                    def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Code_Validation',
                                        parameters: [
                                            booleanParam(name: 'USE_STAGE_CODE_VALIDATION', value: "$params.USE_STAGE_CODE_VALIDATION"),
                                            string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                            string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}")
                                        ]
                                    echo "Templates_Pipeline_Code_Validation job result: ${downstreamJob.result}"
                                    } catch (ERROR) {
                                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                        currentBuild.result = 'FAILURE'
                                    } finally {
                                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                                    }
                                }
                            }
                        }
                    }

                stage ('Secret Leaks') { when { expression { env.STAGE_SECRET_LEAKS.toBoolean() } }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                                    // def RUN_JOB_NODE_NAME = "${NODE_NAME}"
                                    echo "Passing Working Node to Downstream: ${env.NODE_NAME}"
                                    echo "Passing Build Number to Downstream: ${params.JOB_BUILD_NUMBER}"

                                    // def CHANGE_BUILD_NUMBER = "${params.JOB_BUILD_NUMBER}"
                                    // currentBuild.displayName = "#${CHANGE_BUILD_NUMBER}"

                                    def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Secret_Leaks',
                                        parameters: [
                                            // string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                                            string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                                            string(name: 'DOCKER_BUILD_IMAGE', value: "$env.DOCKER_BUILD_IMAGE"),
                                            // booleanParam(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                                            // booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                                            // booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                                            booleanParam(name: 'USE_STAGE_SECRET_LEAKS', value: "$params.USE_STAGE_SECRET_LEAKS"),
                                            string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                            string(name: 'CHANGE_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}")
                                        ]
                                    echo "Templates_Pipeline_Secret_Leaks job result: ${downstreamJob.result}"
                                    } catch (ERROR) {
                                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                        currentBuild.result = 'FAILURE'
                                    } finally {
                                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                                    }
                                }
                            }
                        }
                    }
                }
            post {
                failure {
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        StageGitClone = 'FAILURE'
                    }
                }
                success {
                    script{
                        StageGitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Build Docker Image') { when { expression { env.STAGE_BUILD_DOCKER_IMAGE.toBoolean() && StageGitClone == 'SUCCESS' } }
                steps {
                    timeout(activity: true, time: 10, unit: 'MINUTES') {
                        script { // https://docs.docker.com/config/containers/resource_constraints/
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                load "$WORKSPACE/jenkins/DOCKER_BUILD_INFORMATION.txt"

                                if (env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE == null))
                                    dir("${env.DOCKER_BUILD_FOLDER}") {
                                        DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --memory=100m .")
                                    }
                                if (env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE != null))
                                    dir("${env.DOCKER_BUILD_FOLDER}") {
                                        DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --build-arg BASE_OS_VERSION=${env.DOCKER_FROM_IMAGE} --memory=100m .")
                                    }
                                if (!env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE == null))
                                    dir("${env.DOCKER_BUILD_FOLDER}") {
                                        DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --no-cache --memory=100m .")
                                    }
                                if (!env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE != null))
                                    dir("${env.DOCKER_BUILD_FOLDER}") {
                                        DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --build-arg BASE_OS_VERSION=${env.DOCKER_FROM_IMAGE} --no-cache --memory=100m .")
                                    }
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                }
                post {
                    failure {
                        script{
                            echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                            // BuildDockerImage = 'FAILURE'
                        }
                    }
                    success {
                        script{
                            echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                            // BuildDockerImage = 'SUCCESS'
                        }
                    }
                }
        }

        stage('Push Docker Image') { when { expression { env.STAGE_PUSH_DOCKER_IMAGE.toBoolean() && BuildDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 20, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            script {
                                DOCKER_BUILD_IMAGE.push()
                                }
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {
                failure {
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        TestDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TestDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Docker CVE Tests') { when { expression { env.STAGE_DOCKER_CVE_SCAN.toBoolean() } }
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script{
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo "Passing Working Node to Downstream: ${NODE_NAME}"

                            def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_CVE',
                                parameters: [
                                    // string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                                    string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                                    string(name: 'DOCKER_BUILD_IMAGE', value: "$env.DOCKER_BUILD_IMAGE"),
                                    booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "$params.USE_STAGE_CVE_TESTS"),
                                    string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                    string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}")
                                ]
                            echo "Templates_Pipeline_Docker_CVE job result: ${downstreamJob.result}"
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
        }

        // stage ('Artifactory Publish Build Info') { when { expression { env.STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO.toBoolean() } }
        //     agent { label "${env.RUN_JOB_NODE_NAME}" }
        //     steps {
        //         script{
        //             rtBuildInfo (
        //                 captureEnv: true,
        //                 // Optional - Build name and build number. If not set, the Jenkins job's build name and build number are used.
        //                 buildName: 'my-build',
        //                 buildNumber: '20',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'my-project-key'
        //             )
        //         }
        //     }
        // }

        // stage ('Artifactory configuration') { when { expression { env.STAGE_PUSH_IMAGE_TO_ARTIFACTORY.toBoolean() } }
        //     agent { label "${env.RUN_JOB_NODE_NAME}" }
        //     steps {
        //         script{
        //             rtServer (
        //                 id: 'Artifactory-1',
        //                 url: 'http://localhost:8082/artifactory',
        //                     // If you're using username and password:
        //                 username: 'admin',
        //                 password: '',
        //                     // If you're using Credentials ID:
        //                     // credentialsId: 'ccrreeddeennttiiaall',
        //                     // If Jenkins is configured to use an http proxy, you can bypass the proxy when using this Artifactory server:
        //                     bypassProxy: false,
        //                     // Configure the connection timeout (in seconds).
        //                     // The default value (if not configured) is 300 seconds:
        //                     timeout: 300
        //             )

        //             rtDownload (
        //                 serverId: 'Artifactory-1',
        //                 spec: '''{
        //                       "files": [
        //                         {
        //                           "pattern": "generic-local/",
        //                           "target": "generic-local/"
        //                         }
        //                       ]
        //                 }''',

        //                 // Optional - Associate the downloaded files with the following custom build name and build number,
        //                 // as build dependencies.
        //                 // If not set, the files will be associated with the default build name and build number (i.e the
        //                 // the Jenkins job name and number).
        //                 buildName: 'holyFrog',
        //                 buildNumber: '42',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'myproject'
        //             )

        //             rtUpload (
        //                 serverId: 'Artifactory-1',
        //                 spec: '''{
        //                       "files": [
        //                         {
        //                           "pattern": "dockerfile",
        //                           "target": "generic-local/"
        //                         }
        //                      ]
        //                 }''',

        //                 // Optional - Associate the uploaded files with the following custom build name and build number,
        //                 // as build artifacts.
        //                 // If not set, the files will be associated with the default build name and build number (i.e the
        //                 // the Jenkins job name and number).
        //                 buildName: 'holyFrog',
        //                 buildNumber: '42',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'myproject'
        //             )
        //         }
        //     }
        // }
        stage ('Update Build Info') {
            // agent { label "${env.RUN_JOB_NODE_NAME}" }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        echo "Upstream Build Number is: ${params.JOB_BUILD_NUMBER}"
                        echo "Changing Build Number to Upstream Number: ${params.JOB_BUILD_NUMBER}"
                        currentBuild.displayName = "#${params.JOB_BUILD_NUMBER}"

                    } catch (ERROR) {
                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                        currentBuild.result = 'FAILURE'
                    } finally {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                    }
                }
            }
        }
    }

// Post Condition Blocks: always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, not_built, cleanup
    post {
        aborted {
            echo "Pipeline is Aborted, Sending Email Notifications"
            addBadge(icon: "delete.gif", text: "Build Aborted")
        }
        unstable {
            echo "Pipeline is Unstable, Sending Email Notifications"
            addBadge(icon: "warning.gif", text: "Build Unstable")
        }
        failure {
            echo "Pipeline is a Failure, Sending Email Notifications"
            addBadge(icon: "error.gif", text: "Build Failure")
        }
        success {
            echo "Pipeline is Successfully, Sending Email Notifications"
            addBadge(icon: "success.gif", text: "Build Success")
        }
        always {
            echo "Pipeline is Done, Running Always Post Condition"
            // cleanWs() // Clean workspace after each run
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
