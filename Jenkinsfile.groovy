#!/usr/bin/groovy

NODE_LABEL = 'master'
// TODO: use CLI to get the IP based on instance id; https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instances.html
// or aws ec2 describe-instances --filters 'Name=tag:Name,Values=XXXXXX' --output text --query 'Reservations[].Instances[].[PrivateIpAddress,Tags[?Key==`Name`].Value[]]'
// prob replace PrivateIpAdress with PublicIpAddress

// TODO SP: use the puddle domain names except once the DNS problem is resolved for Mac or when running somewhere else
// DAI_URL = 'http://stefan-puddle-dai-142-cpu-small2-puddle.h2o.ai:12345'
DAI_URL = 'http://18.206.201.110:12345'
DAI_USERNAME = 'h2oai'
DAI_PASSWORD = 'i-0495a5469c1111c0a'
S3_DATA_SET_LOCATION = 'https://s3.amazonaws.com/h2o-public-test-data/smalldata/kaggle/CreditCard/creditcard_train_cat.csv'
GIT_REPO = 'https://github.com/stexyz/dai-ci-cd'

MINIO_URL = 'http://localhost:9000'
MINIO_ACCESS_KEY = 'accesskey'
MINIO_SECRET_KEY = 'secretkey'
MINIO_MODEL_BUCKET = 'model-bucket'
MINIO_MOJO_OBJECT = 'model.mojo'

def NEW_DATASET = null

pipeline {
    // Specify agent on a per stage basis.
    agent none

    // Setup job options.
    options {
        ansiColor('green')
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        // holding last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('clean-and-install-h2oai_client') {
            agent { label NODE_LABEL }
            steps {
                script {
                    // built-in calls in Jenkins
                    deleteDir()

                    // checkout git
                    git "${GIT_REPO}"
                    ansiColor('green') {
                        echo 'Checked out github repo with scripts.'
                    }
                    //wget the python h2o_client and install it
                    //TODO SP: maybe this should also be part of the imports/@Library section; this is, however, more portable
                    // sh "wget ${DAI_URL}/static/h2oai_client-1.4.2-py3-none-any.whl"
                    // sh "/usr/local/bin/pip install h2oai_client-1.4.2-py3-none-any.whl"
                }
            }
        }

        // Upload a dataset to DAI from a well-known localtion (for now in an S3)
        stage('upload-dataset-to-dai') {
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Loading dataset [${S3_DATA_SET_LOCATION}] to Driverless AI running at ${DAI_URL}."
                    }
                    NEW_DATASET = sh(script: "python3 upload_new_dataset.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${S3_DATA_SET_LOCATION}", returnStdout: true).trim()
                }
            }
        }

        // Run an experiment with the dataset
        stage('run-experiment') {
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Training a DAI model with 1-1-10 settings on dataset [${NEW_DATASET}]."
                    }
                    EXPERIMENT_NAME = sh(script: "python3 run_experiment.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${NEW_DATASET} 1 1 10", returnStdout: true).trim()
                    ansiColor('green') {
                        echo "Experiment ${EXPERIMENT_NAME} finished."
                    }
                }
            }
        }

        stage('check-model-score') {
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Validating performance of the model ${EXPERIMENT_NAME}."
                    }
                    def EXPERIMENT_SCORE = sh(script: "python3 check_model_score.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${EXPERIMENT_NAME}", returnStdout: true).trim() as Double
                    if (EXPERIMENT_SCORE <= 0.5){
                        ansiColor('green') {
                            echo "Model score [${EXPERIMENT_SCORE}] was too low, failing pipeline build."
                        }
                        exit 1;
                    }
                    ansiColor('green') {
                        echo "Model score [${EXPERIMENT_SCORE}] was good enough, proceeding with the pipeline."
                    }
                }
            }
        }        

        stage('download-mojo'){
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Downloading mojo for experiment [${EXPERIMENT_NAME}]."
                    }
                    
                    def MOJO_PATH = sh(script: "python3 download_mojo.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${EXPERIMENT_NAME}", returnStdout: true).trim()
                    
                    ansiColor('green') {
                        echo "Mojo zip successfully downloaded at [${MOJO_PATH}]."
                    }
                }
            }
        }

        stage('deploy-mojo'){
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Deploying mojo to production. Mojo path is ${MOJO_PATH}."
                    }

                    def UPLOAD_RESULT = sh(script: "python3 deploy_mojo.py ${MINIO_URL} ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY} \"${MOJO_PATH}\" ${MINIO_MODEL_BUCKET} ${MINIO_MOJO_OBJECT}", returnStdout: true).trim()
                    // TODO: check that the mojo file is really present at the S3 location
                    ansiColor('green') {
                        echo "Mojo successfully deployed to production."
                    }
                }
            }
         }

         stage('upload-experiment-summary'){
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "TODO: upload experiment summary to S3.."
                    }
                }
            }
        }
    }
}