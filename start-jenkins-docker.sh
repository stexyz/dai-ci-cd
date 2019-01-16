docker run --name dai-ci-cd-jenkins --rm -p 8080:8080 -p 50000:50000 -p 9000:9000 -v "/tmp/jenkins_home/":"/var/jenkins_home" jenkins/jenkins:lts

# check if --rm should be kept after debugging