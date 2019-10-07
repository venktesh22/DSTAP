/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import dstap.links.Link;
import dstap.nodes.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jblas.DoubleMatrix;

/**
 * Has functions and general properties for any partitioning algorithm
 * @author vp6258
 */
public abstract class PartitioningAlgo {
    protected int noOfClusters;
    protected String netName;
    protected PartitioningNetwork network;
    
    protected Map<Node,Integer> associatedClusterLabel;
    protected Set<Node> boundaryNodes;
    protected String partitionOutputFolderName; //folder name inside INPUTS where partitions are created
    
    //partition statistics (computed at the end)
    private Map<Integer, Integer> noOfNodesInEachSubnet = new HashMap<>();
    private Map<Integer, Integer> noOfLinksInEachSubnet = new HashMap<>();
    private int noOfRegionalLinks;
    private double interflow; //will depend on flow values being read
    DoubleMatrix demandClusterToCluster; //matrix with i j element being demand from subnet i to subnet j...diagonal element is internal demand
    
    private List<Node> droppedNodes;
    private List<Link> droppedLinks; //these are nodes/links that are dropped while mapping a partition to DSTAP inputs. Reason is they are disconnected components
    
    //below mappoing are used to duplicate centroids which could get bisected
    Map<Integer, Integer> newNodeIDToOldNodeId = new HashMap<>();
    Map<Integer, Map<Integer, Integer>> oldNodeIdToClusterToNewNodeID = new HashMap<>();

    public PartitioningAlgo(int noOfClusters, String netName) {
        this.noOfClusters = noOfClusters;
        this.netName = netName;
        network = new PartitioningNetwork();
        
        associatedClusterLabel = new HashMap<>();
        boundaryNodes = new HashSet<>();
        partitionOutputFolderName = "_blank_"; //initialize with a blank name
        
        noOfNodesInEachSubnet = new HashMap<>();
        noOfLinksInEachSubnet = new HashMap<>();
        for(int i=0;i<noOfClusters;i++){
            noOfNodesInEachSubnet.put(i, 0);
            noOfLinksInEachSubnet.put(i, 0);
        }
        noOfRegionalLinks=0;
        interflow=0.0;
        demandClusterToCluster= DoubleMatrix.zeros(noOfClusters, noOfClusters);
        
        droppedNodes= new ArrayList<>();
        droppedLinks = new ArrayList<>();
    }
    
    public void readAndPrepareNetworkFiles() throws FileNotFoundException{
        network.readBarGeraLinkInput(netName);
        network.readBarGeraODFile(netName);
        network.readCompleteNetLinkFlowsAndCosts();
    }
    /**
     * Once partitioning is done, only the regular nodes are assigned a cluster label.
     * This function adds centroids to the same cluster based on the centroid connecter
     * If two or more centroid connectors, then the same node is split into multiple nodes and
     * is associated with both subnetworks.
     */
    protected void addClusterLabelToCentroids(){
        System.out.println("No of nodes which are assigned a cluster label (before assigning centroids)="+associatedClusterLabel.keySet().size());
        
        int newNodesId = network.firstThruNodeID+network.getNodes().size(); //we start with this value for all centroids split into multiple
        
        for(Integer nodeID: network.centroidsByID.keySet()){
            if(nodeID< network.firstThruNodeID){
//            if(!network.getNodesByID().containsKey(nodeID)){
                Node n = network.centroidsByID.get(nodeID);
                List<Node> connectedNodes = new ArrayList<>();
                n.getIncoming().forEach((l) -> {
                    if(!connectedNodes.contains(l.getSource()))
                        connectedNodes.add(l.getSource());
                });
                n.getOutgoing().forEach((l) -> {
                    if(!connectedNodes.contains(l.getDest()))
                        connectedNodes.add(l.getDest());
                });
//                System.out.println("Centroid "+n+" has these connected nodes "+connectedNodes);
//                if(connectedNodes.size()>2){
//                    System.out.println("Why is this centroid connected to more than two nodes?");
//                    System.exit(1);
//                }
                List<Integer> clusterLabels = new ArrayList<>();
                for(Node n2:connectedNodes){
                    Node n3 = network.getNodesByID().get(n2.getId()); //equivalent node in network
                    if(!associatedClusterLabel.containsKey(n3)){
//                        System.out.println("Node "+n2+" is ");
                        System.out.println("Node "+n2+" is not a centroid and does not connect with the rest of network through any other link.");
                        droppedNodes.add(n2);
                        continue;
//                        System.exit(1);
                    }
                    clusterLabels.add(associatedClusterLabel.get(n3));
                }
                
                if(clusterLabels.isEmpty()){
                    System.out.println("Weird, but centroid "+n+" is not connected to any node which was classified by the partitioning algorithm");
                    System.out.println(" This is possible when centroid and the node only connect to each other and not to the rest of the network");
                    System.out.println("Fix the issue of disconnected components in directed graph if possible. Exiting");
                    System.exit(1);
                }

                if(new HashSet<>(clusterLabels).size() <= 1){
                    //all cluster labels equal, so assign centroid to same node
                    Node newNode;
                    network.getNodesByID().put(n.getId(), newNode = new Node(n.getId(),"Partitioning"));
                    associatedClusterLabel.put(newNode, clusterLabels.get(0));
                    network.getNodes().add(newNode);
                }
                else{
                    Set<Integer> uniqueClusterLabels = new HashSet<>(clusterLabels);
                    System.out.println("\nCentroid "+n+" is connected to "+connectedNodes+" which are in this partitions="+clusterLabels);
                    
                    for(Integer clusterId: uniqueClusterLabels){
                        int newNodeId = newNodesId++;
                        Node newNode;
                        network.getNodesByID().put(newNodeId, newNode = new Node(newNodeId,"Partitioning"));
                        associatedClusterLabel.put(newNode, clusterId);
                        network.getNodes().add(newNode);
                        
                        
                        if(oldNodeIdToClusterToNewNodeID.containsKey(n.getId())){
                            Map<Integer, Integer> temp = oldNodeIdToClusterToNewNodeID.get(n.getId());
                            temp.put(clusterId, newNodeId);
                            oldNodeIdToClusterToNewNodeID.put(n.getId(), temp);
                        }
                        else{
                            Map<Integer, Integer> temp = new HashMap<>();
                            temp.put(clusterId, newNodeId);
                            oldNodeIdToClusterToNewNodeID.put(n.getId(), temp);
                        }
                        newNodeIDToOldNodeId.put(newNodeId, n.getId());
                    }
                    
                    //unequal cluster label. We will split centroid into two nodes
                    //for now forcing both labels to be the same as the first!!
//                    for(int i=1; i< clusterLabels.size(); i++){
//                        clusterLabels.set(i, clusterLabels.get(0));
//                    }
//                    int index=0;
//                    for(Node n2:connectedNodes){
//                        Node n3 = network.getNodesByID().get(n2.getId()); //equivalent node in network
//                        associatedClusterLabel.put(n3, clusterLabels.get(index));
//                        index++;
////                        if(!associatedClusterLabel.containsKey(n3)){
////                            System.out.println("Node "+n3+" is not a centroid, yet does not have an associated label");
////                        }
////                        clusterLabels.add(associatedClusterLabel.get(n3));
//                    }
//                    Node newNode;
//                    network.getNodesByID().put(n.getId(), newNode = new Node(n.getId(),"Partitioning"));
//                    associatedClusterLabel.put(newNode, clusterLabels.get(0));
//                    System.out.println("Complicated case! Cannot handle");
//                    System.exit(1);
                }
            }
        }
        
        for(Integer id: oldNodeIdToClusterToNewNodeID.keySet()){
            System.out.println("Centroid "+id+ " is now replaced with ");
            for(Integer cluster: oldNodeIdToClusterToNewNodeID.get(id).keySet()){
                System.out.println("++Cluster "+cluster+" with newNode ID="+oldNodeIdToClusterToNewNodeID.get(id).get(cluster));
            }
        }
        
        reassignCentroidConnectorsAsLinks();
        System.out.println("New no of nodes="+network.getNodesByID().keySet().size());
        System.out.println("No of nodes which are assigned a cluster label (after assigning centroids)="+associatedClusterLabel.keySet().size());
        
    }
    
    private void reassignCentroidConnectorsAsLinks(){
        //only called after 
        for(Integer sourceID: network.centroidConnectorsByID.keySet()){
            for(Integer destID: network.centroidConnectorsByID.get(sourceID).keySet()){
                               
                Node source=null, dest=null;
                Link l= network.centroidConnectorsByID.get(sourceID).get(destID);
                
                if(oldNodeIdToClusterToNewNodeID.containsKey(sourceID) && !oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    dest = network.getNodesByID().get(destID);
                    int clusterDest = associatedClusterLabel.get(dest);
                    source = network.getNodesByID().get(oldNodeIdToClusterToNewNodeID.get(sourceID).get(clusterDest));
                }
                else if(!oldNodeIdToClusterToNewNodeID.containsKey(sourceID) && oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    source = network.getNodesByID().get(sourceID);
                    int clusterSource = associatedClusterLabel.get(source);
                    dest = network.getNodesByID().get(oldNodeIdToClusterToNewNodeID.get(destID).get(clusterSource));
                }
                else if(!oldNodeIdToClusterToNewNodeID.containsKey(sourceID) && !oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    source = network.getNodesByID().get(sourceID);
                    dest= network.getNodesByID().get(destID);
                }
                else{
                    System.out.println("Both source and destination are split. This case is not designed to be handled well. Centroid Connector="+l);
                    System.exit(1);
                }
                if(source==(null) || dest==(null)){
//                    System.out.println("Centroid connector="+l);
//                    System.out.println("Source="+source+" and destination="+dest+". One of them is null so exiting!");
//                    System.out.println("associatedClusterLabel.containsKey(source)="+associatedClusterLabel.containsKey(source));
//                    System.out.println("associatedClusterLabel.containsKey(dest)="+associatedClusterLabel.containsKey(dest));
                    droppedLinks.add(l);
                    System.out.println("Dropping centroid connector "+l+" due to lack of connectivity");
                    continue;
//                    System.exit(1);
                }
                
                Link ll = new Link(source, dest, l.getFFTime(), l.getCoef(), l.getPower(), l.getCapacity(), "Partitioning");
                network.getLinks().add(ll);
                if(network.linksByID.containsKey(sourceID)){
                    Map<Integer,Link> temp= network.linksByID.get(sourceID);
                    temp.put(destID, ll);
                    network.linksByID.put(sourceID, temp);
                }
                else{
                    Map<Integer,Link> temp= new HashMap<>();
                    temp.put(destID, ll);
                    network.linksByID.put(sourceID, temp);
                }
            }
        }
        System.out.println("New number of links in the network="+network.getLinks().size());
    }
    
    public void generateNetworkFileOutput(String netName, int numCluster) throws FileNotFoundException{
//        PrintWriter fileOut1 = new PrintWriter(new FileOutputStream(new File("Networks/"+netName +"/DSTAPfiles/"+"regionalLinks.txt"), false /* append = true */));
        PrintWriter fileOut1 = new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/regionalLinks.txt"), false /* append = true */));
        
        List<PrintWriter> fileOut_list = new ArrayList<>();
        for(int i=0;i<numCluster;i++)
           fileOut_list.add(new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/subnet_"+i+"_net.txt"), false /* append = true */)));
        
        fileOut1.println("source\tdest\tfft\tcoef\tpower\tcapacity");
        for(PrintWriter p: fileOut_list)
            p.println("source\tdest\tfft\tcoef\tpower\tcapacity");
        
        for(Link l:network.getLinks()){
            if(associatedClusterLabel.get(l.getSource())==associatedClusterLabel.get(l.getDest())){
                int clusterLabel = associatedClusterLabel.get(l.getSource());
                fileOut_list.get(clusterLabel).println(l.getSource().getId()+"\t"+l.getDest().getId()+"\t"+l.getFFTime()+"\t"+l.getCoef()+"\t"+l.getPower()+"\t"+l.getCapacity());
                noOfLinksInEachSubnet.put(clusterLabel, noOfLinksInEachSubnet.get(clusterLabel)+1);
            }
            else{
                fileOut1.println(l.getSource().getId()+"\t"+l.getDest().getId()+"\t"+l.getFFTime()+"\t"+l.getCoef()+"\t"+l.getPower()+"\t"+l.getCapacity());
                noOfRegionalLinks++;
                this.interflow+= this.network.UELinkFlow.get(l);
            }
        }
        for(Node n: network.getNodes()){
            noOfNodesInEachSubnet.put(associatedClusterLabel.get(n), noOfNodesInEachSubnet.get(associatedClusterLabel.get(n))+1);
        }
                
        fileOut1.close();
        for(PrintWriter p: fileOut_list)
            p.close();
    }
    
    public void generateODTripsOutput(String netName, int numCluster) throws FileNotFoundException{
        List<PrintWriter> fileOut_InTrips_list = new ArrayList<>();
        List<PrintWriter> fileOut_OutTrips_list = new ArrayList<>();
        for(int i=0;i<numCluster;i++){
           fileOut_InTrips_list.add(new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/subnet_"+i+"In_trips.txt"), false /* append = true */)));
           fileOut_OutTrips_list.add(new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/subnet_"+i+"Out_trips.txt"), false /* append = true */)));
        }
        
        for(PrintWriter p: fileOut_InTrips_list)
            p.println("\n");
        for(PrintWriter p: fileOut_OutTrips_list)
            p.println("\n");
        
        //make a deep copy of networkOdDemand
        Map<Integer, Map<Integer, Double>> newOdDemand = new HashMap<>();
        
        //reassigning OD demand to new Nodes generated after splitting centroids
        for(Integer originID: network.odDemand.keySet()){
            int newOriginId = originID;
            for(Integer destID: network.odDemand.get(originID).keySet()){
                int newDestId = destID;
                if(oldNodeIdToClusterToNewNodeID.containsKey(originID) && !oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    Node dest= network.getNodesByID().get(destID);
                    int clusterDest = associatedClusterLabel.get(dest);
                    newOriginId = oldNodeIdToClusterToNewNodeID.get(originID).get(clusterDest);
                    
                }
                else if(!oldNodeIdToClusterToNewNodeID.containsKey(originID) && oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    Node source= network.getNodesByID().get(originID);
                    int clusterOrig = associatedClusterLabel.get(source);
                    newDestId = oldNodeIdToClusterToNewNodeID.get(destID).get(clusterOrig);
                }
                else if(oldNodeIdToClusterToNewNodeID.containsKey(originID) && oldNodeIdToClusterToNewNodeID.containsKey(destID)){
                    //when both origin and destination are split. Pick one cluster ID and assign total demand to that
                    int chosenClusterId = 0;
                    newOriginId = oldNodeIdToClusterToNewNodeID.get(originID).get(chosenClusterId);
                    newDestId = oldNodeIdToClusterToNewNodeID.get(destID).get(chosenClusterId);
//                    System.out.println("I do not know how to handle case when both origin and destinations are split. Think later");
//                    System.exit(1);
                }
                if(newOdDemand.containsKey(newOriginId)){
                    Map<Integer, Double> temp = newOdDemand.get(newOriginId);
                    temp.put(newDestId, network.odDemand.get(originID).get(destID));
                }
                else{
                    Map<Integer, Double> temp = new HashMap<>();
                    temp.put(newDestId, network.odDemand.get(originID).get(destID));
                    newOdDemand.put(newOriginId, temp);
                }
            }
        }
//        System.out.println("New OD demand structure is");
//        for(Integer orig: newOdDemand.keySet()){
//            System.out.println("Origin "+orig+" demand="+newOdDemand.get(orig));
//        }
        
        for(Integer origin: newOdDemand.keySet()){
//            if(!network.getNodesByID().containsKey(origin)){
//                continue; //this origin has been replaced by newNodeId so we do not want to work with it
//            }
            int originSubNetID = associatedClusterLabel.get(network.getNodesByID().get(origin));
            boolean flag_InTrips= true;
            boolean flag_OutTrips=true;
            
            for(Integer dest: newOdDemand.get(origin).keySet()){
//                if(!network.getNodesByID().containsKey(dest)){
//                    continue; //this origin has been replaced by newNodeId so we do not want to work with it
//                }
                int destSubNetID = associatedClusterLabel.get(network.getNodesByID().get(dest));
                if(originSubNetID==destSubNetID){
                    if(flag_InTrips){
                        fileOut_InTrips_list.get(originSubNetID).println("Origin\t"+origin);
                        flag_InTrips=false;
                    }
                    fileOut_InTrips_list.get(originSubNetID).print("\t"+dest+" :\t"+newOdDemand.get(origin).get(dest)+";");
                    
                }
                else{
                    if(flag_OutTrips){
                        fileOut_OutTrips_list.get(originSubNetID).println("Origin\t"+origin);
                        flag_OutTrips=false;
                    }
                    fileOut_OutTrips_list.get(originSubNetID).print("\t"+dest+" :\t"+newOdDemand.get(origin).get(dest)+";");
                }
                demandClusterToCluster.put(originSubNetID, destSubNetID, 
                            demandClusterToCluster.get(originSubNetID, destSubNetID)+newOdDemand.get(origin).get(dest));
            }
            fileOut_InTrips_list.get(originSubNetID).println("\n");
            fileOut_OutTrips_list.get(originSubNetID).println("\n");
        }
        
        for(PrintWriter p: fileOut_InTrips_list)
            p.close();
        for(PrintWriter p: fileOut_OutTrips_list)
            p.close();
    }
    
    protected abstract void setOutputFolderName();
    
    public void generateSubNetNames(String netName, int numCluster) throws FileNotFoundException{
        this.setOutputFolderName();
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/subnetNames.txt"), false /* append = true */));
        for(int i=0;i<numCluster;i++){
           fileOut.println("subnet_"+i);
        }
        fileOut.close();
    }
    
    public void generatePartitionOutput() throws FileNotFoundException{
//        this.setOutputFolderName();
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/nodeToClusterAssociation.txt"), false /* append = true */));
        fileOut.println("NodeID\tClusterNumber"); 
        for(Node n: associatedClusterLabel.keySet())
            fileOut.println(n.getId()+"\t"+associatedClusterLabel.get(n));
        fileOut.close();
    }
    
    public void printPartitionStats() throws FileNotFoundException{
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/partitionStats.txt"), false /* append = true */));
        fileOut.println("Cluster\tNo of nodes\tNo of Links");
        for(Integer clusterID: noOfNodesInEachSubnet.keySet())
            fileOut.println(clusterID+"\t"+noOfNodesInEachSubnet.get(clusterID)+"\t"+noOfLinksInEachSubnet.get(clusterID));
        
        fileOut.println("\nNo of regional links="+noOfRegionalLinks);
        fileOut.println("No of boundary nodes="+boundaryNodes.size());
        fileOut.println("Interflow="+interflow);
        fileOut.println("\n\nDemand Matrix from one subnet to other(including internal demand):\n"+demandClusterToCluster);
        fileOut.println("Number of dropped nodes="+droppedNodes.size());
        fileOut.println("Number of dropped links="+droppedLinks.size());
        
        System.out.println("Cluster\tNo of nodes\tNo of Links");
        for(Integer clusterID: noOfNodesInEachSubnet.keySet())
            System.out.println(clusterID+"\t"+noOfNodesInEachSubnet.get(clusterID)+"\t"+noOfLinksInEachSubnet.get(clusterID));
        
        System.out.println("\nNo of regional links="+noOfRegionalLinks);
        System.out.println("No of boundary nodes="+boundaryNodes.size());
        System.out.println("Interflow="+interflow);
        System.out.println("\n\nDemand Matrix:\n"+demandClusterToCluster); 
        System.out.println("Number of dropped nodes="+droppedNodes.size());
        System.out.println("Number of dropped links="+droppedLinks.size());
//        for(Node n: associatedClusterLabel.keySet())
//            fileOut.println(n.getId()+"\t"+associatedClusterLabel.get(n));
        fileOut.close();
    }
    
    public abstract void runAlgorithm();
    
    protected void findBoundaryNodes(){
        for(Link l: network.getLinks()){
            if(associatedClusterLabel.get(l.getDest())!=associatedClusterLabel.get(l.getSource())){
                boundaryNodes.add(l.getDest());
                boundaryNodes.add(l.getSource());
            }
        }
//        System.out.println(boundaryNodes);
        System.out.println("No. of boundary nodes after the partition: "+boundaryNodes.size());
    }
}
