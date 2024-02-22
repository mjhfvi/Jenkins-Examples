// @Library('Jenkins-Shared-Library') _

pipeline {
    agent { label 'linux' }

    environment {
        GIT_REPOSITORY_URL                  = "https://github.com/mjhfvi/DockerExamples.git"
        DOCKER_FILE                         = "dockerfile.nginx"
        DOCKER_REPOSITORY                   = "mjhfvi/demo"
        STAGE_BUILD_DOCKER_IMAGE            = "true"
        STAGE_TEST_DOCKER_IMAGE             = "true"
        STAGE_PUSH_IMAGE_TO_ARTIFACTORY     = "false"
        STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO  = "false"
        STAGE_SECURITY_TESTS                = "true"
        ARTIFACTORY_SERVER                  = "192.168.50.50:8082"
    }

    parameters {
        gitParameter branchFilter: 'origin/(.*)', defaultValue: 'main', name: 'BRANCH', type: 'PT_BRANCH', description: 'Choose the Branch to run'
        // string ( name: 'BRANCH_TO_RUN', defaultValue: 'dev', description: 'Name of Branch to run', trim: false )
        // choice ( name: 'BRANCH_TO_RUN', choices: ['dev', 'Releases/1.0'], description: 'Select a Branch to run')
        // string ( name: 'DOCKER_IMAGE_NODE_VERSION', defaultValue: '16.16.0', description: 'Node Version to Run in Docker.', trim: false )
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
        // retry(3)
    }

    triggers {
        GenericTrigger (
            genericVariables: [ [key: 'ref', value: '$.ref'] ],
            token: 'pipeline_token',
            causeString: 'Triggered by $.actor.displayName for BitBucket Project $ref from $branch',
            printContributedVariables: true,
            printPostContent: true
        )
    }

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
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        StageGitClone = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
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
                            docker.build("${env.DOCKER_REPOSITORY}", "-f ${env.DOCKER_FILE} --no-cache --memory=100m .")
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        BuildDockerImage = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        BuildDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage('Test Docker Image') { when { expression { env.STAGE_TEST_DOCKER_IMAGE.toBoolean() && BuildDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    print("Docker Image Testing")
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            script {
                                DOCKER_OUTPUT = DOCKER_BUILD_IMAGE.inside {
                                    sh(script: 'ls', label: 'Folder List for Testing', returnStdout: true).trim()
                                }
                                if (DOCKER_OUTPUT.contains('Dockerfile') || output.contains('Dockerfile.nginx')) {
                                    echo 'Dockerfile found in console output!'
                                } else {
                                    echo 'Dockerfile not found in console output!'
                                }
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
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        TestDockerImage = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TestDockerImage = 'SUCCESS'
                    }
                }
            }
        }

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
                            def catchErrorHandling = "${ERROR}.trim()"
                            if (catchErrorHandling.contains("ouldn't execute docker task")) {
                            bat ("echo \033[41m\033[97m\033[1mthis is a catch error in jenkins\033[0m")}
                            // else if (cmdOutput.contains("docker build failed with exit code 1")) {
                            // error "Command failed with error : ${ERROR}. Retrying ...."}
                            // bat ("echo \033[41m\033[97m\033[1mthis is a catch error in jenkins\033[0m")
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        PushImagetoArtifactory = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
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
                        currentBuild.result = 'FAILURE'
                    } finally {
                        echo "\033[42m\033[97mStep ${env.STAGE_NAME} Done\033[0m"
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'SUCCESS'
                    }
                }
            }
        }

        stage('Git Secrets Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
            steps { // https://github.com/awslabs/git-secrets
                script {
                    try {
                        def gitSecretsOutput = sh(script: 'git secrets --list', returnStdout: true).trim()
                        // echo "Git Secrets Scan Output:"
                        // echo secretsOutput
                        if (gitSecretsOutput.contains("secrets.patterns")) {
                            error "Secrets found in repository!"}
                        writeFile(file: "git-secrets-scan-report.txt", text: gitSecretsOutput, encoding: "UTF-8")
                        archiveArtifacts artifacts: 'git-secrets-scan-report.txt', onlyIfSuccessful: false, allowEmptyArchive: false // https://www.jenkins.io/doc/pipeline/steps/core/
                    } catch (ERROR) {
                        def catchErrorHandling = "${ERROR}"
                        if (catchErrorHandling.contains("exit code 1")) {
                            sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                            sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
                        }
                        currentBuild.result = 'SUCCESS'
                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                    } finally {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                    }
                }
            }
        }

        stage('GitLeaks Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
            steps {
                script {
                    try {
                        sh(script: 'gitleaks detect --report-path gitleaks-detect-report.json', returnStdout: false)
                        def gitLeaksOutput = sh(script: 'gitleaks detect --baseline-path gitleaks-detect-report.json --report-path gitleaks-detect-findings.json', returnStdout: false)
                        // def gitLeaksOutput = sh(script: 'gitleaks detect --report-path=./gitleaks-leaks-report.json', returnStdout: true).trim()
                        echo "GitLeaks Scan Output:"
                        echo gitLeaksOutput
                        // writeFile(file: "gitleaks-detect-report.json", text: gitLeaksOutput, encoding: "UTF-8")
                        archiveArtifacts artifacts: 'gitleaks-detect-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                    } catch (ERROR) {
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
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'SUCCESS'
                    }
                }
            }
        }

        stage('GitGuardian Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
            environment {
                GITGUARDIAN_API_KEY = credentials('GitGuardian-Access-Credentials')
            }
            steps {
                script {
                    try {
                        sh(script: 'ggshield secret scan path . --recursive --show-secrets --exit-zero --output=ggshield-secret-report.json --json -y', label:"GitGuardian Files and Folders Scan",returnStdout: false)
                        // echo 'GitGuardian docker image scan'
                        // sh 'ggshield secret scan docker gitguardian/ggshield --output=ggshield.json --json --show-secrets --exit-zero'
                        archiveArtifacts artifacts: 'ggshield-secret-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                        // sh(script: 'ggshield secret scan ci --show-secrets --exit-zero --output=ggshield-ci-report.json --json --debug', label:"GitGuardian CI Scan", returnStdout: false)
                        // archiveArtifacts artifacts: 'ggshield-ci-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                    } catch (ERROR) {
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
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'SUCCESS'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script{
                    withCredentials([string(credentialsId: 'SonarQube-Access-Credentials', variable: 'SONAR_TOKEN')]) {
                        sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
                    }
                }
            }
            post {          //  always, changed, fixed, regression, aborted, failure, success, unstable, unsuccessful, and cleanup
                failure {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'FAILURE'
                        }
                    }
                success {   // "SUCCESS", "UNSTABLE", "FAILURE", "NOT_BUILT", "ABORTED"
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PublishBuildArtifactoryInfo = 'SUCCESS'
                    }
                }
            }
        }
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
            // cleanWs() // Clean workspace after each run
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}