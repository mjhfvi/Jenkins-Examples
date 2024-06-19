pipeline {
    agent none

    environment {
        STAGE_SECURITY_TESTS_DOCKER_SCOUT       = "true"
        STAGE_SECURITY_TESTS_XRAY_SCAN          = "false"
        STAGE_SECURITY_TESTS_SONARQUBE          = "false"
        STAGE_SECURITY_TESTS_GITGUARDIAN        = "false"
        STAGE_SECURITY_TESTS_GITLEAKS           = "false"
        DOCKER_BUILD_MEMORY_CACHE               = "100M"
        API_PORT                                = "5000"
        SERVER_IP                               = "192.168.50.50"
        // DOCKER_FROM_IMAGE                    = "3.9.18-slim"
    }

    options {
        timeout(time: 10, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Build Docker Image') { when { expression { params.USE_STAGE_BUILD_DOCKER_IMAGE.toBoolean() } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    // customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            load "${params.JOB_WORKSPACE}/jenkins/DOCKER_BUILD_INFORMATION.txt"

                            if (("${params.JOB_WORKSPACE}/${params.DOCKER_FROM_IMAGE}").isEmpty()){
                                dir("${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${params.SET_DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --memory=${env.DOCKER_BUILD_MEMORY_CACHE} .")
                                }
                            } else {
                                dir("${params.JOB_WORKSPACE}/${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${params.SET_DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --build-arg BASE_OS_VERSION=${params.DOCKER_FROM_IMAGE} --memory=${env.DOCKER_BUILD_MEMORY_CACHE} .")
                                }
                            }

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

        stage('UniTest Docker Image') { when { expression { params.USE_STAGE_DOCKER_UNITEST.toBoolean() && BuildDockerImage == 'SUCCESS' } }
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

                            CONTAINER_ID = DOCKER_BUILD_IMAGE.run("--env=HOST_VAR='${env.SERVER_IP}' -p '${env.API_PORT}':'${env.API_PORT}'")

                            echo 'Running Docker Container to Test Health Check withAPI call'
                            def response = httpRequest "http://${env.SERVER_IP}:${env.API_PORT}/health"
                            def json = readJSON text: response.content
                            if (json.version.version == '1.0' && json.version.status == 'healthy') {
                                echo '\033[45m\033[97m\033[1mHealth Check Passed: Version is 1.0 and Status is Healthy. \033[0m'
                            } else {
                                error '\033[41m\033[97m\033[1mHealth Check Failed: Version is not 1.0 or Status is not Healthy.\033[0m'
                            }

                        } catch (Exception ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "Killing the Docker Container"
                            CONTAINER_ID.stop()
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
            post {
                failure {
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} is a Failure, Sending Notifications\033[0m"
                        UniTestDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} is Successfully, Sending Notifications\033[0m"
                        UniTestDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage('Push Docker Image') { when { expression { params.USE_STAGE_PUSH_DOCKER_IMAGE.toBoolean() && BuildDockerImage == 'SUCCESS' } }
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

                            docker.withRegistry('https://registry.hub.docker.com', 'Docker-Hub-Login-Credentials') {
                                DOCKER_BUILD_IMAGE.push("${params.SET_DOCKER_REPOSITORY_TAG}")
                                DOCKER_BUILD_IMAGE.push("latest")
                            }

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
                        PushDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PushDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Docker CVE Tests') { when { expression { params.USE_STAGE_DOCKER_CVE_SCAN.toBoolean() && BuildDockerImage == 'SUCCESS' } }
            parallel{
                stage('Docker Scout CVE') { when { expression { env.STAGE_SECURITY_TESTS_DOCKER_SCOUT.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            // customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    // Install Docker Scout
                                    sh "curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s -- -b ${params.JOB_WORKSPACE}/bin"

                                    // Analyze critical or high vulnerabilities
                                    withCredentials([usernamePassword(credentialsId: 'Docker-Hub-Login-Credentials', passwordVariable: 'password', usernameVariable: 'username')]){
                                        sh '''echo ${password} | docker login -u ${username} --password-stdin'''
                                        sh( script: "docker scout cves --format sarif --locations --ignore-base --output dockerscout.json '${env.DOCKER_REPOSITORY}':'${params.SET_DOCKER_REPOSITORY_TAG}'", returnStdout: true ).trim()
                                        sh( script: "docker scout cves --format markdown --locations --ignore-base --output dockerscout.html '${env.DOCKER_REPOSITORY}':'${params.SET_DOCKER_REPOSITORY_TAG}'", returnStdout: true ).trim()
                                    }

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
                                DockerScoutCVE = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                DockerScoutCVE = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('Xray Scan') { when { expression { env.STAGE_SECURITY_TESTS_XRAY_SCAN.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            // customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    def scanConfig = [
                                            'buildName'      : buildInfo.name,
                                            'buildNumber'    : buildInfo.number,
                                            'failBuild'      : true
                                    ]
                                    def scanResult = server.xrayScan scanConfig
                                    echo scanResult as String
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
                                XrayScan = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                XrayScan = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('SonarQube Analysis') { when { expression { env.STAGE_SECURITY_TESTS_SONARQUBE.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script{
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    withCredentials([string(credentialsId: 'SonarQube-Access-Credentials', variable: 'SONAR_TOKEN')]) {
                                        sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
                                    }

                                    def scannerHome = tool 'sonar-scanner';
                                        withSonarQubeEnv('LocalSonarQubeServer') { // If you have configured more than one global server connection, you can specify its name
                                        sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
                                    }
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
                                SonarQubeAnalysis = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                SonarQubeAnalysis = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('GitGuardian Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITGUARDIAN.toBoolean() } }
                    agent {
                        node {
                            label "${params.RUN_JOB_NODE_NAME}"
                            customWorkspace "${params.JOB_WORKSPACE}"
                            }
                        }
                    environment {
                        GITGUARDIAN_API_KEY = credentials('GitGuardian-Access-Credentials')
                    }
                    steps {
                        timeout(activity: true, time: 5, unit: 'MINUTES') {
                            script {
                                try {
                                    echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                                    sh(script: 'ggshield secret scan path . --recursive --show-secrets --exit-zero --output=ggshield-secret-report.json --json -y', label:"GitGuardian Files and Folders Scan",returnStdout: false)
                                    // echo 'GitGuardian docker image scan'
                                    // sh 'ggshield secret scan docker gitguardian/ggshield --output=ggshield.json --json --show-secrets --exit-zero'
                                    archiveArtifacts artifacts: 'ggshield-secret-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                                    // sh(script: 'ggshield secret scan ci --show-secrets --exit-zero --output=ggshield-ci-report.json --json --debug', label:"GitGuardian CI Scan", returnStdout: false)
                                    // archiveArtifacts artifacts: 'ggshield-ci-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                                } catch (Exception ERROR) {
                                    def catchErrorHandling = "${ERROR}"
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
                                GitGuardianScan = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                GitGuardianScan = 'SUCCESS'
                            }
                        }
                    }
                }

                stage('GitLeaks Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITLEAKS.toBoolean() } }
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

                                    sh(script: 'gitleaks detect --report-path gitleaks-detect-report.json', returnStdout: false)
                                    def gitLeaksOutput = sh(script: 'gitleaks detect --baseline-path gitleaks-detect-report.json --report-path gitleaks-detect-findings.json', returnStdout: false)
                                    // def gitLeaksOutput = sh(script: 'gitleaks detect --report-path=./gitleaks-leaks-report.json', returnStdout: true).trim()
                                    echo "GitLeaks Scan Output:"
                                    echo gitLeaksOutput
                                    // writeFile(file: "gitleaks-detect-report.json", text: gitLeaksOutput, encoding: "UTF-8")
                                    archiveArtifacts artifacts: 'gitleaks-detect-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
                                } catch (Exception ERROR) {
                                    def catchErrorHandling = "${ERROR}"
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
                                GitLeaksScan = 'FAILURE'
                            }
                        }
                        success {
                            script{
                                echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                                GitLeaksScan = 'SUCCESS'
                            }
                        }
                    }
                }
            }
        }

        stage('Continuous Deployment') { when { expression { params.USE_STAGE_CONTINUOUS_DEPLOYMENT.toBoolean() && BuildDockerImage == 'SUCCESS' } }
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

                            echo "building ArgoCD Application for kubernetes"
                            dir("argocd"){
                                sh ("kustomize build --enable-helm | kubectl apply -f -")
                            }

                            def ARGOCD_PASSWORD = sh(script: "kubectl -n argo-cd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d", returnStdout: true)
                            // echo "ArgoCD Password: ${env.ARGOCD_PASSWORD}"

                            echo "logging to ArgoCD Service on argocd.localhost"
                            sh ("argocd login --username admin --password ${ARGOCD_PASSWORD} --insecure argocd.localhost")

                            echo "building Argocd Repository Configuration"
                            sh "argocd repo add git@github.com:mjhfvi/JobAssignment.git --ssh-private-key-path JobAssignment_ssh_login_key_no_password_ed25519"
                            sh "kubectl -f argocd-add-repo.yaml apply"

                            echo "building my Application with ArgoCD"
                            dir("jobProject"){
                                sh ("argocd app create job-project -f argocd-jobProject.yaml")
                            }

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
                        PushDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        PushDockerImage = 'SUCCESS'
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

                    } catch (Exception sERROR) {
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
        }
    }
}
