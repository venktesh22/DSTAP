#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jul 25 08:00:37 2019
File to visualize partition

DOESNOT WORK YET....

@author: vpandey
"""

import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.basemap import Basemap

import pandas as pd

def plotCluster(netName):
    import os
    cwd = os.getcwd()
    
    dataDir = cwd+'/Networks/'+netName+'/'+netName+"_node.txt"
    InputData = pd.read_csv(dataDir, sep='\t', index_col=None)
    
#    plt.scatter(InputData['X'],InputData['Y'])
    
#    print(InputData.)

    plt.figure(figsize=(8, 8))
    m = Basemap(projection='ortho', resolution=None, lat_0=50, lon_0=-100)
    m.bluemarble(scale=0.5)
    
    plt.plot(InputData["X"], InputData["Y"], 'ok', markersize=5)
    plt.show()
    return InputData

if __name__=='__main__':
    data= plotCluster('Austin_sdb')
#    import argparse
#    parser = argparse.ArgumentParser()
#    parser.add_argument('--net_name', type=str, default='vpg') #I assume this is the name of the folder with inputs
#    parser.add_argument('--subfolder_name','-sfn', type=str)
#    args = parser.parse_args()
#    customPlotFunc(args.net_name,args.subfolder_name)
