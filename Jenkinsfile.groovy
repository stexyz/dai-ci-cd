#!/usr/bin/groovy

NODE_LABEL = 'master'
// TODO: use CLI to get the IP based on instance id; https://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instances.html
// or aws ec2 describe-instances --filters 'Name=tag:Name,Values=XXXXXX' --output text --query 'Reservations[].Instances[].[PrivateIpAddress,Tags[?Key==`Name`].Value[]]'
// prob replace PrivateIpAdress with PublicIpAddress
DAI_URL = 'http://54.81.209.97:12345'
S3_DATA_SET_LOCATION = 'https://s3.amazonaws.com/h2o-public-test-data/smalldata/kaggle/CreditCard/creditcard_train_cat.csv'
GIT_REPO = 'https://github.com/stexyz/dai-ci-cd'
def NEW_DATASET = null

pipeline {
    // Specify agent on a per stage basis.
    agent none

    // Setup job options.
    options {
        ansiColor('xterm')
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
                    echo 'Checked out github repo with scripts.'
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
                    echo "Loading dataset [${S3_DATA_SET_LOCATION}] to Driverless AI running at ${DAI_URL}."
                    NEW_DATASET = sh(script: "python3 upload_new_dataset.py ${DAI_URL} ${S3_DATA_SET_LOCATION}", returnStdout: true).trim()
                }
            }
        }

        // Run an experiment with the dataset
        stage('run-experiment') {
            agent { label NODE_LABEL }
            steps {
                script {
                    echo "Training a DAI model with 1-1-10 settings on dataset [${NEW_DATASET}]."
                    EXPERIMENT_NAME = sh(script: "python3 run_experiment.py ${DAI_URL} ${NEW_DATASET} 1 1 10", returnStdout: true).trim()
                    echo "Experiment ${EXPERIMENT_NAME} finished."
                }
            }
        }

        stage('check-model-score') {
            agent { label NODE_LABEL }
            steps {
                script {
                    echo "Validating performance of the model ${EXPERIMENT_NAME}."
                    def EXPERIMENT_SCORE = sh(script: "python3 check_model_score.py ${DAI_URL} ${EXPERIMENT_NAME}", returnStdout: true).trim() as Double
                    if (EXPERIMENT_SCORE <= 0.5){
                        echo "Model score [${EXPERIMENT_SCORE}] was too low, failing pipeline build."
                        exit 1;
                    }
                    echo "Model score [${EXPERIMENT_SCORE}] was good enough, proceeding with the pipeline."
                }
            }

        }        
    }
}