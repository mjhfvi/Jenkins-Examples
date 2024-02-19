def NOTIFY_TEAMS (COLOR) {
    office365ConnectorSend (
        status: "Pipeline Status",
        webhookUrl: "${env.TEAMS_URL}",
        color: '${COLOR}',
        message: "Test Successful: ${JOB_NAME} - ${BUILD_DISPLAY_NAME}<br>Pipeline duration: ${currentBuild.durationString}"
    )
}
