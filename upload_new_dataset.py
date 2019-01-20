#TODO SP: check the imports, encapsulate the client generation into a separate file
import h2oai_client
import sys
import requests
import math
import boto3
from h2oai_client import Client, ModelParameters, InterpretParameters

address = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
bucket = sys.argv[4]
object_name = sys.argv[5]
MINIO_URL = sys.argv[6]
MINIO_ACCESS_KEY = sys.argv[7]
MINIO_SECRET_KEY = sys.argv[8]

r3 = boto3.resource('s3',
                  endpoint_url=MINIO_URL,
                  aws_access_key_id=MINIO_ACCESS_KEY,
                  aws_secret_access_key=MINIO_SECRET_KEY,
                  region_name='us-east-1')

# to utilise the local s3 (minio) we need to download the file locally (easier than set up bucket policy and generate unexpirable url)
r3.Bucket(bucket).download_file(object_name, object_name)

h2oai = Client(address = address, username = username, password = password)

# read dataset from s3
dataset = h2oai.upload_dataset(object_name)

# output the key of the dataset so that next build step in Jenkins can use it
print(dataset)