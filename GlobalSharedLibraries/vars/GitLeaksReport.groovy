def GitLeaksReport() {
    if (isUnix()) {
        def gitleaksOutput = bat(script: './gitleaks --repo-path . --report=/tmp/gitleaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: '/tmp/gitleaks-report.json', allowEmptyArchive: true
    } else {
        def gitleaksOutput = bat(script: './gitleaks --repo-path . --report=/tmp/gitleaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: '/tmp/gitleaks-report.json', allowEmptyArchive: true
    }
}
