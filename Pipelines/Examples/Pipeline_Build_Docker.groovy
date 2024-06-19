pipeline {
    agent { label 'linux' }

    parameters {
        // separator(name: "GIT", sectionHeader: "Run Git Logic")
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        choice(choices: ['main', 'release', '1.0'], name: 'SET_GIT_REPOSITORY_BRANCH', description: 'Choose Git Repository Branch')
        separator(name: "DOCKER", sectionHeader: "Run Docker  Steps")
        string(defaultValue: '00', name: 'SET_DOCKER_REPOSITORY_TAG', description: 'Set Docker Tag Number')
        string(defaultValue: '', name: 'DOCKER_FROM_IMAGE', description: 'Choose Docker Base Image - use only for tag testing, otherwise update the dockerfile')
        booleanParam(defaultValue: true, name: 'USE_STAGE_BUILD_DOCKER_IMAGE', description: 'Build Docker Image')
        // booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'Use Cache When Building Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_DOCKER_UNITEST', description: 'Run Unitest for Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_DOCKER_CVE_SCAN', description: 'Test Security Vulnerability Exploits For Docker Image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'Push Docker Image to Repository')
        booleanParam(defaultValue: false, name: 'USE_STAGE_CONTINUOUS_DEPLOYMENT', description: 'Deploy Docker Images to Environment')
        separator(name: "CODE", sectionHeader: "Run Security & Code Checks Steps")
        booleanParam(defaultValue: false, name: 'USE_STAGE_CODE_VALIDATION', description: 'Test Code Validation')
        booleanParam(defaultValue: false, name: 'USE_STAGE_SECRET_LEAKS', description: 'Search Secret Leaks in code')
        separator(name: "CLEANUP", sectionHeader: "Run Cleanup Step")
        booleanParam(defaultValue: false, name: 'RUN_CLEANUP', description: 'Cleanup Temporary Jenkins Files')
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Build Job') {
            steps {
                script {
                    try {
                        echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"
                        def BUILD_NUMBER = currentBuild.number
                        echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Git_Code'
Build Number: '${BUILD_NUMBER}'
Repository: '${params.SET_GIT_REPOSITORY_URL}'
Branch: '${params.SET_GIT_REPOSITORY_BRANCH}'
Working Node: '${NODE_NAME}'
Working Workspace: '${WORKSPACE}'
=============================================================
"""

                        def downstreamJob = build job: 'Pipeline_Templates/Templates_Pipeline_Git_Code',
                            parameters: [
                                string( name: 'JOB_NODE_NAME', value: "${params.NODE_NAME}"),
                                string( name: 'JOB_WORKSPACE', value: "${params.WORKSPACE}"),
                                string( name: 'SET_GIT_REPOSITORY_URL', value: "${params.SET_GIT_REPOSITORY_URL}"),
                                string( name: 'SET_GIT_REPOSITORY_BRANCH', value: "${params.SET_GIT_REPOSITORY_BRANCH}"),
                                string( name: 'SET_DOCKER_REPOSITORY_TAG', value: "${params.SET_DOCKER_REPOSITORY_TAG}"),
                                string( name: 'JOB_BUILD_NUMBER', value: "${BUILD_NUMBER}"),
                                string( name: 'DOCKER_FROM_IMAGE', value: "${params.DOCKER_FROM_IMAGE}"),
                                booleanParam( name: 'USE_STAGE_BUILD_DOCKER_IMAGE', value: "${params.USE_STAGE_BUILD_DOCKER_IMAGE}"),
                                booleanParam( name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "${params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE}"),
                                booleanParam( name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "${params.USE_STAGE_PUSH_DOCKER_IMAGE}"),
                                booleanParam( name: 'USE_STAGE_DOCKER_UNITEST', value: "${params.USE_STAGE_DOCKER_UNITEST}"),
                                booleanParam( name: 'USE_STAGE_CONTINUOUS_DEPLOYMENT', value: "${params.USE_STAGE_CONTINUOUS_DEPLOYMENT}"),
                                booleanParam( name: 'USE_STAGE_CVE_TESTS', value: "${params.USE_STAGE_CVE_TESTS}"),
                                booleanParam( name: 'USE_STAGE_CODE_VALIDATION', value: "${params.USE_STAGE_CODE_VALIDATION}"),
                                booleanParam( name: 'USE_STAGE_SECRET_LEAKS', value: "${params.USE_STAGE_SECRET_LEAKS}"),
                                booleanParam( name: 'USE_STAGE_DOCKER_CVE_SCAN', value: "${params.USE_STAGE_DOCKER_CVE_SCAN}")
                            ]
                        echo "Templates_Pipeline_Git_Code job result: ${downstreamJob.result}"
                    } catch (Exception ERROR) {
                        echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                        def catchErrorHandling = "${ERROR}"
                        if (catchErrorHandling.contains("exit code 1")) {
                            sh ("echo \033[41m\033[97m\033[1mGot Error: ${catchErrorHandling}\033[0m")
                            sh ("echo \033[41m\033[97m\033[1mSending Email to Admin\033[0m")}
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
                        BuildJob = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        BuildJob = 'SUCCESS'
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
            cleanWs() // Clean workspace after each run
            // buildDescription 'Build Time: ${BUILD_NUMBER}'
        }
    }
}

def setDescription() {
    def item = Jenkins.instance.getItemByFullName(env.JOB_NAME)
    item.setDescription(
        """This is a Master Pipeline to Run Other Multiple Jobs
Use it when Building with Parameters""")
    item.save()
}
setDescription()
