pipeline {
    agent none

    environment {
        CUSTOM_TOOL_AUTOPEP8                    = tool name: 'Autopep8', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        CUSTOM_TOOL_GOLINT                      = tool name: 'Golangci-lint', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        CUSTOM_TOOL_YAMLINT                     = tool name: 'Yamllint', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        CUSTOM_TOOL_PRECOMMIT                   = tool name: 'PreCommit', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        CUSTOM_TOOL_CHECK_JSONSCHEMA            = tool name: 'Check-jsonschema', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        STAGE_CODE_VALIDATION_LINTING_PYTHON    = "true"
        STAGE_CODE_VALIDATION_LINTING_GO        = "true"
        STAGE_CODE_VALIDATION_LINTING_YAML      = "true"
        STAGE_CODE_VALIDATION_LINTING_XML       = "true"
        STAGE_CODE_VALIDATION_LINTING_JSON      = "true"
        STAGE_CODE_VALIDATION_LINTING_MARKDOWN  = "true"
        STAGE_CODE_SPELLING                     = "true"
        STAGE_SECRET_LEAKS_AWS                  = "true"
        STAGE_SECRET_LEAKS_PASSWORD             = "true"
    }

    options {
        timeout(time: 20, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Git Clone') {
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
                }
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            checkout([$class: 'GitSCM', branches: [[name: "${params.SET_GIT_REPOSITORY_BRANCH}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GitHub-SSH-Access-Credentials-to-JobAssignment-Git', url: "git@github.com:mjhfvi/${params.SET_GIT_REPOSITORY_URL}.git"]]])

                            env.GIT_COMMIT_FULL_NAME = sh(script: 'git show -s --pretty=%an', returnStdout: true).trim()
                            env.GIT_COMMIT_EMAIL = sh(script: 'git --no-pager show -s --format=\'%ae\'', returnStdout: true).trim()

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

        stage('Code Validation') {
            parallel{
                stage('Linting Python') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_PYTHON.toBoolean() } }
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh( script: '${TOOL_AUTOPEP8}/autopep8 -v --in-place --recursive .', returnStdout: true ).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                currentBuild.result = 'FAILURE'

                                def catchErrorHandling = "${ERROR}"
                                if (catchErrorHandling.contains("exit code 1")) {
                                    sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                    sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
                                }
                                if (catchErrorHandling.contains("exit code 127")) {
                                    sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                    sh ("echo \033[41m\033[97m\033[1mTrying to install autopep8 with pip \033[0m")
                                    sh ("pip install autopep8")
                                }
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh (script: '${TOOL_GOLINT}/golangci-lint run -v .', returnStdout: true).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try{
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh( script: '${TOOL_YAMLINT}/yamllint .', returnStdout: true ).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try {
                                echo '\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m'

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                                // RUN_RESULT = sh( script: '/home/tzahi/.local/bin/xmllint --timing --valid --noout *.xml', returnStdout: true ).trim()
                                RUN_RESULT = sh( script: 'pre-commit run -a check-xml', returnStdout: true ).trim()

                            } catch (Exception ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                if (catchErrorHandling.contains("66b185e2-3205-4c66-946b-5af62094a041")) {
                                    sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                    sh ("echo \033[41m\033[97m\033[1mrun 'pre-commit install' in the repo to initiate the pre-commit \033[0m")
                                }
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"


                                RUN_RESULT = sh( script: 'check-jsonschema -v --builtin-schema *.json', returnStdout: true ).trim()
                                RUN_RESULT = sh( script: 'pre-commit run -a pretty-format-json', returnStdout: true ).trim()
                                RUN_RESULT = sh( script: 'pre-commit run -a check-json', returnStdout: true ).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                if (catchErrorHandling.contains("66b185e2-3205-4c66-946b-5af62094a041")) {
                                    sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                                    sh ("echo \033[41m\033[97m\033[1mrun 'pre-commit install' in the repo to initiate the pre-commit \033[0m")
                                }
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script{
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh( script: 'mdl .', returnStdout: true ).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
                                echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                                echo "install mdl from https://github.com/markdownlint/markdownlint"
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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        script {
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh( script: 'pre-commit run -a typos', returnStdout: true ).trim()

                                // echo "Saving Report File to Jenkins Archive Artifacts"
                                // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                                // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                            } catch (Exception ERROR) {
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

        stage('Secret Leaks') {
            parallel{
                stage('Search AWS Credentials') { when { expression { env.STAGE_SECRET_LEAKS_AWS.toBoolean() } }
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        timeout(activity: true, time: 10, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    RUN_RESULT = sh( script: 'pre-commit run detect-aws-credentials -a', returnStdout: true ).trim()
                                    RUN_RESULT = sh( script: 'pre-commit run detect-private-key -a', returnStdout: true ).trim()

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
                    // agent {
                    //     node {
                    //         label "${params.RUN_JOB_NODE_NAME}"
                    //         customWorkspace "${params.JOB_WORKSPACE}"
                    //         }
                    //     }
                    steps {
                        timeout(activity: true, time: 10, unit: 'MINUTES') {
                            script {
                            try {
                                echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                RUN_RESULT = sh( script: 'pre-commit run gitleaks -a', returnStdout: true ).trim()

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

        stage('Build Docker Image') { when { expression { params.USE_STAGE_BUILD_DOCKER_IMAGE.toBoolean() && GitClone == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Docker'
Build Number: '${params.JOB_BUILD_NUMBER}'
Working Node: '${env.NODE_NAME}'
Workspace Path: '${WORKSPACE}'
Git Committer Email: '${env.GIT_COMMIT_EMAIL}'
Git Committer Full Name: '${env.GIT_COMMIT_FULL_NAME}'
=============================================================
"""

                            def downstreamJob = build job: 'Pipeline_Templates/Templates_Pipeline_Docker',
                                parameters: [
                                    string( name: 'SET_GIT_REPOSITORY_URL', value: "${params.SET_GIT_REPOSITORY_URL}"),
                                    string( name: 'SET_DOCKER_REPOSITORY_TAG', value: "${params.SET_DOCKER_REPOSITORY_TAG}"),
                                    string( name: 'JOB_BUILD_NUMBER', value: "${params.JOB_BUILD_NUMBER}"),
                                    string( name: 'DOCKER_FROM_IMAGE', value: "${params.DOCKER_FROM_IMAGE}"),
                                    string( name: 'RUN_JOB_NODE_NAME', value: "${env.NODE_NAME}"),
                                    string( name: 'JOB_WORKSPACE', value: "${WORKSPACE}"),
                                    string( name: 'GIT_COMMIT_EMAIL', value: "${env.GIT_COMMIT_EMAIL}"),
                                    string( name: 'GIT_COMMIT_FULL_NAME', value: "${env.GIT_COMMIT_FULL_NAME}"),
                                    booleanParam( name: 'USE_STAGE_BUILD_DOCKER_IMAGE', value: "${params.USE_STAGE_BUILD_DOCKER_IMAGE}"),
                                    booleanParam( name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "${params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE}"),
                                    booleanParam( name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "${params.USE_STAGE_PUSH_DOCKER_IMAGE}"),
                                    booleanParam( name: 'USE_STAGE_CONTINUOUS_DEPLOYMENT', value: "${params.USE_STAGE_CONTINUOUS_DEPLOYMENT}"),
                                    booleanParam( name: 'USE_STAGE_CVE_TESTS', value: "${params.USE_STAGE_CVE_TESTS}"),
                                    booleanParam( name: 'USE_STAGE_DOCKER_UNITEST', value: "${params.USE_STAGE_DOCKER_UNITEST}"),
                                    booleanParam( name: 'USE_STAGE_DOCKER_CVE_SCAN', value: "${params.USE_STAGE_DOCKER_CVE_SCAN}")
                                ]
                            echo "Templates_Pipeline_Docker job result: ${downstreamJob.result}"

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

                    } catch (Exception ERROR) {
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
