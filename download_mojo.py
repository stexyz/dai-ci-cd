#TODO SP: check the imports, encapsulate the client generation into a separate file
import h2oai_client
import sys
import requests
import math
# TODO: remove model params and interpret params for client init
from h2oai_client import Client, ModelParameters, InterpretParameters

address = sys.argv[1]
username = sys.argv[2]
password = sys.argv[3]
h2oai = Client(address = address, username = username, password = password)

# load model summary and output the score
# TODO: possibly do checks here if we need to validate more
experiment_name = sys.argv[4]

mojo_scoring_pipeline = h2oai.build_mojo_pipeline_sync(experiment_name)
mojo_path = h2oai.download(mojo_scoring_pipeline.file_path, dest_dir=".")
print(mojo_path)