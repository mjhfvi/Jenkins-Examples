// install scriptler
// https://plugins.jenkins.io/scriptler/
// add Active Choices scripts, more examples: https://github.com/jenkinsci/jenkins-scripts/tree/master/scriptler
//

properties([
  parameters([
    [
      $class: 'ChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      name: 'Environment',
      script: [
        $class: 'ScriptlerScript',
        scriptlerScriptId:'Environments.groovy'
      ]
    ],
    [
      $class: 'CascadeChoiceParameter',
      choiceType: 'PT_SINGLE_SELECT',
      name: 'Host',
      referencedParameters: 'Environment',
      script: [
        $class: 'ScriptlerScript',
        scriptlerScriptId:'HostsInEnv.groovy',
        parameters: [
          [name:'Environment', value: '$Environment']
        ]
      ]
   ]
 ])
])

pipeline {
    agent { label 'linux' }

    // parameters {
    //     choices: ['TESTING', 'STAGING', 'PRODUCTION']
    // }

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

                        // def downstreamJob = build job: 'Storage_Pipelines/Templates_Pipeline_Docker_Build',
                        //     parameters: [
                        //         string(name: 'SET_GIT_REPOSITORY_URL', value: "$params.SET_GIT_REPOSITORY_URL"),
                        //         string(name: 'DOCKER_REPOSITORY_TAG', value: "$params.DOCKER_REPOSITORY_TAG"),
                        //         string(name: 'JOB_BUILD_NUMBER', value: "${BUILD_NUMBER}"),
                        //         booleanParam(name: 'STAGE_BUILD_DOCKER_IMAGE', value: "$params.STAGE_BUILD_DOCKER_IMAGE"),
                        //         booleanParam(name: 'USE_CACHE_FOR_DOCKER_BUILD_IMAGE', value: "$params.USE_CACHE_FOR_DOCKER_BUILD_IMAGE"),
                        //         booleanParam(name: 'USE_STAGE_PUSH_DOCKER_IMAGE', value: "$params.USE_STAGE_PUSH_DOCKER_IMAGE"),
                        //         booleanParam(name: 'USE_STAGE_DEPLOY_TO_ENVIRONMENT', value: "$params.USE_STAGE_DEPLOY_TO_ENVIRONMENT"),
                        //         booleanParam(name: 'USE_STAGE_CVE_TESTS', value: "$params.USE_STAGE_CVE_TESTS"),
                        //         booleanParam(name: 'USE_STAGE_CODE_VALIDATION', value: "$params.USE_STAGE_CODE_VALIDATION"),
                        //         booleanParam(name: 'USE_STAGE_SECRET_LEAKS', value: "$params.USE_STAGE_SECRET_LEAKS")
                        //     ]
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
}
