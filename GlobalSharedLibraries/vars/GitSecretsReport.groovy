def GitSecretsReport() {
    if (isUnix()) {
        def secretsOutput = sh(script: 'git secrets --scan -r', returnStdout: true).trim()
        echo "Git Secrets Scan Output:"
        echo secretsOutput
        writeFile file: 'git-secrets-report.txt', text: secretsOutput
        if (secretsOutput) {
        error "Secrets found in repository!"}
    } else {
        def secretsOutput = bat(script: 'git secrets --scan -r', returnStdout: true).trim()
        echo "Git Secrets Scan Output:"
        echo secretsOutput
        writeFile file: 'git-secrets-report.txt', text: secretsOutput
        if (secretsOutput) {
        error "Secrets found in repository!"}
    }
}
