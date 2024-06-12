pipeline {
    agent none

    environment {
        STAGE_CODE_VALIDATION_LINTING_PYTHON    = "true"
        STAGE_CODE_VALIDATION_LINTING_GO        = "false"
        STAGE_CODE_VALIDATION_LINTING_YAML      = "false"
        STAGE_CODE_VALIDATION_LINTING_XML       = "false"
        STAGE_CODE_VALIDATION_LINTING_JSON      = "false"
        STAGE_CODE_VALIDATION_LINTING_MARKDOWN  = "false"
        STAGE_CODE_SPELLING                     = "false"
        // AUTOPEP8_HOME                           = tool name: 'Autopep8', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
    }

    options {
        timeout(time: 3, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('CodeValidation') {
            parallel{
                stage('Linting Python') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_PYTHON.toBoolean() } }
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

                                // def String RUN_RESULT
                                RUN_RESULT = sh( script: '/home/tzahi/.local/bin/autopep8 -v --in-place --recursive .', returnStdout: true ).trim()

                                // def RUN_RESULT = sh( script: '/home/tzahi/.local/bin/autopep8 -v --in-place --recursive .', returnStdout: true ).trim()
                                // echo "${GIT_COMMIT_FULL_NAME}"

                                echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingPython = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingPython = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Linting Go') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_GO.toBoolean() } }
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

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }

                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingGo = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingGo = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Linting YAML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_MARKDOWN.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        script{
                            try{
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }
                                // - repo: github.com/igorshubovych/markdownlint-cli
                                //     rev: v0.41.0
                                //     hooks:  # Use yamlfix to fix yaml files
                                //     - id: markdownlint
                                //         name: markdownlint (linter for Markdown files)

                                // - repo: https://github.com/pre-commit/pre-commit-hooks
                                //     rev: v4.5.0
                                //     hooks:
                                //     - id: check-yaml
                                //         verbose: true
                                //         args: [--allow-multiple-documents]
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingYAML = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingYAML = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Linting XML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_XML.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        script{
                            try {
                                echo '\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m'

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }
                                // - repo: https://github.com/pre-commit/pre-commit-hooks
                                //     rev: v4.5.0
                                //     hooks:
                                //     - id: check-xml
                                //         verbose: true
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingXMP = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingXML = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Linting JSON') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_JSON.toBoolean() } }
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

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }

                                // - repo: https://github.com/pre-commit/pre-commit-hooks
                                //     rev: v4.5.0
                                //     hooks:
                                //     - id: check-json
                                //         verbose: true
                                //     - id: pretty-format-json
                                //         verbose: true
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingJSON = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingJSON = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Linting Markdown') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_JSON.toBoolean() } }
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

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }
                                // - repo: https://github.com/pre-commit/pre-commit-hooks
                                //     rev: v4.5.0
                                //     hooks:
                                //     - id: check-json
                                //         verbose: true
                                //     - id: pretty-format-json
                                //         verbose: true
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }
                    }
                    post {
                        failure {
                            script{
                                echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                                LintingMarkdown = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                LintingMarkdown = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Code Spelling') { when { expression { env.STAGE_CODE_SPELLING.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        script {
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                // RUN_RESULT = dir("${params.JOB_WORKSPACE}") { sh (script: 'autopep8 -v --in-place --recursive .', returnStdout: true).trim() }
                                // - repo: https://github.com/crate-ci/typos
                                //     rev: v1.18.2
                                //     hooks:
                                //     - id: typos
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
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
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
