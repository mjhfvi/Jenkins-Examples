def GitLeaksReport() {
    if (isUnix()) {
        def gitleaksOutput = sh(script: 'gitleaks detect --report-path=./gitleaks-detect-leaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: 'gitleaks-detect-leaks-report.json', allowEmptyArchive: true
    } else {
        def gitleaksOutput = bat(script: 'gitleaks detect --report-path=./gitleaks-detect-leaks-report.json', returnStdout: true).trim()
        echo "Gitleaks Scan Output:"
        echo gitleaksOutput
        archiveArtifacts artifacts: 'gitleaks-detect-leaks-report.json', allowEmptyArchive: true
    }
}
