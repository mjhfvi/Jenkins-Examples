pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        choice(choices: ['main', 'release', '1.0'], name: 'SET_GIT_REPOSITORY_BRANCH', description: 'Choose Git Repository Branch')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_INIT', description: 'initializes a working directory containing Terraform configuration files')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_PLAN', description: 'creates an execution plan, which lets you preview the changes that Terraform plans to make to your infrastructure')
        booleanParam( defaultValue: false, name: 'USE_STAGE_TERRAFORM_APPLY', description: 'executes the actions proposed in a Terraform plan')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_BACKUP_STATE_FILE', description: 'JSON formatted mapping of the resources defined in the configuration and those that exist in your infrastructure')
        booleanParam( defaultValue: false, name: 'USE_STAGE_TERRAFORM_DESTROY', description: 'convenient way to destroy all remote objects managed by a particular Terraform configuration.')

    }

    environment {
        USE_STAGE_TERRAFORM_CODE_VALIDATE           = "false"
        // TOOL_TERRAFORM                              = tool name: 'Terraform', type: 'terraform'
        TF_VAR_aws_region                           ="il-central-1"
        TF_VAR_aws_user_name                        ="tzahi.cohen"
        TF_VAR_aws_access_key                       =credentials('Aws-Access-Key')
        TF_VAR_aws_secret_key                       =credentials('Aws-Secret-Key')
        TF_VAR_environment_name                     ="tzahicohen"
        TF_VAR_environment_type                     ="Testing"
        TF_VAR_aws_node_groups_instance_type        ="t3.micro"
        TF_VAR_cluster_name                         ="terraform-eks-demo"
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
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

        stage('Terraform Init') { when { expression { params.USE_STAGE_TERRAFORM_INIT.toBoolean() && StageGitClone == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("jobProject/terraform") {
                                sh 'terraform init'
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
                        StageTerraformInit = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageTerraformInit = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Code Validate') { when { expression { env.USE_STAGE_TERRAFORM_CODE_VALIDATE.toBoolean() && StageTerraformInit == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("jobProject/terraform") {
                                sh 'terraform validate'
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
                        StageTerraformCodeValidate = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageTerraformCodeValidate = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Plan') { when { expression { params.USE_STAGE_TERRAFORM_PLAN.toBoolean() && StageTerraformInit == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("jobProject/terraform") {
                                sh 'terraform plan -out=tfplan'
                                archiveArtifacts artifacts: 'tfplan', fingerprint: true
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
                        StageTerraformPlan = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageTerraformPlan = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Apply') { when { expression { params.USE_STAGE_TERRAFORM_APPLY.toBoolean() && StageTerraformPlan == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 30, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("jobProject/terraform") {
                                sh 'terraform apply -auto-approve tfplan'

                            // write the template output in a file
                            def fileContent = sh(returnStdout: true, script: "terraform output neededForAnsible")
                            writeFile file: "${WORKSPACE}/vars.yaml", text: fileContent

                            echo "Saving Terraform State File to Jenkins Archive Artifacts"
                            "archiveArtifacts artifacts: '*.state', onlyIfSuccessful: true, fingerprint: true"
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
                        StageTerraformApply = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageTerraformApply = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Destroy') { when { expression { params.USE_STAGE_TERRAFORM_DESTROY.toBoolean() && StageTerraformApply == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("jobProject/terraform") {
                                sh 'terraform destroy -auto-approve tfplan'
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
                        StageTerraformApply = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageTerraformApply = 'SUCCESS'
                    }
                }
            }
        }

        stage('Backup Terraform State File') { when { expression { params.USE_STAGE_TERRAFORM_BACKUP_STATE_FILE.toBoolean() && StageTerraformApply == 'SUCCESS' } }
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            sh 'upload to git repo?'

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
                        StageBackupTerraformStateFile = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        StageBackupTerraformStateFile = 'SUCCESS'
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
        }
    }
}
