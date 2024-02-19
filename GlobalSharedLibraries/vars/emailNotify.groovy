def NOTIFY_EMAIL (EMAIL_ADDRESS, TEST_RESULT) {
    emailext (
        to: "${EMAIL_ADDRESS}",
        subject: "Job Result: Job '${env.JOB_NAME} - Build Number [${env.BUILD_NUMBER}]' - Status '${TEST_RESULT}'",
        body: """<p>Job Result: Job '${env.JOB_NAME} - Build Number [${env.BUILD_NUMBER}]' - Status '${TEST_RESULT}':</p>
            <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
        )
}