def GitLeaksReport() {
    if (isUnix()) {
        def gitleaksOutput = sh(script: 'gitleaks --repo-path . --report=./gitleaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: './gitleaks-report.json', allowEmptyArchive: true
    } else {
        def gitleaksOutput = bat(script: 'gitleaks --repo-path . --report=./gitleaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: './gitleaks-report.json', allowEmptyArchive: true
    }
}
