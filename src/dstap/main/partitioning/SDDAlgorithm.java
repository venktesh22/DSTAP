/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import dstap.links.Link;
import dstap.network.Network;
import dstap.nodes.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.jblas.DoubleMatrix;

/**
 * Code that generates network partitions using the SDDA algorithm
 * @author vpandey
 */
public class SDDAlgorithm extends PartitioningAlgo{
    private List<Node> sourceNodes;
    private Map<Node, Map<Node, Double>> nodeDistanceFrmSourceNodes;
    protected Map<Node, Set<Node>> clusterAssociation;
    
    private List<Integer> sourceNodeIds;
    private Map<Integer, Map<Integer, Integer>> nodeDistanceFrmSourceNodeIds;
    protected Map<Integer, Set<Integer>> clusterAssociationIds;
    
    private boolean addODPairLinks = false; //if true, it adds a link between every OD pair with demand

    public SDDAlgorithm(int noOfClusters, String netName) {
        super(noOfClusters,netName);
        sourceNodes = new ArrayList<>();
        nodeDistanceFrmSourceNodes = new HashMap<>();
        clusterAssociation = new HashMap<>();
        
        sourceNodeIds = new ArrayList<>();
        nodeDistanceFrmSourceNodeIds = new HashMap<>();
        clusterAssociationIds = new HashMap<>();
    }
    
    /**
     * This algorithm works with Integer IDs of nodes alone
     */
    public void runAlgorithm(){
        //Step-1 create an undirected graph
        UndirectedNetwork undirectedNet=  this.network.convertToUndirected();
        
        //Step-2 determine the rank of each node and find its first source node
        int firstSourceNode= undirectedNet.getLowestDegreeNodeId();
        sourceNodeIds.add(firstSourceNode);
        System.out.println("The first source node is: "+sourceNodeIds.get(0));
        
        //Determine remaining source nodes
        for(int clusterNo=0;clusterNo<noOfClusters;clusterNo++){
            int latestSourceNodeId = sourceNodeIds.get(clusterNo);
            updateDistanceFromSourceNodeId(undirectedNet, latestSourceNodeId);
//            printNodeDistance();
            
            int maxDistance=0;
            List<Integer> maxDistanceNodes= new ArrayList<>();
            for(Integer nId: nodeDistanceFrmSourceNodeIds.keySet()){
                if(!sourceNodeIds.contains(nId)){
                    int totalDist=0;
                    for(Integer sNodeIdForN: nodeDistanceFrmSourceNodeIds.get(nId).keySet()){
                        totalDist+= nodeDistanceFrmSourceNodeIds.get(nId).get(sNodeIdForN);
                    }
//                    System.out.println("Distance of node: "+n+"\t is: "+totalDist);
                    if(totalDist>maxDistance && totalDist<Integer.MAX_VALUE && totalDist>0){
                        maxDistance=totalDist;
//                        System.out.println("Highest distance node: "+nextSourceNode+"\t with dist: "+totalDist);
                    }
                }
            }
            for(Integer nId: nodeDistanceFrmSourceNodeIds.keySet()){
                int totalDist=0;
                for(Integer sNodeIdForN: nodeDistanceFrmSourceNodeIds.get(nId).keySet())
                    totalDist+= nodeDistanceFrmSourceNodeIds.get(nId).get(sNodeIdForN);
                if(totalDist==maxDistance)
                    maxDistanceNodes.add(nId);
            }
            
            int nextSourceNodeId= -1;
            
            //if more than one node which are at the same maximum distance, choose the one with lowest minimum range
            if(maxDistanceNodes.size()>1){
                double minRange=Double.MAX_VALUE;
                int nodeIdWithMinRange= -1;
                for(Integer nId: maxDistanceNodes){
//                    int n= network.getNodesByID().get(ids);
                    double range=0;
                    for(int i=0;i< sourceNodeIds.size();i++){
                        for(int j=i+1;j<sourceNodeIds.size();j++){
                            double diff= Math.abs( nodeDistanceFrmSourceNodeIds.get(nId).get(sourceNodeIds.get(i))- nodeDistanceFrmSourceNodeIds.get(nId).get(sourceNodeIds.get(j)));
                            range+= diff;
                        }
                    }
                    if(range<minRange){
                        minRange=range;
                        nodeIdWithMinRange= nId;
                    }
                }
                nextSourceNodeId= nodeIdWithMinRange;
            }
            else{
                nextSourceNodeId= maxDistanceNodes.get(0);
            }
            if(nextSourceNodeId==-1){
                System.out.println("Next Source node is null");
                System.exit(1);
            }
            
            //brute force method to ensure that last source node is not added
            if(clusterNo== noOfClusters-1){
                break;
            }
            sourceNodeIds.add(nextSourceNodeId);
            System.out.println("The next source node is: "+nextSourceNodeId);
        }
        
        //================================================//
        //==================Step 5 Partition==============//
        //================================================//
        
        System.out.println("Printing the shortest-hop distance from source nodes to all nodes");
        for(Integer i: nodeDistanceFrmSourceNodeIds.keySet()){
            System.out.println("Node "+i+" has distances as "+ nodeDistanceFrmSourceNodeIds.get(i));
        }
        
        ///5(A) create custom maps
        Map<Integer, Map<Integer, Integer>> nodeDistFromSNodesBySNodeIds = new HashMap<>();
        for(Integer sId: sourceNodeIds){
            Map<Integer, Integer> temp = new HashMap<>();
            for(Integer nodeId: nodeDistanceFrmSourceNodeIds.keySet()){
                temp.put(nodeId, nodeDistanceFrmSourceNodeIds.get(nodeId).get(sId));
            }
            nodeDistFromSNodesBySNodeIds.put(sId, temp);
        }
        
        Map<Integer, Integer> nodeRange = new HashMap<>();
        for(Integer nodeId: nodeDistanceFrmSourceNodeIds.keySet()){
            int range=0;
            for(int i=0;i< sourceNodeIds.size();i++){
                for(int j=i+1;j<sourceNodeIds.size();j++){
                    int diff= Math.abs( nodeDistanceFrmSourceNodeIds.get(nodeId).get(sourceNodeIds.get(i))- nodeDistanceFrmSourceNodeIds.get(nodeId).get(sourceNodeIds.get(j)));
                    range+= diff;
                }
            }
            nodeRange.put(nodeId, range);
        }
        //5(B) A while loop that runs until all nodes are assigned
        int clusterIndex=0;
        for(Integer sId: sourceNodeIds){
            associatedClusterLabel.put(network.getNodesByID().get(sId), clusterIndex++);
        }
        while(associatedClusterLabel.keySet().size()!= network.getNodes().size()){
            int sourceNodeClusterIndex=-1;
            System.out.println("associatedClusterLabel.keySet().size()="+associatedClusterLabel.keySet().size()+" and network.getNodes().size()="+network.getNodes().size());
            for(Integer sourceNodeId: sourceNodeIds){
                int nodeIdToBeAssignedToThisSNode = -1;
                sourceNodeClusterIndex++;
                
                //5(C) find node with minimum label
                int minLabel=10000;
                List<Integer> allMinLabelNodeIds = new ArrayList<>();
                for(Integer nodeId: nodeDistFromSNodesBySNodeIds.get(sourceNodeId).keySet()){
                    //check if this node is already labeled. If not, check its candidacy for minimum label node
                    if(!associatedClusterLabel.containsKey(network.getNodesByID().get(nodeId))){
                        if(nodeDistFromSNodesBySNodeIds.get(sourceNodeId).get(nodeId) < minLabel){
                            minLabel = nodeDistFromSNodesBySNodeIds.get(sourceNodeId).get(nodeId);
                        }
                    }
                    else{
                        continue;
                    }
                }
                for(Integer nId: nodeDistFromSNodesBySNodeIds.get(sourceNodeId).keySet()){
                    if(!associatedClusterLabel.containsKey(network.getNodesByID().get(nId))){
                        if(nodeDistFromSNodesBySNodeIds.get(sourceNodeId).get(nId) == minLabel){
                            allMinLabelNodeIds.add(nId);
                        }
                    }
                }
                
                if(allMinLabelNodeIds.size()>1){
                    
                    //5(D) Tie-braker 1: find the node with min rank
                    int minRank=100000;
                    List<Integer> allMinRankNodeIds = new ArrayList<>();
                    for(Integer nodeId: allMinLabelNodeIds){
                        if(!associatedClusterLabel.containsKey(network.getNodesByID().get(nodeId))){
                            if(undirectedNet.getNodesById().get(nodeId).getLinks().size()< minRank){
                                minRank = undirectedNet.getNodesById().get(nodeId).getLinks().size();
                            }
                        }
                    }
                    for(Integer nodeId: allMinLabelNodeIds){
                        if(!associatedClusterLabel.containsKey(network.getNodesByID().get(nodeId))){
                            if(undirectedNet.getNodesById().get(nodeId).getLinks().size()== minRank){
                                allMinRankNodeIds.add(nodeId);
                            }
                        }
                    }
                    if(allMinRankNodeIds.size()>1){
                        
                        //5(E) Tie breaker 2: find the node with highest range and if there are multiple of those, choose one randomly
                        int maxRange = -10000;
                        for(Integer nodeId: allMinRankNodeIds){
                            if(!associatedClusterLabel.containsKey(network.getNodesByID().get(nodeId))){
                                if(nodeRange.get(nodeId)> maxRange){
                                    nodeIdToBeAssignedToThisSNode = nodeId;
                                    maxRange= nodeRange.get(nodeId);
                                }
                            }
                        }
                    }
                    else if(allMinRankNodeIds.size()==1){
                        nodeIdToBeAssignedToThisSNode = allMinRankNodeIds.get(0);
                    }
                    else{
//                        System.out.println("No node found.");
//                        System.exit(1);
                          continue;
                    }
                }
                else if(allMinLabelNodeIds.size()==1){
                    nodeIdToBeAssignedToThisSNode = allMinLabelNodeIds.get(0);
                }
                else{
//                    System.out.println("No node found");
//                    System.exit(1);
                      continue;
                }
                
                //5(F) check if connecting this node will make a continuous graph
                //@to-do: maybe recheck with the original network based on the link directions to make this decision... or else we end up having unconnected directed graphs
                boolean thisNodeIsSafeToBeConnected = false;
                for(UndirectedNode n: undirectedNet.getNodesById().get(nodeIdToBeAssignedToThisSNode).getConnectedNodes()){
                    if(associatedClusterLabel.containsKey((network.getNodesByID().get(n.getId())))){
                        if(associatedClusterLabel.get(network.getNodesByID().get(n.getId())) == sourceNodeClusterIndex){
                            thisNodeIsSafeToBeConnected = true;
                            break;
                        }
                    }
                }
                if(!thisNodeIsSafeToBeConnected){
                    System.out.println("Node "+nodeIdToBeAssignedToThisSNode+" found not safe to be connected");
                    continue;
                }
                
                associatedClusterLabel.put(network.getNodesByID().get(nodeIdToBeAssignedToThisSNode), sourceNodeClusterIndex);
//                System.out.println("Adding node"+nodeIdToBeAssignedToThisSNode+" to cluster "+sourceNodeClusterIndex);
                
            }
            System.out.println("");
        }
        //--------------------Step-6 Identify boundary nodes--------------------
        findBoundaryNodes();
        
        //----------------------Step-7 Reordering nodes---------------------------
        
        addClusterLabelToCentroids();
        System.out.println("Ended");
    }
    
    private void updateDistanceFromSourceNodeId(UndirectedNetwork net, int sNodeId){
        for(UndirectedNode n: net.getNodes())
            n.label=Integer.MAX_VALUE;
        
        List<Integer> scanEligibleList = new ArrayList<>();
        Set<Integer> labeledNodes = new HashSet<>();
        
        scanEligibleList.add(sNodeId);
        labeledNodes.add(sNodeId);
        net.getNodesById().get(sNodeId).label=0;
        
        while(!scanEligibleList.isEmpty()){
            int currentNode= scanEligibleList.get(0);
            scanEligibleList.remove(0);
            
//            List<Link> allLinksToFromNode = new ArrayList(network.getNodesByID().get(currentNode).getOutgoing());
//            allLinksToFromNode.addAll(network.getNodesByID().get(currentNode).getIncoming());
            
            for(UndirectedLink l: net.getNodesById().get(currentNode).getLinks()){
//            for(Link l: nodesByID.get(currentNode).getOutgoing()){
                int downNode= -1;
                for(UndirectedNode n: l.allNodes){
                    if(currentNode!= n.getId())
                        downNode= n.getId();
                }
                if(!labeledNodes.contains(downNode)){
                    labeledNodes.add(downNode);
                    if(net.getNodesById().get(downNode).label > net.getNodesById().get(currentNode).label+1)
                        net.getNodesById().get(downNode).label = net.getNodesById().get(currentNode).label+1;
                    //not tracking the previous node
                    scanEligibleList.add(downNode);
                }
            }
        }
        
        //initialize the nodeDistanceFromSourceNodes
        for(UndirectedNode n: net.getNodes()){
            if(nodeDistanceFrmSourceNodeIds.containsKey(n.getId())){
                Map<Integer, Integer> temp = nodeDistanceFrmSourceNodeIds.get(n.getId());
                temp.put(sNodeId, n.label);
                nodeDistanceFrmSourceNodeIds.put(n.getId(), temp);
            }
            else{
                Map<Integer, Integer> temp = new HashMap<>();
                temp.put(sNodeId, n.label);
                nodeDistanceFrmSourceNodeIds.put(n.getId(), temp);
            }
        }
    }
    
    public void runAlgorithm_old(){
        //Step-1 Initialize
        
        //Step-2 Determine the rank of each node
        //Step-3 Determine the first source node
        int lowestRank=1000;
        Node firstSourceNode= null;
        for(Node n: network.getNodes()){
            int rank= n.getIncoming().size()+n.getOutgoing().size();
            network.nodeRank.put(n,rank );
            if(rank<lowestRank){// && n.getId()!=5){
                lowestRank=rank;
                firstSourceNode=n;
            }
        }
        sourceNodes.add(firstSourceNode);
        System.out.println("The first source node is: "+sourceNodes.get(0).getId());
        
        
        //--------------------Step-4 Determine the remaining source nodes---------------------------
        for(int clusterNo=0;clusterNo<noOfClusters;clusterNo++){
            Node latestSourceNode = sourceNodes.get(clusterNo);
            updateDistanceFromSourceNode(latestSourceNode);
//            printNodeDistance();
            
            int maxDistance=0;
            List<Integer> maxDistanceNodes= new ArrayList<>();
            for(Node n: nodeDistanceFrmSourceNodes.keySet()){
                if(!sourceNodes.contains(n)){
                    int totalDist=0;
                    for(Node sNodeForN: nodeDistanceFrmSourceNodes.get(n).keySet()){
                        totalDist+= nodeDistanceFrmSourceNodes.get(n).get(sNodeForN);
                    }
//                    System.out.println("Distance of node: "+n+"\t is: "+totalDist);
                    if(totalDist>maxDistance && totalDist<Integer.MAX_VALUE && totalDist>0){
                        maxDistance=totalDist;
//                        System.out.println("Highest distance node: "+nextSourceNode+"\t with dist: "+totalDist);
                    }
                }
            }
            for(Node n: nodeDistanceFrmSourceNodes.keySet()){
                int totalDist=0;
                for(Node sNodeForN: nodeDistanceFrmSourceNodes.get(n).keySet())
                    totalDist+= nodeDistanceFrmSourceNodes.get(n).get(sNodeForN);
                if(totalDist==maxDistance)
                    maxDistanceNodes.add(n.getId());
            }
            
            Node nextSourceNode= null;
            if(maxDistanceNodes.size()>1){
                double minRange=Double.MAX_VALUE;
                Node nodeWithMinRange= null;
                for(Integer ids: maxDistanceNodes){
                    Node n= network.getNodesByID().get(ids);
                    double range=0;
                    for(int i=0;i< sourceNodes.size();i++){
                        for(int j=i+1;j<sourceNodes.size();j++){
                            double diff= Math.abs( nodeDistanceFrmSourceNodes.get(n).get(sourceNodes.get(i))- nodeDistanceFrmSourceNodes.get(n).get(sourceNodes.get(j)));
                            range+= diff;
                        }
                    }
                    if(range<minRange){
                        minRange=range;
                        nodeWithMinRange= n;
                    }
                }
                nextSourceNode= nodeWithMinRange;
            }
            else{
                nextSourceNode= network.getNodesByID().get(maxDistanceNodes.get(0));
            }
            if(nextSourceNode==null){
                System.out.println("Next Source node is null");
                System.exit(1);
            }
            
            //brute force method to ensure that last source node is not added
            if(clusterNo== noOfClusters-1){
                continue;
            }
            sourceNodes.add(nextSourceNode);
            System.out.println("The next source node is: "+nextSourceNode.getId());
        }
        
        //----------------Step-5 Populate sub-domains------------------------
        for(int i=0;i<sourceNodes.size();i++){
            Node n= sourceNodes.get(i);
            Set<Node> temp = new HashSet<>();
            temp.add(n);
            clusterAssociation.put(n, temp);
            associatedClusterLabel.put(n, i);
        }
        
        for(Node n: nodeDistanceFrmSourceNodes.keySet()){
            if(!sourceNodes.contains(n)){
                double minDistance=Double.MAX_VALUE;
                Node nodeWithMinDist=null;
                for(Node sNode: nodeDistanceFrmSourceNodes.get(n).keySet()){
                    if(minDistance> nodeDistanceFrmSourceNodes.get(n).get(sNode)){
                        minDistance= (nodeDistanceFrmSourceNodes.get(n).get(sNode));
                        nodeWithMinDist= sNode;
                    }
                }
                if(nodeWithMinDist==null){
                    System.out.println("Node distance from source node is more than max distance. current node:"+n);
                    System.exit(1);
                }
                Set<Node> temp= clusterAssociation.get(nodeWithMinDist);
                temp.add(n);
                clusterAssociation.put(nodeWithMinDist, temp);
                associatedClusterLabel.put(n, associatedClusterLabel.get(nodeWithMinDist));
            }
        }
//        printNodeDistance();
        printClusterAssociation();
        
        //--------------------Step-6 Identify boundary nodes--------------------
        findBoundaryNodes();
        
        //----------------------Step-7 Reordering nodes---------------------------
        
        addClusterLabelToCentroids();
        
    }
    
    /**
     * Runs breadth-first search algorithm to determine the distance from the source node, 
     * which is defined as the number of links between source node and the current node
     * @param sourceNode 
     */
    private void updateDistanceFromSourceNode(Node sourceNode){
        for(Node n: network.getNodes())
            n.label=Integer.MAX_VALUE;
        
        List<Integer> scanEligibleList = new ArrayList<>();
        Set<Node> labeledNodes = new HashSet<>();
        
        scanEligibleList.add(sourceNode.getId());
        labeledNodes.add(sourceNode);
        sourceNode.label=0;
        
        while(!scanEligibleList.isEmpty()){
            int currentNode= scanEligibleList.get(0);
            scanEligibleList.remove(0);
            
            List<Link> allLinksToFromNode = new ArrayList(network.getNodesByID().get(currentNode).getOutgoing());
            allLinksToFromNode.addAll(network.getNodesByID().get(currentNode).getIncoming());
            
            for(Link l: allLinksToFromNode){
//            for(Link l: nodesByID.get(currentNode).getOutgoing()){
                Node downNode= null;
                if(l.getSource().getId()==currentNode)
                    downNode=l.getDest();
                else
                    downNode=l.getSource();
                if(!labeledNodes.contains(downNode)){
                    labeledNodes.add(downNode);
                    if(downNode.label > network.getNodesByID().get(currentNode).label+1)
                        downNode.label = network.getNodesByID().get(currentNode).label+1;
                    //not tracking the previous node
                    scanEligibleList.add(downNode.getId());
                }
            }
        }
        
        //initialize the nodeDistanceFromSourceNodes
        for(Node n: network.getNodes()){
//            if(n.label==Integer.MAX_VALUE){
//                System.out.println("Node "+n+ " is not connected to "+ sourceNode);
//                System.exit(1);
//            }
            if(nodeDistanceFrmSourceNodes.containsKey(n)){
                Map<Node, Double> temp = nodeDistanceFrmSourceNodes.get(n);
                temp.put(sourceNode, n.label);
                nodeDistanceFrmSourceNodes.put(n, temp);
            }
            else{
                Map<Node, Double> temp = new HashMap<>();
                temp.put(sourceNode, n.label);
                nodeDistanceFrmSourceNodes.put(n, temp);
            }
        }
    }
    
    private void printNodeDistance(){
        for(Node n: nodeDistanceFrmSourceNodes.keySet()){
            for(Node sNodeForN: nodeDistanceFrmSourceNodes.get(n).keySet()){
                System.out.println(n.getId()+"-->"+sNodeForN+"-->"+nodeDistanceFrmSourceNodes.get(n).get(sNodeForN));
            }
        }
    }
    
    private void printClusterAssociation(){
        for(Node n: clusterAssociation.keySet()){
            System.out.println(n+"-->"+clusterAssociation.get(n).size());
        }
    }

    public void writeBoundaryNodes(String netName, int noOfCluster) throws FileNotFoundException {
//        if(netName=="mitte_prenzlauerberg_friedrichshain_center" && noOfCluster==3)
//            associatedClusterLabel.put(nodesByID.get(13), 2); //forcing the node 13 to be a part of cluster 2
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File("Networks/"+netName +"/"+"NodeToClusterAssociation.txt"), false /* append = true */));
        fileOut.println("Node\tAssociated_boundary_node");
        for(Node n: network.getNodes()){
            fileOut.println(n.getId()+"\t"+associatedClusterLabel.get(n));
        }
        fileOut.close();
    }
    /**
     * 
     * @param fileName for the partition file
     * @return number of clusters
     */
    public int readExistingPartitionFile(String fileName) throws FileNotFoundException{
        System.out.println("Reading link flow file....");
        Scanner filein = new Scanner(new File(fileName));
        
        filein.nextLine(); //ignore the metadata information
        Set<Integer> uniqueClusterID = new HashSet<>();
        while (filein.hasNext()){
            int node = filein.nextInt();
            int clusterID = filein.nextInt();

            if(!associatedClusterLabel.containsKey(network.getNodesByID().get(node)))
                associatedClusterLabel.put(network.getNodesByID().get(node), clusterID);
            else{
                System.out.print("Same node two times...exiting");
                System.exit(1);
            }
            if(!uniqueClusterID.contains(clusterID))
                uniqueClusterID.add(clusterID);

        }
        return uniqueClusterID.size();
    }
    
    public void writeLinkCoordinateFile(String netName) throws FileNotFoundException{
        Map<Integer, List<Double>> nodeXYCoordinate = new HashMap<>();
        Scanner filein = new Scanner(new File("Networks/"+netName+"/"+netName+ "_node.txt"));
        filein.nextLine();
        int nodeID = -1;

        while (filein.hasNext())
        {
            nodeID = filein.nextInt();
            double xCoord = filein.nextDouble();
            double yCoord = filein.nextDouble();
            List<Double> tempList= new ArrayList<>();
            tempList.add(xCoord); tempList.add(yCoord);
            nodeXYCoordinate.put(nodeID, tempList);

            filein.nextLine();
        }
        filein.close();
        
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File("Networks/"+netName +"/DSTAPfiles/"+"linkCoordinates.txt"), false /* append = true */));
        fileOut.println("Init node\tTerm node \tCapacity\tfft\tCoef\tPower\tFlow\tFlowCost\tWKTCoordinates");
        for(Link l: network.getLinks()){
            int source= l.getSource().getId();
            int dest= l.getDest().getId();
            fileOut.println(source+"\t"+dest+"\t"+l.getCapacity()+"\t"+l.getFFTime()+"\t"+l.getCoef()+"\t"+l.getPower()+"\t"+l.getFlow()+"\t"+l.getTravelTime()+
                    "\t"+"LINESTRING("+nodeXYCoordinate.get(source).get(0)+" "+nodeXYCoordinate.get(source).get(1)+","+
                            nodeXYCoordinate.get(dest).get(0)+" "+nodeXYCoordinate.get(dest).get(1)+")");
        }
        fileOut.flush();
        fileOut.close();
    }
    
    @Override
    protected void setOutputFolderName(){
        String epoch= Integer.toString((int)(System.currentTimeMillis()/1000.0));
        epoch = "SDDA_"+epoch;
        File dir = new File("Networks/"+netName+"/Inputs/"+epoch);
    
        // attempt to create the directory here
        boolean successful = dir.mkdir();
        if (!successful){
            System.out.println("failed trying to create the directory");
        }
        this.partitionOutputFolderName= "Networks/"+netName+"/Inputs/"+epoch;
    }
}
