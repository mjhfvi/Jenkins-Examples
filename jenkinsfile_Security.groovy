pipeline {
    agent { label 'linux' }

    environment {
        GIT_REPOSITORY_URL                  = "https://gitlab.com/training-department/home-task-app"
        DOCKER_FILE                         = "dockerfile"
        DOCKER_REPOSITORY                   = "mjhfvi/demo-develeap"
        STAGE_BUILD_DOCKER_IMAGE            = "true"
        STAGE_TEST_DOCKER_IMAGE             = "true"
        STAGE_PUSH_DOCKER_IMAGE             = "false"
        STAGE_PUSH_IMAGE_TO_ARTIFACTORY     = "false"
        STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO  = "false"
        STAGE_SECURITY_TESTS                = "true"
        // ARTIFACTORY_SERVER                  = "http://localhost:8082"

    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
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
                        StageGitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Build Docker Image') { when { expression { env.STAGE_BUILD_DOCKER_IMAGE.toBoolean() } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    print("Docker Image Building")
                    script { // https://docs.docker.com/config/containers/resource_constraints/
                        try {
                            DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}", "-f ${env.DOCKER_FILE} --no-cache --memory=100m .")
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

        stage('Push Docker Image') { when { expression { env.STAGE_PUSH_DOCKER_IMAGE.toBoolean() && TestDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    print("Docker Image Pushing")
                    script {
                        try {
                            script {
                            //     docker.withRegistry('https://registry.example.com', 'credentials-id') {
                            //     def customImage = docker.build("my-image:${env.BUILD_ID}")
                            //     /* Push the container to the custom Registry */
                            //     customImage.push()
                            // }



                                DOCKER_OUTPUT = DOCKER_BUILD_IMAGE.inside { sh(script: 'ls', label: 'Folder List for Testing', returnStdout: true).trim()
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

        stage('Test') {
            parallel{
                stage('Docker Scout CVES') {
                    steps {
                        // Install Docker Scout
                        sh 'curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s -- -b /usr/local/bin'

                        // Log into Docker Hub
                        // sh 'echo $DOCKER_HUB_PAT | docker login -u $DOCKER_HUB_USER --password-stdin'

                        // Analyze and fail on critical or high vulnerabilities
                        sh 'docker scout cves --format sarif --locations --ignore-base --output dockerscout.json nginx'
                        sh 'docker scout cves --format markdown --locations --ignore-base --output dockerscout.html nginx'
                    }
                }

                // stage('Xray scan') {
                //     steps {
                //         script{
                //             def scanConfig = [
                //                     'buildName'      : buildInfo.name,
                //                     'buildNumber'    : buildInfo.number,
                //                     'failBuild'      : true
                //             ]
                //             def scanResult = server.xrayScan scanConfig
                //             echo scanResult as String
                //         }
                //     }
                // }

                // stage('SonarQube Analysis') {
                //     steps {
                //         script{
                //             withCredentials([string(credentialsId: 'SonarQube-Access-Credentials', variable: 'SONAR_TOKEN')]) {
                //                 sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
                //             }

                //         // def scannerHome = tool 'sonar-scanner';
                //         //     withSonarQubeEnv('LocalSonarQubeServer') { // If you have configured more than one global server connection, you can specify its name
                //         //     sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
                //         // }
                //         }
                //     }
                // }

                stage('GitGuardian Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
                    // agent {
                    //     docker { image 'gitguardian/ggshield' }
                    // }
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
                }
            }
        }

        // stage ('Artifactory Publish Build Info') {
        //     steps {
        //         script{
        //             rtBuildInfo (
        //                 captureEnv: true,
        //                 // Optional - Build name and build number. If not set, the Jenkins job's build name and build number are used.
        //                 buildName: 'my-build',
        //                 buildNumber: '20',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'my-project-key'
        //             )
        //         }
        //     }
        // }

        // stage ('Artifactory configuration') {
        //     steps {
        //         script{
        //             rtServer (
        //                 id: 'Artifactory-1',
        //                 url: 'http://localhost:8082/artifactory',
        //                     // If you're using username and password:
        //                 username: 'admin',
        //                 password: 'Password1',
        //                     // If you're using Credentials ID:
        //                     // credentialsId: 'ccrreeddeennttiiaall',
        //                     // If Jenkins is configured to use an http proxy, you can bypass the proxy when using this Artifactory server:
        //                     bypassProxy: false,
        //                     // Configure the connection timeout (in seconds).
        //                     // The default value (if not configured) is 300 seconds:
        //                     timeout: 300
        //             )

        //             rtDownload (
        //                 serverId: 'Artifactory-1',
        //                 spec: '''{
        //                       "files": [
        //                         {
        //                           "pattern": "generic-local/",
        //                           "target": "generic-local/"
        //                         }
        //                       ]
        //                 }''',

        //                 // Optional - Associate the downloaded files with the following custom build name and build number,
        //                 // as build dependencies.
        //                 // If not set, the files will be associated with the default build name and build number (i.e the
        //                 // the Jenkins job name and number).
        //                 buildName: 'holyFrog',
        //                 buildNumber: '42',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'myproject'
        //             )

        //             rtUpload (
        //                 serverId: 'Artifactory-1',
        //                 spec: '''{
        //                       "files": [
        //                         {
        //                           "pattern": "dockerfile",
        //                           "target": "generic-local/"
        //                         }
        //                      ]
        //                 }''',

        //                 // Optional - Associate the uploaded files with the following custom build name and build number,
        //                 // as build artifacts.
        //                 // If not set, the files will be associated with the default build name and build number (i.e the
        //                 // the Jenkins job name and number).
        //                 buildName: 'holyFrog',
        //                 buildNumber: '42',
        //                 // Optional - Only if this build is associated with a project in Artifactory, set the project key as follows.
        //                 project: 'myproject'
        //             )

        //         }
        //     }
        // }

        // stage('Docker Scout CVES') {
        //     steps {
        //         // Install Docker Scout
        //         sh 'curl -sSfL https://raw.githubusercontent.com/docker/scout-cli/main/install.sh | sh -s -- -b /usr/local/bin'

        //         // Log into Docker Hub
        //         // sh 'echo $DOCKER_HUB_PAT | docker login -u $DOCKER_HUB_USER --password-stdin'

        //         // Analyze and fail on critical or high vulnerabilities
        //         sh 'docker scout cves --format sarif --locations --ignore-base --output dockerscout.json nginx'
        //         sh 'docker scout cves --format markdown --locations --ignore-base --output dockerscout.html nginx'

        //     }
        // }

        // stage ('Xray scan') {
        //     steps {
        //         script{
        //             def scanConfig = [
        //                     'buildName'      : buildInfo.name,
        //                     'buildNumber'    : buildInfo.number,
        //                     'failBuild'      : true
        //             ]
        //             def scanResult = server.xrayScan scanConfig
        //             echo scanResult as String
        //         }
        //     }
        // }

        // stage('SonarQube Analysis') {
        //     steps {
        //         script{
        //             withCredentials([string(credentialsId: 'SonarQube-Access-Credentials', variable: 'SONAR_TOKEN')]) {
        //                 sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
        //             }

        //             // def scannerHome = tool 'sonar-scanner';
        //             //     withSonarQubeEnv('LocalSonarQubeServer') { // If you have configured more than one global server connection, you can specify its name
        //             //     sh '/home/tzahi/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner -X -Dsonar.projectKey=localproject -Dsonar.sources=. -Dsonar.css.node=. -Dsonar.host.url=http://localhost:8088'
        //             // }
        //         }
        //     }
        // }

        // stage('GitGuardian Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
        //     // agent {
        //     //     docker { image 'gitguardian/ggshield' }
        //     // }
        //     environment {
        //         GITGUARDIAN_API_KEY = credentials('GitGuardian-Access-Credentials')
        //     }
        //     steps {
        //         script {
        //             try {
        //                 sh(script: 'ggshield secret scan path . --recursive --show-secrets --exit-zero --output=ggshield-secret-report.json --json -y', label:"GitGuardian Files and Folders Scan",returnStdout: false)
        //                 // echo 'GitGuardian docker image scan'
        //                 // sh 'ggshield secret scan docker gitguardian/ggshield --output=ggshield.json --json --show-secrets --exit-zero'
        //                 archiveArtifacts artifacts: 'ggshield-secret-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
        //                 // sh(script: 'ggshield secret scan ci --show-secrets --exit-zero --output=ggshield-ci-report.json --json --debug', label:"GitGuardian CI Scan", returnStdout: false)
        //                 // archiveArtifacts artifacts: 'ggshield-ci-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/

        //             } catch (ERROR) {
        //                 def catchErrorHandling = "${ERROR}"
        //                 if (catchErrorHandling.contains("exit code 1")) {
        //                     sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
        //                     sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
        //                 }
        //                 echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
        //                 currentBuild.result = 'SUCCESS'
        //             } finally {
        //                 echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
        //             }
        //         }

        //     }
        // }

        // stage('GitLeaks Scan') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() } }
        //     steps {
        //         script {
        //             try {

        //                 sh(script: 'gitleaks detect --report-path gitleaks-detect-report.json', returnStdout: false)
        //                 def gitLeaksOutput = sh(script: 'gitleaks detect --baseline-path gitleaks-detect-report.json --report-path gitleaks-detect-findings.json', returnStdout: false)
        //                 // def gitLeaksOutput = sh(script: 'gitleaks detect --report-path=./gitleaks-leaks-report.json', returnStdout: true).trim()
        //                 echo "GitLeaks Scan Output:"
        //                 echo gitLeaksOutput
        //                 // writeFile(file: "gitleaks-detect-report.json", text: gitLeaksOutput, encoding: "UTF-8")
        //                 archiveArtifacts artifacts: 'gitleaks-detect-report.json', allowEmptyArchive: false, onlyIfSuccessful: true // https://www.jenkins.io/doc/pipeline/steps/core/
        //             } catch (ERROR) {
        //                 def catchErrorHandling = "${ERROR}"
        //                 if (catchErrorHandling.contains("exit code 1")) {
        //                     sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
        //                     sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")
        //                 }
        //                 echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
        //             } finally {
        //                 echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
        //             }
        //         }
        //     }
        // }
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
