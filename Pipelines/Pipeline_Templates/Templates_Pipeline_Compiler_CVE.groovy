pipeline {
    agent none

    // parameters {
        // choice(choices: ['https://github.com/mjhfvi/JobAssignment.git', 'https://github.com/mjhfvi/DockerExamples.git'],name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
        // string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
        // booleanParam(defaultValue: false, name: 'STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
        // booleanParam(defaultValue: false, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
        // booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
        // booleanParam(defaultValue: false, name: 'STAGE_SECURITY_TESTS', description: 'test security vulnerabilities in docker image')
        // string(name: 'RUN_JOB_NODE_NAME', description: 'Set Node Label')
    // }

    environment {
        STAGE_SECURITY_TESTS_SAFETY         = "false"
        STAGE_SECURITY_TESTS_BANDIT         = "false"
        STAGE_SECURITY_TESTS_PYUP           = "false"
        STAGE_SECURITY_TESTS_PIP_AUDIT      = "false"
        STAGE_SECURITY_TESTS_TRIVY          = "false"
    }

    options {
        timeout(time: 3, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Compiler Checks') {
            parallel{
                stage('Safety Scan') { when { expression { env.STAGE_SECURITY_TESTS_DOCKER_SCOUT.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 20, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (ERROR) {
                                    if (catchErrorHandling.contains("exit code 1")) {
                                        sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                        sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
                                    }
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
                                CodeSpelling = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                CodeSpelling = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Bandit Scan') { when { expression { env.STAGE_SECURITY_TESTS_XRAY_SCAN.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 20, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (ERROR) {
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
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
                                CodeSpelling = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                CodeSpelling = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('PyUp Scan') { when { expression { env.STAGE_SECURITY_TESTS_SONARQUBE.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 20, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (ERROR) {
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
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
                                CodeSpelling = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                CodeSpelling = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Pip-audit Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITGUARDIAN.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 20, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (ERROR) {
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"

                                    def catchErrorHandling = "${ERROR}"
                                    if (catchErrorHandling.contains("exit code 1")) {
                                        sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                        sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
                                    }
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                    currentBuild.result = 'SUCCESS'
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
                                CodeSpelling = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                CodeSpelling = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Trivy Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITLEAKS.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 20, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                } catch (ERROR) {
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                    def catchErrorHandling = "${ERROR}"
                                    if (catchErrorHandling.contains("exit code 1")) {
                                        sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                        sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
                                    }
                                    echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
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
                                CodeSpelling = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                CodeSpelling = 'SUCCESS'
                            }
                        }
                    }
                }
            }
        }
        stage ('Update Build Info') {
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
                }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                        def UPSTREAM_BUILD_NUMBER   = "${params.CHANGE_BUILD_NUMBER}"
                        echo "Current Build number is ${UPSTREAM_BUILD_NUMBER}"

                        def CHANGE_BUILD_NUMBER     = "${UPSTREAM_BUILD_NUMBER}"
                        currentBuild.displayName    = "#${CHANGE_BUILD_NUMBER}"
                        echo "Changing Build number to ${CHANGE_BUILD_NUMBER}"

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
            echo "The Build is Aborted, Sending Email Notifications"
            addBadge(icon: "delete.gif", text: "Build Aborted")
        }
        unstable {
            echo "The Build is Unstable, Sending Email Notifications"
            addBadge(icon: "warning.gif", text: "Build Unstable")
        }
        failure {
            echo "The Build is a Failure, Sending Email Notifications"
            addBadge(icon: "error.gif", text: "Build Failure")
        }
        success {
            echo "The Build is Successfully, Sending Email Notifications"
            addBadge(icon: "success.gif", text: "Build Success")
        }
        always {
            echo "The Build is Done, Running Always Post Condition"
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
