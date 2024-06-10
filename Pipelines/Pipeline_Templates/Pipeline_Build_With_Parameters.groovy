def setDescription() {
    def item = Jenkins.instance.getItemByFullName(env.JOB_NAME)
    item.setDescription(
        """This is a Master Pipeline to Run Other Docker Jobs
Use it when Building a Docker Image with Parameters""")
    item.save()
}
setDescription()

pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'Set Docker Tag Number')
        booleanParam(defaultValue: true, name: 'STAGE_BUILD_DOCKER_IMAGE', description: 'Build Docker Image')
        booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'Use Cache When Building Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_CODE_VALIDATION', description: 'Test Code Validation')
        booleanParam(defaultValue: false, name: 'USE_STAGE_CVE_TESTS', description: 'Test Security Vulnerability Exploits For Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_SECRET_LEAKS', description: 'Search Secret Leaks in code')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'Push Docker Image to Repository')
        booleanParam(defaultValue: false, name: 'USE_STAGE_DEPLOY_TO_ENVIRONMENT', description: 'Deploy to Environment')
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Build Docker Job') {
            steps {
                script {
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                        def BUILD_NUMBER = currentBuild.number
                        echo "Passing Current Build Number to DownStream: ${BUILD_NUMBER}"

                        def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_Build',
                            parameters: [
                                string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                                string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                                string(name: 'JOB_BUILD_NUMBER', value: "${BUILD_NUMBER}"),
                                booleanParam(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                                booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                                booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                                booleanParam(name: 'USE_STAGE_DEPLOY_TO_ENVIRONMENT', value: "$params.USE_STAGE_DEPLOY_TO_ENVIRONMENT"),
                                booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "$params.USE_STAGE_CVE_TESTS"),
                                booleanParam(name: 'USE_STAGE_CODE_VALIDATION', value: "$params.USE_STAGE_CODE_VALIDATION"),
                                booleanParam(name: 'USE_STAGE_SECRET_LEAKS', value: "$params.USE_STAGE_SECRET_LEAKS")
                            ]
                        echo "Templates_Pipeline_Docker_Build job result: ${downstreamJob.result}"
                    } catch (Exception ERROR) {
                        def catchErrorHandling = "${ERROR}"
                        if (catchErrorHandling.contains("exit code 1")) {
                            sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                            sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")}
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
