#TODO SP: check the imports, encapsulate the client generation into a separate file
import h2oai_client
import sys
import requests
import math
from h2oai_client import Client, ModelParameters, InterpretParameters

address = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
h2oai = Client(address = address, username = username, password = password)

# read dataset from s3
dataset = h2oai.create_dataset_from_s3(sys.argv[4])

# output the key of the dataset so that next build step in Jenkins can use it
print(dataset)