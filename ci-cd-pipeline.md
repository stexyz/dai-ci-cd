# CI/CD Pipeline
## Customers Using That:
* Booking
* TenneT
* Karthig's prospect

https://blog.statsbot.co/machine-learning-devops-611210393c1a

https://d1.awsstatic.com/Projects/P5505030/aws-project_Jenkins-build-server.pdf

## Steps

### Set up Jenkins in docker
* Use jenkins/jenkins:lts
* Install recommended plugins
* Install ansi-color plugin
* Install virtualenv

* dependencies inside docker container (later put into dockerfile)
	* Install Python3 (for DAI client)
	* Install pip3 on jenkins os-level; (python3-pip)
	  * https://github.com/xmartlabs/docker-jenkins-android/issues/5
	  * https://pip.pypa.io/en/latest/installing/#install-or-upgrade-pip
	  * **TODO:** add it to dockerfile later.
	* Install h2oai_client wheel
	  * **TODO:** install this to virtualenv from DAI server
	* Install boto3 (s3/minio client)
	```
	apt-get update
	apt install python3
	apt install python3-pip
	wget http://18.206.201.110:12345/static/h2oai_client-1.4.2-py3-none-any.whl
	pip3 install h2oai_client-1.4.2-py3-none-any.whl
	pip3 install boto3
	```

### Setup new color schema for ANSI color 
Used in Jenkinsfile.groovy, need to setup a schema named 'green'.

### Import pipeline
`java -jar jenkins_home/war/jenkins-cli.jar -s http://localhost:8080/ -auth h2oai:11374f4488d177892507207ae563f9377d create-job dai-ci-cd-pipeline < git-root/jenkins_pipeline/pipeline-backup/config.xml`

Get the API code here: http://localhost:8080/user/h2oai/configure by generating a new one.

### Import AnsiColor settings
* location `JENKINS_HOME/hudson.plugins.ansicolor.AnsiColorBuildWrapper.xml`

### Create a docker network to link Jenkins and Minio
```
docker network create cicd-demo-network
docker network connect cicd-demo-network dai-ci-cd-jenkins
docker network connect cicd-demo-network minio
```

### Storage for mojo files
Will aim to use AWS S3, but for demo purposes will use Minio, which is a free version of S3 running in Docker.
```
import boto3
s3 = boto3.client('s3',
                  endpoint_url='http://localhost:9000',
                  aws_access_key_id='accesskey',
                  aws_secret_access_key='secretkey',
                  region_name='us-east-1')
```

Need to create a bucket named `model-bucket` into which we will be uploading new mojo files and experiment summaries.

### Create Website
* [SpringBoot Thymeleaf Templates]( https://www.thymeleaf.org/doc/articles/standardurlsyntax.html)
* [RMSE](https://github.com/haifengl/smile/blob/master/core/src/main/java/smile/validation/RMSE.java)

## Ideas

### Problems
* waiting for experiment to finish creates a blocking step;
  * can do polling from master and repeatedly check for experiment status and sleep; MR thinks this is overkill
  * consume an executor in a sync-fashion (will do now)
* with a new DAI being deployed on demand, can I insert license using python client
* DAI should have a version-agnostic endpoint for python client too; not just sth like `http://34.229.38.81:12345/static/h2oai_client-1.4.2-py3-none-any.whl`

### Future
* on-demand DAI provisioning before experiment (a la puddle)
* Use test dataset
* version mojo files (don't replace)
* implement model retraining (topping-up)
* different triggers
  * 

## How-to Jenkins
* `docker exec -u 0 -ti jenkins bash` gets the root console


### Read output from the shell command

```
def result = sh(script: 'asdasda', returnStdout: true).trim()
def restultCode = sh(script: 'asdasd...', retrunCode: true)
```

### Misc
https://s3.amazonaws.com/h2o-public-test-data/smalldata/kaggle/CreditCard/creditcard_train_cat.csv

18.212.156.172:12345

jenkins password: changeit123
jenkins username: h2oai

