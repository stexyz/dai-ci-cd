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

target = 'DEFAULT_PAYMENT_NEXT_MONTH'

# TODO solve better params passing so that we can get some logs here; now we only can output experiment name so that Jenkins can consume it
# print("About to start experiment with accuracy=[", sys.argv[2], "], time=[", sys.argv[3], "], interpretability=[",sys.argv[4],"].")
# start experiment synchronously
experiment = h2oai.start_experiment_sync(dataset_key = sys.argv[4],
                                         target_col = "Weekly_Sales",
                                         orig_time_col = 'Date',
                                         time_col = 'Date',
                                         is_classification = False,
                                         enable_gpus = True,
                                         accuracy = int(sys.argv[5]),
                                         time = int(sys.argv[6]),
                                         interpretability = int(sys.argv[7]),
                                         scorer = 'RMSE',
                                         time_groups_columns = ['Date', 'Dept', 'Store'],
                                         time_period_in_seconds = 604800,
                                         num_prediction_periods = 1,
                                         num_gap_periods = 0,
                                         is_timeseries = True,
                                         seed = 1234)

# output the key of the experiment so that next build step in Jenkins can use it
print(experiment.key)