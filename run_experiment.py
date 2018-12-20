import h2oai_client
import sys
import requests
import math
from h2oai_client import Client, ModelParameters, InterpretParameters
address = sys.argv[1]

#TODO SP: add params to Jenkins too
target = 'DEFAULT_PAYMENT_NEXT_MONTH'
username = 'h2oai'
password = 'h2oai'
h2oai = Client(address = address, username = username, password = password)
# make sure to use the same user name and password when signing in through the GUI

# TODO solve better params passing so that we can get some logs here; now we only can output experiment name so that Jenkins can consume it
# print("About to start experiment with accuracy=[", sys.argv[2], "], time=[", sys.argv[3], "], interpretability=[",sys.argv[4],"].")
# start experiment synchronously
experiment = h2oai.start_experiment_sync(dataset_key = sys.argv[2],
                                         # testset_key = test.key,
                                         target_col = target,
                                         is_classification = True,
                                         accuracy = int(sys.argv[3]),
                                         time = int(sys.argv[4]),
                                         interpretability = int(sys.argv[5]),
                                         scorer = "AUC",
                                         seed = 1234)



# output the key of the experiment so that next build step in Jenkins can use it
print(experiment)