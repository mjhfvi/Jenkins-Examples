# Source: https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/client-and-managed-controllers/how-to-create-a-job-using-the-rest-api-and-curl
# https://stackoverflow.com/questions/15909650/create-jobs-and-execute-them-in-jenkins-using-rest


adding new job using api requests:

set environments variables
set URL=jenkins.localhost
set USER=admin
set PASSWORD=
set JOBNAME=Pipeline_Build_Docker_API_Testing
set CONFIGFILE=config.xml


run API request
curl -X POST -H "Content-Type:application/xml" -d @%CONFIGFILE% "http://%URL%/createItem?name=%JOBNAME%" -u %USER%:%PASSWORD%



curl -s -X POST 'https://example.com/createItem?name=yourJobName' -u username:API_TOKEN --data-binary @mylocalconfig.xml -H "Content-Type:text/xml"
