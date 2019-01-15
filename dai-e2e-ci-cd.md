# DAI Process E2E

## Create Pipeline
1. New Item -> Pipeline; specify name and create
1. Pipeline -> Definition select `Pipeline script from SCM`
1. SCM -> Git
1. Repository URL : `https://github.com/stexyz/dai-ci-cd`
1. Script Path: `Jenkinsfile.groovy`

## Pipeline Steps
1. [future] Pipeline trigger - data checkout/chron
	https://stackoverflow.com/questions/45585748/trigger-jenkins-job-when-a-s3-file-is-updated

1. Clean workspace
2. [PyClient] Prepare and run experiment
	1. [Optional] Checkout settings for this pipeline from GIT. 
	2. Load data into DAI
	3. Set experiment params
	4. Run experiment
3. Smart waiting on eof experiment - keep checking on experiment status? ideal would be to register for some call-back
4. Get experiment results, assert that minimal score levels are reached (=model is performing well)
5. Deployment
	1. Build mojo, download mojo, deploy to 
6. Archive artifacts (mojo, py scoring pipeline, DAI logs, autodoc) to Minio (easier than S3)
7. Have locally running web app download mojo from local Minio when doing predictions


http://docs.h2o.ai/driverless-ai/latest-stable/docs/userguide/notifications.html




## Feedback
* missing metadata in mojo (version, dataset hash)