pipeline {
    agent none

    // parameters {
    //     choice( choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
    //     string( name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
    //     booleanParam( name: 'USE_STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
    //     booleanParam( name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
    //     booleanParam( name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
    //     booleanParam( name: 'USE_STAGE_DOCKER_CVE_SCAN', description: 'Test Security Vulnerability Exploits For Docker Image')
    //     booleanParam( name: 'USE_STAGE_CODE_CHECKS', description: 'Check Code for Security Vulnerabilities')
    // }

    // environment {
    //     GIT_REPOSITORY_URL          = "$params.SET_GIT_REPOSITORY_URL"
    //     GIT_BRANCH_NAME             = "main"
    //     DOCKER_REPOSITORY_TAG       = "$params.DOCKER_REPOSITORY_TAG"
    //     DOCKER_BUILD_USE_CACHE      = "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"
    //     STAGE_BUILD_DOCKER_IMAGE    = "$params.USE_STAGE_BUILD_DOCKER_IMAGE"            // Defaults to "false"
    //     STAGE_PUSH_DOCKER_IMAGE     = "$params.USE_STAGE_PUSH_DOCKER_IMAGE"             // Defaults to "false"
    //     STAGE_DOCKER_CVE_SCAN       = "$params.USE_STAGE_DOCKER_CVE_SCAN"               // Defaults to "false"
    //     STAGE_CODE_VALIDATION       = "$params.USE_STAGE_CODE_VALIDATION"               // Defaults to "false"
    //     STAGE_SECRET_LEAKS          = "$params.USE_STAGE_SECRET_LEAKS"                  // Defaults to "false"
    //     STAGE_CODE_CHECKS           = "$params.USE_STAGE_CODE_CHECKS"                   // Defaults to "false"
    //     // ARTIFACTORY_SERVER       = "http://localhost:8082"
    // }

    options {
        timeout(time: 2, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Deploy To Environment') { when { expression { env.STAGE_BUILD_DOCKER_IMAGE.toBoolean() && StageGitClone == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

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
                        DeployToEnvironment = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        DeployToEnvironment = 'SUCCESS'
                    }
                }
            }
        }

        // stage('Push Docker Image') { when { expression { env.STAGE_PUSH_DOCKER_IMAGE.toBoolean() && BuildDockerImage == 'SUCCESS' } }
        //     agent {
        //         node {
        //             label "${params.RUN_JOB_NODE_NAME}"
        //             customWorkspace "${params.JOB_WORKSPACE}"
        //             }
        //     }
        //     steps {
        //         timeout(activity: true, time: 20, unit: 'MINUTES') {
        //             script {
        //                 try {
        //                     echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

        //                     echo "Passing Working Node to ${env.STAGE_NAME} Downstream: ${NODE_NAME}"
        //                     echo "Passing Build Number to ${env.STAGE_NAME} Downstream: ${params.JOB_BUILD_NUMBER}"
        //                     echo "Passing Workspace path to ${env.STAGE_NAME} Downstream: ${WORKSPACE}"
        //                     echo "Passing Git Committer Email to ${env.STAGE_NAME} Downstream: ${env.GIT_COMMIT_EMAIL}"
        //                     echo "Passing Git Committer Full Name to ${env.STAGE_NAME} Downstream: ${env.GIT_COMMIT_FULL_NAME}"

        //                     def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_Push',
        //                         parameters: [
        //                             booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "${params.USE_STAGE_CVE_TESTS}"),
        //                             string(name: 'DOCKER_REPOSITORY_TAG', value: "${params.DOCKER_REPOSITORY_TAG}"),
        //                             string(name: 'DOCKER_BUILD_IMAGE', value: "${env.DOCKER_BUILD_IMAGE}"),
        //                             string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
        //                             string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
        //                             string(name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
        //                             string(name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
        //                             string(name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}")
        //                         ]

        //                     echo "Templates_Pipeline_Docker_Push job result: ${downstreamJob.result}"

        //                 } catch (ERROR) {
        //                     echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
        //                     currentBuild.result = 'FAILURE'
        //                 } finally {
        //                     echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
        //                 }
        //             }
        //         }
        //     }
        //     post {
        //         failure {
        //             script{
        //                 echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
        //                 TestDockerImage = 'FAILURE'
        //             }
        //         }
        //         success {
        //             script{
        //                 // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
        //                 TestDockerImage = 'SUCCESS'
        //             }
        //         }
        //     }
        // }

        // stage ('Docker CVE Tests') { when { expression { env.STAGE_DOCKER_CVE_SCAN.toBoolean() && StageGitClone == 'SUCCESS' } }
        //     agent {
        //         node {
        //             label "${params.RUN_JOB_NODE_NAME}"
        //             customWorkspace "${params.JOB_WORKSPACE}"
        //             }
        //     }
        //     steps {
        //         timeout(activity: true, time: 5, unit: 'MINUTES') {
        //             script{
        //                 try {
        //                     echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

        //                             env.GIT_COMMIT_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()
        //                             env.GIT_COMMIT_FULL_NAME = sh(script: 'git show -s --pretty=%an', returnStdout: true).trim()

        //                             echo "Passing Working Node to ${env.STAGE_NAME} Downstream: ${NODE_NAME}"
        //                             echo "Passing Build Number to ${env.STAGE_NAME} Downstream: ${params.JOB_BUILD_NUMBER}"
        //                             echo "Passing Workspace path to ${env.STAGE_NAME} Downstream: ${WORKSPACE}"
        //                             echo "Passing Git Committer Email to ${env.STAGE_NAME} Downstream: ${env.GIT_COMMIT_EMAIL}"
        //                             echo "Passing Git Committer Full Name to ${env.STAGE_NAME} Downstream: ${env.GIT_COMMIT_FULL_NAME}"

        //                     def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_CVE',
        //                         parameters: [
        //                             booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "${params.USE_STAGE_CVE_TESTS}"),
        //                             string(name: 'DOCKER_REPOSITORY_TAG', value: "${params.DOCKER_REPOSITORY_TAG}"),
        //                             string(name: 'DOCKER_BUILD_IMAGE', value: "${env.DOCKER_BUILD_IMAGE}"),
        //                             string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
        //                             string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
        //                             string(name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
        //                             string(name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
        //                             string(name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}")
        //                         ]

        //                     echo "Templates_Pipeline_Docker_CVE job result: ${downstreamJob.result}"

        //                 } catch (ERROR) {
        //                     echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
        //                     currentBuild.result = 'FAILURE'
        //                 } finally {
        //                     echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
        //                 }
        //             }
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
