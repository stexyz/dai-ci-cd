import h2oai_client
import sys
import requests
import math
from h2oai_client import Client, ModelParameters, InterpretParameters
address = sys.argv[1]

#TODO SP: add params to Jenkins too
username = 'h2oai'
password = 'h2oai'
h2oai = Client(address = address, username = username, password = password)
# make sure to use the same user name and password when signing in through the GUI

# load model summary and output the score
# TODO: possibly do checks here if we need to validate more
experiment_name = sys.argv[2]
model = h2oai.get_model_summary(experiment_name)
print(model.score)