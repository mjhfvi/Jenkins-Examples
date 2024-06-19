pipeline {
    agent { label 'linux' }

    environment {
        USE_STAGE_CONTINUOUS_DEPLOYMENT       = "true"
    }

    options {
        timeout(time: 10, unit: 'MINUTES')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
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
        stage('Kubernetes') { when { expression { StageGitClone == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 20, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo "building ArgoCD Application for kubernetes"
                            dir("argocd"){
                                sh ("kustomize build --enable-helm | kubectl apply -f -")
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
                        StageKubernetes = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageKubernetes = 'SUCCESS'
                    }
                }
            }
        }

        stage('ArgoCD') { when { expression { StageGitClone == 'SUCCESS' && StageKubernetes == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 20, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo "Getting ArgoCD Admin Password"
                            def ARGOCD_PASSWORD = sh(script: "kubectl -n argo-cd get secret argocd-initial-admin-secret -o jsonpath={.data.password} | base64 -d", returnStdout: true)

                            maskPasswords(varPasswordPairs: [[password: ARGOCD_PASSWORD, var: 'IGNORE']]) {
                                sh label: "Masking Password", script: "ArgoCD Password ${ARGOCD_PASSWORD}"
                            }

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
                        StageArgoCD = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageArgoCD = 'SUCCESS'
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
        }
    }
}
