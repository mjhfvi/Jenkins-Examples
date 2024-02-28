pipeline {
    agent { label 'windows' }

    environment {
        NODEJS = tool name: 'NodeJS', type: 'com.cloudbees.jenkins.plugins.customtools.CustomTool'
        GITWINDOWS = tool name: 'Git-Windows', type: 'git'
        // gitlinux = tool name: 'Git-Linux', type: 'git'
    }

    stages {
        stage('Git Clone') {
            steps {
                timeout(activity: true, time: 5) {
                    script {
                        try {
                            git branch: 'main', credentialsId: 'GitHub-Access-Credentials', url: "https://github.com/mjhfvi/DockerExamples.git"
                        } catch (ERROR) {
                            echo "\033[41m\033[97m\033[1mStep ${env.STAGE_NAME} Failed: ${ERROR}\033[0m"
                            currentBuild.result = 'FAILURE'
                        } finally {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Done =====================\033[0m"
                        }
                    }
                }
            }
        }
        stage('Hello') {
            steps {
                echo 'Hello World'
                bat "${env.GITWINDOWS}\\git -v"
                bat "${env.NODEJS}\\node -v"
            }
        }
    }
}
