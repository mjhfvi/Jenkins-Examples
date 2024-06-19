pipeline {
    agent { label 'linux' }

    parameters {
        // separator(name: "GIT", sectionHeader: "Run Git Logic")
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        choice(choices: ['main', 'release', '1.0'], name: 'SET_GIT_REPOSITORY_BRANCH', description: 'Choose Git Repository Branch')
        separator(name: "DOCKER", sectionHeader: "Run Docker  Steps")
        string(defaultValue: '00', name: 'SET_DOCKER_REPOSITORY_TAG', description: 'Set Docker Tag Number')
        // string(defaultValue: '', name: 'DOCKER_FROM_IMAGE', description: 'Choose Docker Base Image - use only for tag testing, otherwise update the dockerfile')
        booleanParam(defaultValue: true, name: 'USE_STAGE_BUILD_DOCKER_IMAGE', description: 'Build Docker Image')
        // booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'Use Cache When Building Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_DOCKER_UNITEST', description: 'Run Unitest for Docker Image')
        // booleanParam(defaultValue: true, name: 'USE_STAGE_DOCKER_CVE_SCAN', description: 'Test Security Vulnerability Exploits For Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'Push Docker Image to Repository')
        booleanParam(defaultValue: false, name: 'USE_STAGE_CONTINUOUS_DEPLOYMENT', description: 'Push Docker Image to Repository')
        separator(name: "CLEANUP", sectionHeader: "Run Cleanup Step")
        booleanParam(defaultValue: false, name: 'RUN_CLEANUP', description: 'Cleanup Temporary Jenkins Files')
    }

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

        stage('Build Docker Image') { when { expression { params.USE_STAGE_BUILD_DOCKER_IMAGE.toBoolean() && StageGitClone == 'SUCCESS'  } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir("jenkins") {
                                load "DOCKER_BUILD_INFORMATION.txt"
                            }

                            dir("${env.DOCKER_BUILD_FOLDER}") {
                                DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${params.SET_DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_BUILD_FILE} --memory=${env.DOCKER_BUILD_MEMORY_CACHE} .")
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
                        StageBuildDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageBuildDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage('UniTest Docker Image') { when { expression { params.USE_STAGE_DOCKER_UNITEST.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 20, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            CONTAINER_ID = DOCKER_BUILD_IMAGE.run("--env=HOST_VAR='${env.SERVER_IP}' -p '${env.API_PORT}':'${env.API_PORT}'")

                            echo 'Running Docker Container to Test Health Check with API call'
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
                        StageUniTestDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} is Successfully, Sending Notifications\033[0m"
                        StageUniTestDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage('Push Docker Image') { when { expression { params.USE_STAGE_PUSH_DOCKER_IMAGE.toBoolean() && StageUniTestDockerImage == 'SUCCESS' } }
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
                        StagePushDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StagePushDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Docker Scout CVE') { when { expression { env.STAGE_SECURITY_TESTS_DOCKER_SCOUT.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
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
                                // sh( script: "docker scout cves --format markdown --locations --ignore-base --output dockerscout.html '${env.DOCKER_REPOSITORY}':'${params.SET_DOCKER_REPOSITORY_TAG}'", returnStdout: true ).trim()
                            }

                            archiveArtifacts artifacts: 'dockerscout.json', fingerprint: true

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
                        StageDockerScoutCVE = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageDockerScoutCVE = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Xray Scan') { when { expression { env.STAGE_SECURITY_TESTS_XRAY_SCAN.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
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
                        StageXrayScan = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageXrayScan = 'SUCCESS'
                    }
                }
            }
        }

        stage ('SonarQube Analysis') { when { expression { env.STAGE_SECURITY_TESTS_SONARQUBE.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
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
                        StageSonarQubeAnalysis = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageSonarQubeAnalysis = 'SUCCESS'
                    }
                }
            }
        }

        stage ('GitGuardian Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITGUARDIAN.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
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
                        StageGitGuardianScan = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageGitGuardianScan = 'SUCCESS'
                    }
                }
            }
        }

        stage ('GitLeaks Scan') { when { expression { env.STAGE_SECURITY_TESTS_GITLEAKS.toBoolean() && StageBuildDockerImage == 'SUCCESS' } }
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
                        StageGitLeaksScan = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageGitLeaksScan = 'SUCCESS'
                    }
                }
            }
        }

        // stage('Continuous Deployment') { when { expression { params.USE_STAGE_CONTINUOUS_DEPLOYMENT.toBoolean() && BuildDockerImage == 'SUCCESS' } }
        //     steps {
        //         timeout(activity: true, time: 20, unit: 'MINUTES') {
        //             script {
        //                 try {
        //                     echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

        //                     echo "building ArgoCD Application for kubernetes"
        //                     dir("argocd"){
        //                         sh ("kustomize build --enable-helm | kubectl apply -f -")
        //                     }

        //                     def ARGOCD_PASSWORD = sh(script: "kubectl -n argo-cd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d", returnStdout: true)
        //                     // echo "ArgoCD Password: ${env.ARGOCD_PASSWORD}"

        //                     echo "logging to ArgoCD Service on argocd.localhost"
        //                     sh ("argocd login --username admin --password ${ARGOCD_PASSWORD} --insecure argocd.localhost")

        //                     echo "building Argocd Repository Configuration"
        //                     sh "argocd repo add git@github.com:mjhfvi/JobAssignment.git --ssh-private-key-path JobAssignment_ssh_login_key_no_password_ed25519"
        //                     sh "kubectl -f argocd-add-repo.yaml apply"

        //                     echo "building my Application with ArgoCD"
        //                     dir("jobProject"){
        //                         sh ("argocd app create job-project -f argocd-jobProject.yaml")
        //                     }

        //                 } catch (Exception ERROR) {
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
        //                 StagePushDockerImage = 'FAILURE'
        //             }
        //         }
        //         success {
        //             script{
        //                 // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
        //                 StagePushDockerImage = 'SUCCESS'
        //             }
        //         }
        //     }
        // }
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
            cleanWs() // Clean workspace after each run
        }
    }
}
