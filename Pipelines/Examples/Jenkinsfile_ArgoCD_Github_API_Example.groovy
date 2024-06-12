pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "your-docker-repo/your-image"
        GIT_REPO = "your-org/your-helm-chart-repo"
        GIT_BRANCH = "main"
        HELM_VALUES_FILE = "path/to/values.yaml"
        GITHUB_TOKEN = credentials('github-token')  // Assuming you've stored your GitHub token in Jenkins credentials
    }

    stages {
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${env.GIT_COMMIT}")
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://your-docker-registry', 'docker-credentials-id') {
                        docker.image("${DOCKER_IMAGE}:${env.GIT_COMMIT}").push()
                    }
                }
            }
        }

        stage('Update Helm Chart on GitHub') {
            steps {
                script {
                    def newTag = "${env.GIT_COMMIT}"
                    def filePath = "${HELM_VALUES_FILE}"

                    // Fetch the file from GitHub
                    def fileContent = sh(
                        script: "curl -H 'Authorization: token ${env.GITHUB_TOKEN}' https://api.github.com/repos/${GIT_REPO}/contents/${filePath}?ref=${GIT_BRANCH}",
                        returnStdout: true
                    ).trim()
                    def json = new groovy.json.JsonSlurper().parseText(fileContent)
                    def encodedContent = json.content
                    def sha = json.sha

                    // Decode the content
                    def content = new String(encodedContent.decodeBase64())

                    // Update the image tag in the content
                    content = content.replaceAll(/imageTag: .*/, "imageTag: ${newTag}")

                    // Encode the updated content
                    def updatedContent = content.bytes.encodeBase64().toString()

                    // Prepare the JSON payload
                    def payload = [
                        message: "Update Docker image tag to ${newTag}",
                        content: updatedContent,
                        sha: sha,
                        branch: GIT_BRANCH
                    ]
                    def payloadJson = new groovy.json.JsonBuilder(payload).toString()

                    // Update the file on GitHub
                    sh "curl -X PUT -H 'Authorization: token ${env.GITHUB_TOKEN}' -H 'Content-Type: application/json' -d '${payloadJson}' https://api.github.com/repos/${GIT_REPO}/contents/${filePath}"
                }
            }
        }

        stage('Trigger ArgoCD Sync') {
            steps {
                script {
                    // Assuming ArgoCD CLI is installed and configured in Jenkins
                    sh "argocd app sync your-argo-app"
                }
            }
        }
    }
}
