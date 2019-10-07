#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Jul 18 12:40:20 2019
This code does Spectral Partitioning for a given network
It takes two inputs, the network file in Bar-Gera format and the flows at UE (solved using any algorithm)

Used latest version of networkX
@author: Originally written by cnyahia. Revised by vpandey
"""
from networkx import *
import time
from networkx import *
from networkx import DiGraph
from networkx import linalg
from networkx import convert as convert
import copy as cp
import os
import numpy

'''
CustomNetwork inherits from DiGraph and adds no of zones as additional variable
'''
class customNetwork(DiGraph):
    def __init__(self, data=None, **attr):
        DiGraph.__init__(self, data, **attr)
        self.firstThroughNode = None

    def readBarGeraLinkInput(self, textfile):
        # This function populates the network based on the input text file if it's in bargera format
        try:
            with open(textfile) as bargera:  # use with to avoid having to close the file
                for k in range(2):
                    bargera.readline() #ignore first three lines
                third_line = bargera.readline()
                delimited = third_line.split()
                self.firstThroughNode = int(delimited[-1])
                
                #ignore the rest of metadata until ~ is found
                while True:
                    data = bargera.readline()
                    if data.find('~') == -1:
                        pass
                    else:
                        break
                #read until end of file is reached
                while True:
#                    data = bargera.readline()
#                    lod = data.split('\t')[1:11]
#                    self.add_edges_from([(int(lod[0]), int(lod[1]),
#                                  {'capacity': float(lod[2]), 'length': float(lod[3]), 
#                                   'FFT': float(lod[4]), 'b': float(lod[5]), 
#                                   'Power': float(lod[6]),
#                                'SpeedLimit': float(lod[7]), 'Toll': float(lod[8]), 
#                                'Type': float(lod[9])})])
#                    count= count+1
                    try:
                        data = bargera.readline()
                        lod = data.split('\t')[1:11]
                        #original self.add_edge had an issue with latest networkx 
                        self.add_edges_from([(int(lod[0]), int(lod[1]),
                                      {'capacity': float(lod[2]), 'length': float(lod[3]), 
                                       'FFT': float(lod[4]), 'b': float(lod[5]), 
                                       'Power': float(lod[6]),
                                    'SpeedLimit': float(lod[7]), 'Toll': float(lod[8]), 
                                    'Type': float(lod[9])})])
                    except:
                        break
            print("Network reading completed...")
            print("Number of links=", self.number_of_edges())
            print("Number of nodes=", self.number_of_nodes())
            print("first through node=", self.firstThroughNode)

        except IOError as ioerr:  # send an error message if file is not available
            print('File Error:' + str(ioerr))

    def readLinkVolumeAndCostsAtUE(self, textfile):
        """
        This function adds the attributes: cost of travel, volume
        Those attributes are obtained after running static traffic assignment
        They should be stored in a text file that has the following format
            First line: initialnode finalnode volume cost
            Second line: 1  2   300 0.5
            So on:
            The values on each line should be tab separated.
        """
        try:
            count=0
            with open(textfile) as results:
                results.readline() #ignore first line
                while True:
                    try:
                        data = results.readline()

                        newdata = data.rstrip() # remove \n from end and tidy up
                        lod = newdata.replace('\t',' ').split()  # If you don't specify \t automatically checks \t or ' '
                        for key, val in enumerate(lod):
                            lod[key] = val.rstrip()
                        self.add_edges_from([(int(lod[0]), int(lod[1]), 
                                      {'volume': float(lod[2]), 'cost': float(lod[3])})])
                        count = count+1
                        
                    except:
                        break
            print("Finished reading flow file...")
            print("Number of records=", count)
        except IOError as ioerr:  # send an error message if file is not available
            print('File Error:' + str(ioerr))
    
    #removes links that were in flow file but not in original network
    #if a link has both capacity and volume keys, it was in both file. Or else remove        
    def removeEdgesWithMissingData(self):
        allNodes = list(self.nodes)
        for i in allNodes:
            for j in allNodes[allNodes.index(i) + 1:]:
                if self.has_edge(i, j):
                    if 'capacity' not in self[i][j] or 'volume' not in self[i][j] == 0:
                        # some faulty links, not common to network and assignment file
                        print('Link removed from analysis',j,i)
                        self.remove_edge(i, j)
        
                if self.has_edge(j, i):
                    if 'capacity' not in self[j][i] or 'volume' not in self[j][i] == 0:
                        # more faulty links, those links are removed from the analysis
                        print('Link removed from analysis', j,i)
                        self.remove_edge(j, i)
    
    #spectral partitioning is based on undirected graphs
    #so we combine the flow and capacity of links between two nodes in both direction
    def combineWeightsOfLinksInBothDirection(self):
        allNodes = list(self.nodes)
        count=0
        for i in allNodes:
            count=count+1
            if count%100==0:
                print("---finished through node count "+str(count))
            for j in allNodes[allNodes.index(i) + 1:]:
                if self.has_edge(i, j):
                    if self.has_edge(j, i):
                        tempv1 = self[i][j]['volume']  # can use deepcopy instead
                        tempv2 = self[j][i]['volume']
                        tempc1 = self[i][j]['capacity']
                        tempc2 = self[j][i]['capacity']
                        newVolume = tempv1 + tempv2
                        newCapacity = tempc1 + tempc2
                        self[i][j]['volume'] = self[j][i]['volume'] = newVolume
                        self[i][j]['capacity'] = self[j][i]['capacity'] = newCapacity
                        
    
    
            
def runSpectralPartitioning(netName):
    #create the network
    network = customNetwork()
    network.readBarGeraLinkInput(os.getcwd()+"/Networks/"+netName+"/"+netName+"_net.txt")
    network.readLinkVolumeAndCostsAtUE(os.getcwd()+"/Networks/"+netName+"/"+netName+"_Flow.txt")
    
#    network.removeEdgesWithMissingData()
    print("Combining weights of links in both direction")
    network.combineWeightsOfLinksInBothDirection();
    
    #convert network to undirected graph
    print('\nConverting graph to an undirected graph combining link volumes and capacities')
    UndirectedGraph = network.to_undirected()
    copygraph = cp.deepcopy(UndirectedGraph)
    
    # Remove all the nodes that are centroids, then do partitioning, then add them
#    for node in list(copygraph.nodes()):
#        if node < network.firstThroughNode:
#            UndirectedGraph.remove_node(node)
#            print('Removing node ',node, ' temporarily before doing partition as it is a centroid')
    
    # If there are edges with zero flow, remove them as well so that you have connected components
    for edge in list(copygraph.edges()):
        if UndirectedGraph[edge[0]][edge[1]]['volume'] == 0:
            print('Removing edge' + str(edge[0]) + str(edge[1])+' as it has zero flow')
            UndirectedGraph.remove_edge(edge[0], edge[1])
    
    print('New number of nodes=',UndirectedGraph.number_of_nodes())
    print('New number of edges=',UndirectedGraph.number_of_edges())
    
#    if networkx.has_path(UndirectedGraph,32,36):
#        print("Yes, there is a path")
#    else:
#        print("32 and 36 are disconnected")
#        print(list(UndirectedGraph.edges()))

    #identify if the network is still connected after dropping edges. 
    #If so, only consider components where totalFlow>0
    #this helps get rid of edges at the end with zero flow
    graphs = list(connected_component_subgraphs(UndirectedGraph))
    keys = []
    for key, graph in enumerate(graphs):
        totalflow = 0  # If the total flow remains zero then it's one node
        for edge in graph.edges():
            totalflow = totalflow + UndirectedGraph[edge[0]][edge[1]]['volume']  # Single node components have no edges
        print(graph.number_of_nodes())
    
        print(graph.nodes())
        if totalflow == 0:
            keys.append(key)
    
    # Remove components that have zero flow
    for index in sorted(keys, reverse=True):
        del graphs[index]
    print('There are ' + str(len(graphs)) + ' connected component(s) with +ve flow')
    
    # Now do the partitioning for each connected component!
    countpartition = 0  # enumerate the subgraphs
    for NewUG in graphs:
        # Command for finding the eigenvector corresponding to the second smallest eignevalue
        secondEigenvector = linalg.fiedler_vector(NewUG, weight='volume', normalized='True')
        # Get the actual list of nodes in the connected component
        listofnodes = list(NewUG.nodes())
        # Sort the nodes based on the eigenvector order
        orderednodes = [x for _, x in sorted(zip(secondEigenvector, listofnodes))]
        # Check the number of elements in the eigenvector that are negative
        count = 0
        for elem in secondEigenvector:
            if elem < 0:
                count = count + 1
        # Depending on number of nodes with negative eigenvector value, partition based on the sort obtained earlier
        leftHalf = list(sorted(orderednodes[:count]))
        rightHalf = list(sorted(orderednodes[count:]))
        print('Number of nodes in one cluster=',len(leftHalf))
        print('Number of nodes in other cluster=', len(rightHalf))
        
        
        
        associatedClusterLabel={}
        for node in listofnodes:
            if node in leftHalf:
                associatedClusterLabel[node]=0
            else:
                associatedClusterLabel[node]=1
        printPartitionOutput(associatedClusterLabel, netName+'_SpectralPartition.txt');
    
    #print partition file

def printPartitionOutput(dictionaryOfLabels, fileName):
    out = open(fileName, "w")
    print("Node" + '\t' + "Subnet", file=out)
    for node in dictionaryOfLabels:
        print(str(node) + '\t' + str(dictionaryOfLabels[node]), file=out)
    out.close()
    

if __name__=='__main__':
#    netName = '6by6Grid'
#    netName = 'Texas'
    netName = 'Berlin-mpfc'
#    netName = 'Austin_sdb'
#    netName = 'SiouxFalls'
    runSpectralPartitioning(netName)