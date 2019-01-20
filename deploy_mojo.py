# Syntax:
# deploy_mojo.py ${MINIO_URL} ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY} ${MOJO_PATH}

# deploy mojo using these tutorials:
# https://sysadmins.co.za/using-minios-python-sdk-to-interact-with-a-minio-s3-bucket/
# https://docs.minio.io/docs/python-client-api-reference.html
# https://docs.minio.io/docs/minio-select-api-quickstart-guide.html

import boto3
import sys
import zipfile

MINIO_URL = sys.argv[1]
MINIO_ACCESS_KEY = sys.argv[2]
MINIO_SECRET_KEY = sys.argv[3]
MOJO_PATH = sys.argv[4]
MODEL_BUCKET = sys.argv[5]
MOJO_OBJECT_NAME = sys.argv[6]



r3 = boto3.resource('s3',
                  endpoint_url=MINIO_URL,
                  aws_access_key_id=MINIO_ACCESS_KEY,
                  aws_secret_access_key=MINIO_SECRET_KEY,
                  region_name='us-east-1')

# Unzip the mojo.zip 
zip_ref = zipfile.ZipFile('MOJO_PATH', 'r')
zip_ref.extractall('.')
zip_ref.close()

# Clearly for proper production use we would use some tagging to only keep adding new mojo files 
# and our production app would select the latest mojo file to run the predictions; 
# for sake of simplicity we compromise this transactional stability

# Putting data to S3 with boto3
# https://stackoverflow.com/questions/40336918/how-to-write-a-file-or-data-to-an-s3-object-using-boto3
# https://boto3.amazonaws.com/v1/documentation/api/latest/guide/migrations3.html

# Rewriting the old mojo for now
# https://stackoverflow.com/questions/23764683/are-writes-to-amazon-s3-atomic-all-or-nothing

# Only upload pipeline.mojo
MOJO_PIPELINE_LOCATION='./mojo-pipeline/pipeline.mojo'
r3.Object(MODEL_BUCKET, MOJO_OBJECT_NAME).put(Body=open(MOJO_PIPELINE_LOCATION, 'rb'))

print('success')