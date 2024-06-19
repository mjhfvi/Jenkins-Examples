pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        choice(choices: ['main', 'release', '1.0'], name: 'SET_GIT_REPOSITORY_BRANCH', description: 'Choose Git Repository Branch')
        }

    environment {
        // CUSTOM_TOOL_AUTOPEP8                    = tool name: 'Autopep8', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        // CUSTOM_TOOL_GOLINT                      = tool name: 'Golangci-lint', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        // CUSTOM_TOOL_YAMLINT                     = tool name: 'Yamllint', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        // CUSTOM_TOOL_PRECOMMIT                   = tool name: 'PreCommit', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        // CUSTOM_TOOL_CHECK_JSONSCHEMA            = tool name: 'Check-jsonschema', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
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
        disableConcurrentBuilds()
        ansiColor('xterm')
    }

    stages {
        stage('Git Clone') {
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
                        StageGitClone = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageGitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting Python') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_PYTHON.toBoolean() } }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'autopep8 -v --in-place --recursive .', returnStdout: true ).trim()

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
                        StageLintingPython = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingPython = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting Go') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_GO.toBoolean() } }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh (script: 'golangci-lint run -v .', returnStdout: true).trim()

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
                        StageLintingGo = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingGo = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting YAML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_YAML.toBoolean() } }
            steps {
                script{
                    try{
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'yamllint .', returnStdout: true ).trim()

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
                        StageLintingYAML = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingYAML = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting XML') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_XML.toBoolean() } }
            steps {
                script{
                    try {
                        echo '\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m'

                        // echo "Saving Report File to Jenkins Archive Artifacts"
                        // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                        // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                        // RUN_RESULT = sh( script: '/home/tzahi/.local/bin/xmllint --timing --valid --noout *.xml', returnStdout: true ).trim()
                        sh( script: 'pre-commit run -a check-xml', returnStdout: true ).trim()

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
                        StageLintingXMP = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingXML = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting JSON') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_JSON.toBoolean() } }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'pre-commit run -a pretty-format-json', returnStdout: true ).trim()
                        sh( script: 'pre-commit run -a check-json', returnStdout: true ).trim()

                        // echo "Saving Report File to Jenkins Archive Artifacts"
                        // "archiveArtifacts artifacts: '*.json', fingerprint: true"
                        // echo "Sending ${env.STAGE_NAME} Result to ${params.GIT_COMMIT_FULL_NAME} using email: ${params.GIT_COMMIT_EMAIL}"

                    } catch (Exception ERROR) {
                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                        if ('${ERROR}'.contains("66b185e2-3205-4c66-946b-5af62094a041")) {
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
                        StageLintingJSON = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingJSON = 'SUCCESS'
                    }
                }
            }
        }

        stage('Linting Markdown') { when { expression { env.STAGE_CODE_VALIDATION_LINTING_MARKDOWN.toBoolean() } }
            steps {
                script{
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'mdl .', returnStdout: true ).trim()

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
                        StageLintingMarkdown = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageLintingMarkdown = 'SUCCESS'
                    }
                }
            }
        }

        stage('Code Spelling') { when { expression { env.STAGE_CODE_SPELLING.toBoolean() } }
            steps {
                script {
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'pre-commit run -a typos', returnStdout: true ).trim()

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
                        StageCodeSpelling = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageCodeSpelling = 'SUCCESS'
                    }
                }
            }
        }

        stage('Search AWS Credentials') { when { expression { env.STAGE_SECRET_LEAKS_AWS.toBoolean() } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            sh( script: 'pre-commit run -a detect-aws-credentials', returnStdout: true ).trim()
                            sh( script: 'pre-commit run -a detect-private-key', returnStdout: true ).trim()

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
                        StageSearchAWSCredentials = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageSearchPasswords = 'SUCCESS'
                    }
                }
            }
        }

        stage('Search Passwords') { when { expression { env.STAGE_SECRET_LEAKS_PASSWORD.toBoolean() } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                        sh( script: 'pre-commit run gitleaks -a', returnStdout: true ).trim()

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
                        StageSearchPasswords = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageSearchPasswords = 'SUCCESS'
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
