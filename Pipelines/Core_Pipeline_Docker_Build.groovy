pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
        string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
        booleanParam(defaultValue: true, name: 'USE_STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
        booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
        booleanParam(defaultValue: true, name: 'USE_STAGE_SECURITY_TESTS', description: 'test security vulnerabilities in docker image')
        // string(name: 'BUILD_NUMBER', value: "BUILD_NUMBER")
        // string(name: 'RUN_JOB_NODE_NAME', description: 'set node')

    }

    environment {
        GIT_REPOSITORY_URL          = "$params.SET_GIT_REPOSITORY_URL"
        GIT_BRANCH_NAME             = "main"
        DOCKER_FILE                 = "dockerfile"
        DOCKER_REPOSITORY           = "mjhfvi/demo-project"
        DOCKER_REPOSITORY_TAG       = "$params.DOCKER_REPOSITORY_TAG"
        DOCKER_BUILD_FOLDER         = "develop-project/home-task-app-main"
        DOCKER_BUILD_USE_CACHE      = "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"
        DOCKER_FROM_IMAGE           = "python:3.9.18-slim"                      // Change Only if you need to Overwrite Default Image in dockerfile
        STAGE_BUILD_DOCKER_IMAGE    = "$params.USE_STAGE_BUILD_DOCKER_IMAGE"    // "false"
        STAGE_PUSH_DOCKER_IMAGE     = "$params.USE_STAGE_PUSH_DOCKER_IMAGE"     //"false"
        STAGE_SECURITY_TESTS        = "$params.USE_STAGE_SECURITY_TESTS"        // "false"
        // BUILD_NUMBER                = "$params.BUILD_NUMBER"
        // RUN_JOB_NODE_NAME           = "$params.RUN_JOB_NODE_NAME"
        // ARTIFACTORY_SERVER       = "http://localhost:8082"
        RUN_JOB_NODE_NAME           = "$params.RUN_JOB_NODE_NAME"
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    // triggers {
    //     githubPush( // Trigger that runs jobs on push notifications from GitHub.
    //         // branchFilter: 'main',           // Optional, specify branches to trigger on
    //         // triggersByBranch: true          // Trigger separate builds for each branch
    //     )
    //     GenericTrigger (
    //         genericVariables: [ [key: 'ref', value: '$.ref'] ],
    //         token: 'pipeline_token',
    //         causeString: 'Triggered by $.actor.displayName for BitBucket Project $ref from $branch',
    //         printContributedVariables: true,
    //         printPostContent: true
    //     )
    // }

    stages {
        stage('Git Clone') {
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            checkout([$class: 'GitSCM', branches: [[name: "${env.GIT_BRANCH_NAME}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'GitHub-SSH-Access-Credentials-to-JobAssignment-Git', url: "git@github.com:mjhfvi/${env.GIT_REPOSITORY_URL}.git"]]])
                        } catch (ERROR) {
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
                            if (env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE == null))
                                dir("${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_FILE} --memory=100m .")
                                }
                            if (env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE != null))
                                dir("${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_FILE} --build-arg BASE_OS_VERSION=${env.DOCKER_FROM_IMAGE} --memory=100m .")
                                }
                            if (!env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE == null))
                                dir("${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_FILE} --no-cache --memory=100m .")
                                }
                            if (!env.DOCKER_BUILD_USE_CACHE.toBoolean() && (env.DOCKER_FROM_IMAGE != null))
                                dir("${env.DOCKER_BUILD_FOLDER}") {
                                    DOCKER_BUILD_IMAGE = docker.build("${env.DOCKER_REPOSITORY}:${env.DOCKER_REPOSITORY_TAG}", "-f ${env.DOCKER_FILE} --build-arg BASE_OS_VERSION=${env.DOCKER_FROM_IMAGE} --no-cache --memory=100m .")
                                }
                            // else
                            //     echo "docker build failed, dont know what to do!"
                        } catch (ERROR) {
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
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        BuildDockerImage = 'SUCCESS'
                    }
                }
            }
    }

        stage('Push Docker Image') { when { expression { env.STAGE_PUSH_DOCKER_IMAGE.toBoolean() && BuildDockerImage == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 20, unit: 'MINUTES') {
                    print("Docker Image Push")
                    script {
                        try {
                            script {
                                DOCKER_BUILD_IMAGE.push()
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
            post {
                failure {
                    script{
                        echo "\033[41m\033[97m\033[1mThe ${env.STAGE_NAME} Build is a Failure, Sending Notifications\033[0m"
                        TestDockerImage = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TestDockerImage = 'SUCCESS'
                    }
                }
            }
        }

        stage ('Security Tests') { when { expression { env.STAGE_SECURITY_TESTS.toBoolean() && BuildDockerImage == 'SUCCESS'  } }
            steps {
                script{
                    def RUN_JOB_NODE_NAME = "${NODE_NAME}"
                    echo "Passing Working Node to Downstream: ${RUN_JOB_NODE_NAME}"

                    def CHANGE_BUILD_NUMBER = "${params.JOB_BUILD_NUMBER}"
                    currentBuild.displayName = "#${CHANGE_BUILD_NUMBER}"

                    def downstreamJob = build job: 'Storage_Pipelines/Core_Pipeline_Security',
                        parameters: [
                            // string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                            string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                            string(name: 'DOCKER_BUILD_IMAGE', value: "$env.DOCKER_BUILD_IMAGE"),
                            // booleanParam(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                            // booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                            // booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                            booleanParam(name: 'USE_STAGE_SECURITY_TESTS', value: "$params.USE_STAGE_SECURITY_TESTS"),
                            string(name: 'RUN_JOB_NODE_NAME', value: "${RUN_JOB_NODE_NAME}"),
                            string(name: 'CHANGE_BUILD_NUMBER', value: "${CHANGE_BUILD_NUMBER}")
                        ]
                    echo "Core_Pipeline_Security job result: ${downstreamJob.result}"
                }
            }
        }

        // stage ('Artifactory Publish Build Info') { when { expression { env.STAGE_PUBLISH_BUILD_ARTIFACTORY_INFO.toBoolean() } }
        //     agent { label "${env.RUN_JOB_NODE_NAME}" }
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

        // stage ('Artifactory configuration') { when { expression { env.STAGE_PUSH_IMAGE_TO_ARTIFACTORY.toBoolean() } }
        //     agent { label "${env.RUN_JOB_NODE_NAME}" }
        //     steps {
        //         script{
        //             rtServer (
        //                 id: 'Artifactory-1',
        //                 url: 'http://localhost:8082/artifactory',
        //                     // If you're using username and password:
        //                 username: 'admin',
        //                 password: '',
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
        // cleanWs() // Clean workspace after each run
        // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}
