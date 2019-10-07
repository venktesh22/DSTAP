'''
This code inherits a directed graph from the networkx module, and adds certain features!
@author: cny

Objectives:
1) Verify what is happening exactly when you change from directed to undirected & have links in both directions!
    Adding everything up to have sum of capacity and sum of flow in both directions when have two directions.
2) Implement CTM!

'''
from __future__ import division
from __future__ import print_function
#import networkx as nx
import time
from networkx import *
from networkx import DiGraph
from networkx import linalg
#from networkx import convert as convert
import networkx.convert as convert
import numpy

#start = time.time()

#######################################################################
##################### General functions and methods ###################
#######################################################################

# Define the class and methods for generating the networks
class networkcny(DiGraph):
    def __init__(self, data=None, **attr):
        DiGraph.__init__(self, data, **attr)

    def populate_from_bargera(self, textfile):
        # This function populates the network based on the input text file if it's in bargera format
        try:
            with open(textfile) as bargera:  # use with to avoid having to close the file
                while True:
                    data = bargera.readline()
                    if data.find('~') == -1:
                        pass
                    else:
                        break

                while True:
                    try:
                        data = bargera.readline()
                        lod = data.split('\t')[1:11]
                        self.add_edge(int(lod[0]), int(lod[1]),
                                   {'capacity': float(lod[2]), 'length': float(lod[3]), 'FFT': float(lod[4]), 'b': float(lod[5]), 'Power': float(lod[6]),
                                    'SpeedLimit': float(lod[7]), 'Toll': float(lod[8]), 'Type': float(lod[9])})
                    except:
                        break


        except IOError as ioerr:  # send an error message if file is not available
            print('File Error:' + str(ioerr))

    def populate_volumes_costs(self,textfile):
        '''
        This function adds the attributes: cost of travel, volume
        Those attributes are obtained after running static traffic assignment
        They should be stored in a text file that has the following format
            First line: initialnode finalnode volume cost
            Second line: 1  2   300 0.5
            So on:
            The values on each line should be tab separated.
        '''
        try:
            with open(textfile) as results:
                results.readline()
                while True:
                    try:
                        data = results.readline()

                        ###################################################################
                        ################# CHECK HOW DATA IS DELIMITED! ####################
                        ###################################################################

                        #If you have Austin network or space separated flow file:
                        #newdata=data.rstrip() #remove \n from end and tidy up
                        #lod=newdata.split(' ')  #list of data, zero is first node, 1 is second node, 2 is volume, 3 is cost
                        #self.add_edge(int(lod[0]), int(lod[1]), {'volume': float(lod[3]), 'cost': float(lod[4])})

                        # If you have Chicago Sketch or tab separated flow file:
                        newdata=data.rstrip()
                        lod=newdata.split('\t')
                        for key,val in enumerate(lod):
                            lod[key]=val.rstrip()
                        self.add_edge(int(lod[0]), int(lod[1]), {'volume': float(lod[2]), 'cost': float(lod[3])})


                        # For the weird structure of the Anaheim network!
                        #newdata = data.rstrip()
                        #lod = newdata.split('\t')
                        #for key, val in enumerate(lod):
                        #    lod[key] = val.rstrip()
                        #self.add_edge(int(lod[0]), int(lod[1]), {'volume': float(lod[3]), 'cost': float(lod[4])})


                    except:
                        break

        except IOError as ioerr:  # send an error message if file is not available
            print('File Error:' + str(ioerr))


# define a function that writes to a text file the partitioned network
def writetofile(lists, counter, name):
    out=open(name + '.txt',"w")
    print("Node" + '\t' + "Subnet", file=out)
    for key, list in enumerate(lists):
        for elem in list:
            print(str(elem) + '\t' + str(counter+key), file=out)
    out.close()


# Generate your base graph
MyGraph=networkcny()
MyGraph.populate_from_bargera('SiouxFalls_net.txt')
MyGraph.populate_volumes_costs('SiouxFalls_flow.txt')


# Generate an undirected graph, add the volumes and capacities on links so that you have an aggregate link
# corresponding to addition of capacities and volumes in the undirected graph
mylist=MyGraph.nodes()
for i in mylist:
    for j in mylist[mylist.index(i)+1:]:
        if MyGraph.has_edge(i,j):
            #try:
            #    MyGraph[i][j]['volume']
            #except:
            #    MyGraph.remove_edge(i,j)
            #    continue
            if MyGraph.has_edge(j,i):
                try:
                    tempv1 = MyGraph[i][j]['volume']
                    tempv2 = MyGraph[j][i]['volume']
                    tempc1 = MyGraph[i][j]['capacity']
                    tempc2 = MyGraph[j][i]['capacity']
                    volume = tempv1 + tempv2
                    capacity = tempc1 + tempc2
                    MyGraph[i][j]['volume'] = MyGraph[j][i]['volume'] = volume
                    MyGraph[i][j]['capacity'] = MyGraph[j][i]['capacity'] = capacity
                 #   MyGraph[j][i]['volume']
                ## Note that the exceptions below handle cases were there are links in the assignment file
                # that are not present in the network file, and vice verse
                # I have no idea why this is the case, files from Ehsan for Austin have this issue
                except:
                    if MyGraph[i][j].has_key('capacity')==0 or MyGraph[i][j].has_key('volume')==0:
                        #some faulty links, not common to network and assignment file
                        print('Link removed from analysis')
                        print(i,j)
                        MyGraph.remove_edge(i, j)

                    if MyGraph[j][i].has_key('capacity')==0 or MyGraph[j][i].has_key('volume')==0:
                        #more faulty links, those links are removed from the analysis
                        print('Link removed from analysis')
                        print(j,i)
                        MyGraph.remove_edge(j, i)


#convert graph to undirected
#UndirectedGraph=convert.convert_to_undirected(MyGraph)
UndirectedGraph=MyGraph.to_undirected()

#get number of nodes for undirected graph
#n = UndirectedGraph.number_of_nodes()




#################################################################################
################## Pre-process to remove links with zero flow ###################
#################################################################################
#Instead of removing , create a copy, do the partitioning, also have to make sure the cut
# doesn't cut out centroids! To do this start doing the partitioning after the centroids
# and then just connect the centroids. How to process the partition after?

for edge in UndirectedGraph.edges():
    if UndirectedGraph[edge[0]][edge[1]]['volume']==0:
        UndirectedGraph.remove_edge(edge[0],edge[1])

graphs=list(connected_component_subgraphs(UndirectedGraph))
keys=[]

# Have to check what's going on here, you are getting the connected component, but it has only one node?
# Check if there are components that have zero flow, periphery of network
for key, graph in enumerate(graphs):
    totalflow=0
    for edge in graph.edges():
        totalflow=totalflow+UndirectedGraph[edge[0]][edge[1]]['volume']
    print(graph.number_of_nodes())
    print(graph.nodes())
    if totalflow==0:
        keys.append(key)

# Remove components that have zero flow
for index in sorted(keys, reverse=True):
    del graphs[index]


print('There are ' + str(len(graphs)) + ' connected component(s) with +ve flow')

# List out the connected components if there are more than one connected components
# Those are give high indexes, 10001, 10002, 1003, to indicate they are separated
# because they belong to different components
################################################################################
################## NEED TO FIX THIS ############################################
################################################################################
countcomponent=1000
nodesforgraphs=[]
if len(graphs)>1:
    for graph in graphs:
        nodesforgraphs.append(graph.nodes())
    namecomp='componentsplit' #gives you where each component goes.
    # The next loop gives you how those components are partitioned
    # Starting at 01 partition for component 1000, 23 for component 1001, etc.
    writetofile(nodesforgraphs, countcomponent, namecomp)



#Now do the partitioning for each connected component!
countpartition=0  # enumerate the subgraphs
for NewUG in graphs:
    # Command for finding the eigenvector corresponding to the second smallest eignevalue
    secondEigenvector = linalg.fiedler_vector(NewUG, weight='volume', normalized='True')
    # Get the actual list of nodes in the connected component
    listofnodes=NewUG.nodes()
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
    # Define the partition
    partitionoriginal=leftHalf, rightHalf
    # Write the partition to file
    name='partition'+str(countpartition)+str(countpartition+1)
    # Create text file
    writetofile(partitionoriginal, countpartition, name)
    # update the partition iterator
    countpartition+=2 # You know that you will partition each component into two subcomponents!




'''
# Need to Automate this part as well!
#############################################################################################
#################### Repeat for subgraph ####################################################
#############################################################################################

sub=NewUG.subgraph(rightHalf)
secondEigenvectorsub = linalg.fiedler_vector(sub, weight='volume', normalized='True')
listofnodessub=sub.nodes()
orderednodessub=[x for _,x in sorted(zip(secondEigenvectorsub,listofnodessub))]
countsub=0
for elem in secondEigenvectorsub:
    if elem<0:
        countsub=countsub+1

leftHalfsub = list(sorted(orderednodessub[:countsub]))
rightHalfsub = list(sorted(orderednodessub[countsub:]))

partitionsub=leftHalfsub, rightHalfsub
writetofile(partitionsub, 'AustinFlowWeightedFourRightHalf')

################################################################################################
################################################################################################
'''

##################################### Print computation time if needed ##########################
#end = time.time()
#print(end - start)







################################################################################################
################################# Extra code ###################################################
################################################################################################

'''
################### WARNING
###### THIS IS ASSUMING THAT THERE IS ONLY ONE NON-ZERO FLOW COMPONENET
NewUG=graphs[0] #for the case of Austin there are only one graph that has non-zero components, have to fix this to make it more general

########################################################################################
#################### The new connected component graph is NewUG ########################
########################################################################################




########################################################################################
################### Find the second smallest Eigenvector and sort nodes ################
########################################################################################

#Command for finding the eigenvector corresponding to the second smallest eignevalue
secondEigenvector = linalg.fiedler_vector(NewUG, weight='volume', normalized='True')

#Get the actual list of nodes of the graph
listofnodes=NewUG.nodes()

#Sorted nodes based on the eigenvector
orderednodes=[x for _,x in sorted(zip(secondEigenvector,listofnodes))]

#########################################################################################
######################## Print out values of partition ##################################
#########################################################################################


#check the number of elements in the eigenvector that are negative
count=0
for elem in secondEigenvector:
    if elem<0:
        count=count+1

#Depending on number of nodes with negative eigenvector value, partition based on the sort obtained earlier.
leftHalf = list(sorted(orderednodes[:count]))
rightHalf= list(sorted(orderednodes[count:]))


partitionoriginal=leftHalf, rightHalf
#print(partitionoriginal)
#writetofile(partitionoriginal, 'AustinFlowWeightedTwoParts')







####################################################################################################





#get the adjacency matrix
graph=linalg.adjacency_matrix(UndirectedGraph)

def cutQuality(j):
    firstHalf, secondHalf = sortedVertexIndices[range(j + 1)], sortedVertexIndices[range(j + 1, n)]
    firstTotal, secondTotal, crossTotal = 0, 0, 0

    for u in range(n):
        for v in range(n):
            if graph[u, v] > 0:
                if u in firstHalf and v in firstHalf:
                    firstTotal += graph[u, v]
                elif u in secondHalf and v in secondHalf:
                    secondTotal += graph[u, v]
                else:
                    crossTotal += graph[u, v]

    if 0 == min(firstTotal, secondTotal):
        return numpy.inf

    return crossTotal / min(firstTotal, secondTotal)


bestCutIndex = min(range(n), key=cutQuality)
leftHalf, rightHalf = sortedVertexIndices[:bestCutIndex], sortedVertexIndices[bestCutIndex:]

#partition=list(sorted(leftHalf)), list(sorted(rightHalf))
subleft=UndirectedGraph.subgraph(leftHalf)
nleft=subleft.number_of_nodes()
secondEigenvectorleft = linalg.fiedler_vector(subleft, weight='volume', normalized='True')
sortedVertexIndicesleft = secondEigenvectorleft.argsort()
countleft=0
for elem in secondEigenvectorleft:
    if elem<0:
        countleft=countleft+1

leftHalfleft = list(sorted(sortedVertexIndicesleft[:count]))
rightHalfleft= list(sorted(sortedVertexIndicesleft[count:]))

partition=leftHalfleft, rightHalfleft
print(partition)

#repeat for subgraph
nr=sub.number_of_nodes()
secondEigenvectorr = linalg.fiedler_vector(sub, weight='volume', normalized='True')
sortedVertexIndicesr = secondEigenvectorr.argsort()

listofnodesr=sub.nodes()
orderednodesr=[0]*nr
for key, sortedindex in enumerate(sortedVertexIndicesr):
    orderednodesr[key]=listofnodesr[sortedindex]


countr=0
for elem in secondEigenvectorr:
    if elem<0:
        countr=countr+1

leftHalfr = list(sorted(orderednodesr[:countr]))
rightHalfr= list(sorted(orderednodesr[countr:]))

partitionr=leftHalfr, rightHalfr
writetofile(partitionr, 'tst1')
#print(leftHalfr)

subr=UndirectedGraph.subgraph(leftHalf)

#repeat for subgraph
nr=subr.number_of_nodes()
secondEigenvectorr = linalg.fiedler_vector(subr, weight='volume', normalized='True')
sortedVertexIndicesr = secondEigenvectorr.argsort()

listofnodesr=subr.nodes()
orderednodesr=[0]*nr
for key, sortedindex in enumerate(sortedVertexIndicesr):
    orderednodesr[key]=listofnodesr[sortedindex]


countr=0
for elem in secondEigenvectorr:
    if elem<0:
        countr=countr+1

leftHalfr = list(sorted(orderednodesr[:countr]))
rightHalfr= list(sorted(orderednodesr[countr:]))

partitionr=leftHalfr, rightHalfr
#print(partitionr)

TotalNetVolume=0
mylist=UndirectedGraph.nodes()
totalvolume=0
totalcapacity=0
for i in mylist:
    for j in mylist[mylist.index(i)+1:]:
        if UndirectedGraph.has_edge(i,j):
            #if s2.has_edge(j,i):
            totalvolume=totalvolume+UndirectedGraph[i][j]['volume']
            totalcapacity=totalcapacity+UndirectedGraph[i][j]['capacity']

#s1=UndirectedGraph.subgraph(leftHalfr)
s2=UndirectedGraph.subgraph(rightHalfr)

mylistsmall=s2.nodes()
volume=0
capacity=0
for i in mylistsmall:
    for j in mylistsmall[mylistsmall.index(i)+1:]:
        if s2.has_edge(i,j):
            #if s2.has_edge(j,i):
            volume=volume+s2[i][j]['volume']
            capacity=capacity+s2[i][j]['capacity']

v2c=volume/capacity
portion = volume/totalvolume
print(v2c)
print(portion)
print(rightHalfr)
'''