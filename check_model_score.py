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

# load model summary and output the score
# TODO: possibly do checks here if we need to validate more
experiment_name = sys.argv[2]
model = h2oai.get_model_summary(experiment_name)
print(model.score)