pipeline {
    agent { label 'linux' }

    parameters {
        choice(choices: ['JobAssignment', 'DockerExamples'], name: 'SET_GIT_REPOSITORY_URL', description: 'Choose Git Repository')
        choice(choices: ['main', 'release', '1.0'], name: 'SET_GIT_REPOSITORY_BRANCH', description: 'Choose Git Repository Branch')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_INIT', description: 'initializes a working directory containing Terraform configuration files')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_PLAN', description: 'creates an execution plan, which lets you preview the changes that Terraform plans to make to your infrastructure')
        booleanParam( defaultValue: false, name: 'USE_STAGE_TERRAFORM_APPLY', description: 'executes the actions proposed in a Terraform plan')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_BACKUP_STATE_FILE', description: 'JSON formatted mapping of the resources defined in the configuration and those that exist in your infrastructure')
        booleanParam( defaultValue: true, name: 'USE_STAGE_TERRAFORM_DESTROY', description: 'convenient way to destroy all remote objects managed by a particular Terraform configuration.')

    }

    environment {
        USE_STAGE_GIT                               = "true"
        USE_STAGE_TERRAFORM_CODE_VALIDATE           = "true"
        USE_STAGE_TERRAFORM_INIT                    = "true"
        USE_STAGE_TERRAFORM_PLAN                    = "true"
        USE_STAGE_TERRAFORM_APPLY                   = "false"
        USE_STAGE_TERRAFORM_BACKUP_STATE_FILE       = "true"
        USE_STAGE_TERRAFORM_DESTROY                 = "true"
        TOOL_TERRAFORM                              = tool name: 'Terraform', type: 'terraform'
        TF_VAR_aws_region                           ="il-central-1"
        TF_VAR_aws_user_name                        ="tzahi.cohen"
        TF_VAR_aws_access_key                       =""
        TF_VAR_aws_secret_key                       =""
        TF_VAR_environment_name                     ="tzahicohen"
        TF_VAR_environment_type                     ="Testing"
        TF_VAR_aws_node_groups_instance_type        ="t3.micro"
        TF_VAR_cluster_name                         ="terraform-eks-demo"
    }

    options {
        timeout(time: 1, unit: 'HOURS')                                 // Overall Time for the Build to Run
        skipStagesAfterUnstable()
        ansiColor('xterm')
    }

    stages {
        stage('Git') { when { expression { env.USE_STAGE_GIT.toBoolean() } }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            echo """
=============================================================
Passing Variables to DownStream: 'Templates_Pipeline_Git_Code'
Repository: '${params.SET_GIT_REPOSITORY_URL}'
Branch: '${params.SET_GIT_REPOSITORY_BRANCH}'
=============================================================
"""

                        def downstreamJob = build job: 'Pipeline_Templates/Templates_Pipeline_Git_Code',
                            parameters: [
                                string( name: 'SET_GIT_REPOSITORY_URL', value: "${params.SET_GIT_REPOSITORY_URL}"),
                                string( name: 'SET_GIT_REPOSITORY_BRANCH', value: "${params.SET_GIT_REPOSITORY_BRANCH}"),
                            ]
                            echo """
=============================================================
Getting Variables to DownStream: 'Templates_Pipeline_Git_Code'
Working Node: '${downstreamJob.buildVariables.NODE_NAME}'
Working Workspace: '${downstreamJob.buildVariables.WORKSPACE}'
Committer Email Address: '${downstreamJob.buildVariables.GIT_COMMIT_EMAIL}'
Committer Full Name: '${downstreamJob.buildVariables.GIT_COMMIT_FULL_NAME}'
=============================================================
"""
                        // echo "Templates_Pipeline_Git_Code job result: ${downstreamJob.result}"
                        // echo "The returned value from the triggered job was ${downstreamJob.buildVariables.GIT_COMMIT_EMAIL}"
                        // echo "The returned value from the triggered job was ${downstreamJob.buildVariables.GIT_COMMIT_FULL_NAME}"
                        // echo "The returned value from the triggered job was ${downstreamJob.buildVariables.NODE_NAME}"
                        // echo "The returned value from the triggered job was ${downstreamJob.buildVariables.WORKSPACE}"

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
                        GitClone = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        GitClone = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Init') { when { expression { params.USE_STAGE_TERRAFORM_INIT.toBoolean() && GitClone == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("terraform") {
                                sh '${TOOL_TERRAFORM}/terraform init'
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
                        TerraformInit = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TerraformInit = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Code Validate') { when { expression { params.USE_STAGE_TERRAFORM_CODE_VALIDATE.toBoolean() && TerraformInit == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 5, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("terraform") {
                                sh '${TOOL_TERRAFORM}/terraform validate'
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
                        TerraformCodeValidate = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TerraformCodeValidate = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Plan') { when { expression { params.USE_STAGE_TERRAFORM_PLAN.toBoolean() && TerraformInit == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("terraform") {
                                sh '${TOOL_TERRAFORM}/terraform plan -out=tfplan'
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
                        TerraformPlan = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TerraformPlan = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Apply') { when { expression { params.USE_STAGE_TERRAFORM_APPLY.toBoolean() && TerraformPlan == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 30, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("terraform") {
                                sh '${TOOL_TERRAFORM}/terraform apply -auto-approve tfplan'

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
                        TerraformApply = 'FAILURE'
                    }
                }
                success {
                    script{
                        echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TerraformApply = 'SUCCESS'
                    }
                }
            }
        }

        stage('Terraform Destroy') { when { expression { params.USE_STAGE_TERRAFORM_DESTROY.toBoolean() && TerraformPlan == 'SUCCESS' } }
            agent {
                node {
                    label "${params.RUN_JOB_NODE_NAME}"
                    customWorkspace "${params.JOB_WORKSPACE}"
                    }
            }
            steps {
                timeout(activity: true, time: 10, unit: 'MINUTES') {
                    script {
                        try {
                            echo "\033[42m\033[97m\033[1m ===================== Step ${env.STAGE_NAME} Started =====================\033[0m"

                            dir ("terraform") {
                                sh '${TOOL_TERRAFORM}/terraform destroy -auto-approve tfplan'
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
                        TerraformApply = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        TerraformApply = 'SUCCESS'
                    }
                }
            }
        }

        stage('Backup Terraform State File') { when { expression { params.USE_STAGE_TERRAFORM_BACKUP_STATE_FILE.toBoolean() && TerraformApply == 'SUCCESS' } }
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
                        BackupTerraformStateFile = 'FAILURE'
                    }
                }
                success {
                    script{
                        // echo "\033[42m\033[97mThe ${env.STAGE_NAME} Build is Successfully, Sending Notifications\033[0m"
                        BackupTerraformStateFile = 'SUCCESS'
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
