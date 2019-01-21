# run after having run both jenkins and minio containers
docker network create cicd-demo-network
docker network connect cicd-demo-network dai-ci-cd-jenkins
docker network connect cicd-demo-network minio