pipeline {
    agent none

    environment {
        // STAGE_CODE_VALIDATION                   = "$params.STAGE_CODE_VALIDATION"    // "false"
        STAGE_CODE_VALIDATION_LINTING_PYTHON    = "false"
        STAGE_CODE_VALIDATION_LINTING_GO        = "false"
        STAGE_CODE_VALIDATION_LINTING_YAML      = "false"
        STAGE_CODE_VALIDATION_LINTING_XML       = "false"
        STAGE_CODE_VALIDATION_LINTING_JSON      = "false"
        STAGE_CODE_VALIDATION_LINTING_MARKDOWN  = "false"
        STAGE_CODE_SPELLING                     = "false"
        RUN_JOB_NODE_NAME                       = "$params.RUN_JOB_NODE_NAME"
        CHANGE_BUILD_NUMBER                     = "$params.CHANGE_BUILD_NUMBER"
    }

    options {
        timeout(time: 20, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('CodeValidation') {
            parallel{
                stage('Linting Python') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_PYTHON.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                            // - repo: https://github.com/pre-commit/pre-commit-hooks
                            //     rev: v4.5.0
                            //     hooks:
                            //     - id: check-yaml
                            //         verbose: true
                            //         args: [--allow-multiple-documents]

                            // - repo: https://github.com/asottile/reorder-python-imports
                            //     rev: v3.12.0
                            //     hooks:
                            //     - id: reorder-python-imports
                            //         name: reorder-python-imports (Tool for automatically reordering python imports)
                            //         args: [--py39-plus, --add-import, from __future__ import annotations]
                            //         exclude: ^(pre_commit/resources/|testing/resources/python3_hooks_repo/)

                            // - repo: https://github.com/hhatto/autopep8
                            //     rev: v2.0.4
                            //     hooks:
                            //     - id: autopep8
                            //         name: autopep8 (Style Guide Enforcement)
                        }
                    }
                }

                stage('Linting Go') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_GO.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
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

                stage('Linting YAML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_MARKDOWN.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
                            try{
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                            // - repo: github.com/igorshubovych/markdownlint-cli
                            //     rev: v0.41.0
                            //     hooks:  # Use yamlfix to fix yaml files
                            //     - id: markdownlint
                            //         name: markdownlint (linter for Markdown files)
                        }
                    }
                }

                stage('Linting XML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_XML.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
                            try {
                                echo '\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m'
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                            // - repo: https://github.com/pre-commit/pre-commit-hooks
                            //     rev: v4.5.0
                            //     hooks:
                            //     - id: check-xml
                            //         verbose: true
                        }
                    }
                }

                stage('Linting JSON') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_JSON.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                            // - repo: https://github.com/pre-commit/pre-commit-hooks
                            //     rev: v4.5.0
                            //     hooks:
                            //     - id: check-json
                            //         verbose: true
                            //     - id: pretty-format-json
                            //         verbose: true
                        }
                    }
                }

                stage('Linting Markdown') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_JSON.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                            // - repo: https://github.com/pre-commit/pre-commit-hooks
                            //     rev: v4.5.0
                            //     hooks:
                            //     - id: check-json
                            //         verbose: true
                            //     - id: pretty-format-json
                            //         verbose: true
                        }
                    }
                }

                stage('Spelling') { when { expression { env.STAGE_CODE_SPELLING.toBoolean() } }
                    agent { label "${env.RUN_JOB_NODE_NAME}" }
                    steps {
                        script {
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                            } catch (ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'
                            } finally {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                            }
                        }

                            // - repo: https://github.com/crate-ci/typos
                            //     rev: v1.18.2
                            //     hooks:
                            //     - id: typos
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

                        echo "Build Number from Upstream is: ${params.JOB_BUILD_NUMBER}"
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
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
