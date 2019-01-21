docker run --name dai-ci-cd-jenkins --rm -p 8080:8080 -p 50000:50000 -v "/tmp/jenkins_home/":"/var/jenkins_home" jenkins/h2o:v2

# check if --rm should be kept after debugging