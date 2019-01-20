#!/usr/bin/groovy

NODE_LABEL = 'master'
// TODO SP: use the puddle domain names except once the DNS problem is resolved for Mac or when running somewhere else
// DAI_URL = 'http://stefan-puddle-dai-142-cpu-small2-puddle.h2o.ai:12345'
DAI_URL = 'http://3.80.167.234:12345'
DAI_USERNAME = 'h2oai'
DAI_PASSWORD = 'i-0495a5469c1111c0a'

DATA_BUCKET_TRAINING='data-bucket'
DATA_FILE_TRAINING='train.csv'
GIT_REPO = 'https://github.com/stexyz/dai-ci-cd'

// accessing minio by hostname which is a docker container name on the same docker network
MINIO_URL = 'http://minio:9000'
MINIO_ACCESS_KEY = 'accesskey'
MINIO_SECRET_KEY = 'secretkey'
MINIO_MODEL_BUCKET = 'model-bucket'
MINIO_MOJO_OBJECT = 'pipeline.mojo'

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
                        echo "Uploading dataset [${DATA_FILE_TRAINING}] from bucket [${DATA_BUCKET_TRAINING}] to Driverless AI running at ${DAI_URL}."
                    }
                    NEW_DATASET = sh(script: "python3 upload_new_dataset.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${DATA_BUCKET_TRAINING} ${DATA_FILE_TRAINING} ${MINIO_URL} ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY}", returnStdout: true).trim()
                    ansiColor('green') {
                        echo "UpLoading dataset done, dataset key is [${NEW_DATASET}]."
                    }                
                }
            }
        }

        // Run an experiment with the dataset
        stage('run-experiment') {
            agent { label NODE_LABEL }
            steps {
                script {
                    ansiColor('green') {
                        echo "Training a DAI model with 1-1-5 settings on dataset [${NEW_DATASET}]."
                    }
                    EXPERIMENT_NAME = sh(script: "python3 run_experiment.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${NEW_DATASET} 1 1 5", returnStdout: true).trim()
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
                    
                    MOJO_PATH = sh(script: "python3 download_mojo.py ${DAI_URL} ${DAI_USERNAME} ${DAI_PASSWORD} ${EXPERIMENT_NAME}", returnStdout: true).trim()
                    
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
                        echo "Deploying mojo to production."
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