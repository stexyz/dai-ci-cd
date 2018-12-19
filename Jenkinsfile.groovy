#!/usr/bin/groovy

NODE_LABEL = 'master'
DAI_URL = 'http://34.229.38.81:12345'
S3_DATA_SET_LOCATION = 'https://s3.amazonaws.com/h2o-public-test-data/smalldata/kaggle/CreditCard/creditcard_train_cat.csv'
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
                    git ' https://github.com/stexyz/dai-ci-cd'

                    //wget the python h2o_client and install it
                    //TODO SP: maybe this should also be part of the imports/@Library section; this is, however, more portable
                    sh "wget ${DAI_URL}/static/h2oai_client-1.4.2-py3-none-any.whl"
                    sh "pip install h2oai_client-1.4.2-py3-none-any.whl"
                }
            }
        }

        // Upload a dataset to DAI from a well-known localtion (for now in an S3)
        stage('upload-dataset-to-dai') {
            agent { label NODE_LABEL }
            steps {
                script {
                    echo "Loading dataset [${S3_DATA_SET_LOCATION}] to Driverless AI running at ${DAI_URL}."
                    NEW_DATASET = sh(script: "python upload_new_dataset.py ${DAI_URL} ${S3_DATA_SET_LOCATION}", returnStdout: true).trim()
                }
            }
        }
    }
}