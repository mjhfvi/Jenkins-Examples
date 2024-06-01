pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
        string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
        booleanParam(defaultValue: true, name: 'STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
        booleanParam(defaultValue: true, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
        booleanParam(defaultValue: true, name: 'USE_STAGE_SECURITY_TESTS', description: 'test security vulnerabilities in docker image')
        // string(defaultValue: 'linux', name: 'RUN_JOB_NODE_NAME', description: 'set node')
    }

    stages {
        stage('Build Docker Job') {
            steps {
                script {
                    def BUILD_NUMBER = currentBuild.number
                    echo "Current Build number is ${BUILD_NUMBER}"
                    def downstreamJob = build job: 'Storage_Pipelines/Core_Pipeline_Docker',
                        parameters: [
                            string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                            string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                            booleanParam(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                            booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                            booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                            booleanParam(name: 'USE_STAGE_SECURITY_TESTS', value: "$params.USE_STAGE_SECURITY_TESTS"),
                            string(name: 'JOB_BUILD_NUMBER', value: "${BUILD_NUMBER}"),

                        ]
                    echo "Core_Pipeline_Docker job result: ${downstreamJob.result}"
                }
            }
        }
    }
}
