pipeline {
    agent any

    parameters {
        choice(choices: ['https://github.com/mjhfvi/JobAssignment.git', 'https://github.com/mjhfvi/DockerExamples.git'],name: 'SET_GIT_REPOSITORY_URL', description: 'Choose the Git Repository')
        string(defaultValue: '00', name: 'DOCKER_REPOSITORY_TAG', description: 'set docker tag number')
        booleanParam(defaultValue: false, name: 'STAGE_BUILD_DOCKER_IMAGE', description: 'build docker image')
        booleanParam(defaultValue: false, name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', description: 'use cache when building docker image')
        booleanParam(defaultValue: false, name: 'USE_STAGE_PUSH_DOCKER_IMAGE', description: 'push docker image to docker hub')
        booleanParam(defaultValue: false, name: 'STAGE_SECURITY_TESTS', description: 'test security vulnerabilities in docker image')

    }

    stages {
        stage('Trigger CorePipeline Job') {
            steps {
                script {
                    def downstreamJob = build job: 'CorePipeline',
                        parameters: [
                            string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                            string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                            string(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                            string(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                            string(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                            string(name: 'STAGE_SECURITY_TESTS', value: "$params.STAGE_SECURITY_TESTS")

                        ]
                    echo "CorePipeline job result: ${downstreamJob.result}"
                }
            }
        }
    }
}
