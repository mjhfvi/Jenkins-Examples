pipeline {
    agent { label 'linux' }

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
        timeout(time: 5, unit: 'MINUTES')                                 // Overall Time for the Build to Run
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

                            checkout([$class: 'GitSCM', branches: [[name: "${params.SET_GIT_REPOSITORY_BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GitHub-SSH-Access-Credentials-to-JobAssignment-Git', url: "git@github.com:mjhfvi/${params.SET_GIT_REPOSITORY_URL}.git"]]])

                            env.GIT_COMMIT_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()
                            env.GIT_COMMIT_FULL_NAME = sh(script: 'git show -s --pretty=%an', returnStdout: true).trim()

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
                        GitClone = 'FAILURE'
                    }
                }
                success {
                    script{
                        GitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Code Checks') {
            parallel{
                stage ('Code Validation') { when { expression { params.USE_STAGE_CODE_VALIDATION.toBoolean() && GitClone == 'SUCCESS' } }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Code_Validation'
Build Number: '${params.JOB_BUILD_NUMBER}'
Working Node: '${env.NODE_NAME}'
Workspace Path: '${WORKSPACE}'
Git Committer Email: '${env.GIT_COMMIT_EMAIL}'
Git Committer Full Name: '${env.GIT_COMMIT_FULL_NAME}'
=============================================================
"""

                                    def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Code_Validation',
                                        parameters: [
                                            booleanParam(name: 'USE_STAGE_CODE_VALIDATION', value: "${params.USE_STAGE_CODE_VALIDATION}"),
                                            string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                            string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
                                            string(name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
                                            string(name: 'GIT_COMMIT_EMAIL', value: "${GIT_COMMIT_EMAIL}"),
                                            string(name: 'GIT_COMMIT_FULL_NAME', value: "${GIT_COMMIT_FULL_NAME}"),
                                            string( name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
                                            string( name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}")
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

                stage ('Secret Leaks') { when { expression { params.USE_STAGE_SECRET_LEAKS.toBoolean() && GitClone == 'SUCCESS' } }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Secret_Leaks'
Build Number: '${params.JOB_BUILD_NUMBER}'
Working Node: '${env.NODE_NAME}'
Workspace Path: '${WORKSPACE}'
Git Committer Email: '${env.GIT_COMMIT_EMAIL}'
Git Committer Full Name: '${env.GIT_COMMIT_FULL_NAME}'
=============================================================
"""

                                    def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Secret_Leaks',
                                        parameters: [
                                            booleanParam(name: 'USE_STAGE_SECRET_LEAKS', value: "${params.USE_STAGE_SECRET_LEAKS}"),
                                            string(name: 'DOCKER_REPOSITORY_TAG', value: "${params.DOCKER_REPOSITORY_TAG}"),
                                            string(name: 'DOCKER_BUILD_IMAGE', value: "${env.DOCKER_BUILD_IMAGE}"),
                                            string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                            string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
                                            string(name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
                                            string(name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
                                            string(name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}")
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
                        CodeValidation = 'FAILURE'
                    }
                }
                success {
                    script{
                        CodeValidation = 'SUCCESS'
                    }
                }
            }
        }

        stage('Build Docker Image') { when { expression { params.USE_STAGE_BUILD_DOCKER_IMAGE.toBoolean() && GitClone == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Docker_Build'
Build Number: '${params.JOB_BUILD_NUMBER}'
Working Node: '${env.NODE_NAME}'
Workspace Path: '${WORKSPACE}'
Git Committer Email: '${env.GIT_COMMIT_EMAIL}'
Git Committer Full Name: '${env.GIT_COMMIT_FULL_NAME}'
=============================================================
"""

                            def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_Build',
                                parameters: [
                                    string(name: 'SET_GIT_REPOSITORY_URL', value: "${params.SET_GIT_REPOSITORY_URL}"),
                                    string(name: 'SET_DOCKER_REPOSITORY_TAG', value: "${params.SET_DOCKER_REPOSITORY_TAG}"),
                                    string(name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
                                    string(name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                    string(name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
                                    string(name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
                                    string(name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}"),
                                    booleanParam(name: 'USE_STAGE_BUILD_DOCKER_IMAGE', value: "${params.USE_STAGE_BUILD_DOCKER_IMAGE}"),
                                    booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "${params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE}"),
                                    booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "${params.USE_STAGE_PUSH_DOCKER_IMAGE}"),
                                    booleanParam(name: 'USE_STAGE_DEPLOY_TO_ENVIRONMENT', value: "${params.USE_STAGE_DEPLOY_TO_ENVIRONMENT}"),
                                    booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "${params.USE_STAGE_CVE_TESTS}"),
                                    booleanParam(name: 'USE_STAGE_DOCKER_UNITEST', value: "${params.USE_STAGE_DOCKER_UNITEST}"),
                                    booleanParam(name: 'USE_STAGE_DOCKER_CVE_SCAN', value: "${params.USE_STAGE_DOCKER_CVE_SCAN}")
                                ]
                            echo "Templates_Pipeline_Docker_Build job result: ${downstreamJob.result}"

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
                        BuildDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        BuildDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Update Build Info') {
            // agent { label "${env.RUN_JOB_NODE_NAME}" }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        echo """
=============================================================
Upstream Build Number is: ${params.JOB_BUILD_NUMBER}
Changing Build Number to Upstream Number: ${params.JOB_BUILD_NUMBER}
=============================================================
"""
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
