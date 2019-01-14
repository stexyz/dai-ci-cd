import h2oai_client
import sys
import requests
import math
from h2oai_client import Client, ModelParameters, InterpretParameters
address = sys.argv[1]

#TODO SP: add params to Jenkins too
username = 'h2oai'
password = 'i-0495a5469c1111c0a '
h2oai = Client(address = address, username = username, password = password)
# make sure to use the same user name and password when signing in through the GUI

# read dataset from s3
dataset = h2oai.create_dataset_from_s3(sys.argv[2])

# output the key of the dataset so that next build step in Jenkins can use it
print(dataset)