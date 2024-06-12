pipeline {
    agent none

    environment {
        // STAGE_CODE_VALIDATION            = "$params.STAGE_CODE_VALIDATION"    // "false"
        STAGE_SECRET_LEAKS_AWS              = "false"
        STAGE_SECRET_LEAKS_PASSWORD         = "false"
    }

    options {
        timeout(time: 3, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Secret Leaks') {
            parallel{
                stage('Search AWS Credentials') { when { expression { env.STAGE_SECRET_LEAKS_AWS.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 10, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (Exception ERROR) {
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
                                SearchAWSCredentials = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                SearchPasswords = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Search Passwords') { when { expression { env.STAGE_SECRET_LEAKS_PASSWORD.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 10, unit: 'MINUTES') {
                            script {
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (Exception ERROR) {
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
                                SearchPasswords = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                SearchPasswords = 'SUCCESS'
                            }
                        }
                    }
                }
            }
        }

        stage ('Update Build Info') {
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
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
