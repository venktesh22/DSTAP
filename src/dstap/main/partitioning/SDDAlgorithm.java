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

/**
 * Code that generates network partitions using the SDDA algorithm
 * @author vpandey
 */
public class SDDAlgorithm {
    private int noOfClusters;
    private String netName;
    private SDDANetwork network;
    
    private List<Node> sourceNodes;
    private Map<Node, Map<Node, Double>> nodeDistanceFrmSourceNodes;
    private Map<Node, Set<Node>> clusterAssociation;
    private Map<Node,Integer> associatedClusterLabel;
    private Set<Node> boundaryNodes;
    private boolean addODPairLinks = false; //if true, it adds a link between every OD pair with demand
    
    private String partitionOutputFolderName; //folder name inside INPUTS where partitions are created

    public SDDAlgorithm(int noOfClusters, String netName) {
        this.noOfClusters = noOfClusters;
        this.netName = netName;
        network = new SDDANetwork();
        
        sourceNodes = new ArrayList<>();
        nodeDistanceFrmSourceNodes = new HashMap<>();
        clusterAssociation = new HashMap<>();
        associatedClusterLabel = new HashMap<>();
        boundaryNodes = new HashSet<>();
    }
    
    public void readAndPrepareNetworkFiles() throws FileNotFoundException{
        network.readBarGeraLinkInput(netName);
        network.readBarGeraODFile(netName);
    }
    
    public void runSDDAalgorithm(){
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
        for(Link l: network.getLinks()){
            if(associatedClusterLabel.get(l.getDest())!=associatedClusterLabel.get(l.getSource())){
                boundaryNodes.add(l.getDest());
                boundaryNodes.add(l.getSource());
            }
        }
        System.out.println(boundaryNodes);
        System.out.println("No. of boundary nodes after the partition: "+boundaryNodes.size());
        
        //----------------------Step-7 Reordering nodes---------------------------
        
        
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
                fileOut_list.get(associatedClusterLabel.get(l.getSource())).println(l.getSource().getId()+"\t"+l.getDest().getId()+"\t"+l.getFFTime()+"\t"+l.getCoef()+"\t"+l.getPower()+"\t"+l.getCapacity());
            }
            else{
                fileOut1.println(l.getSource().getId()+"\t"+l.getDest().getId()+"\t"+l.getFFTime()+"\t"+l.getCoef()+"\t"+l.getPower()+"\t"+l.getCapacity());
            }
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
        
        for(Integer origin: network.odDemand.keySet()){
            int originSubNetID = associatedClusterLabel.get(network.getNodesByID().get(origin));
            boolean flag_InTrips= true;
            boolean flag_OutTrips=true;
            
            for(Integer dest: network.odDemand.get(origin).keySet()){
                int destSubNetID = associatedClusterLabel.get(network.getNodesByID().get(dest));
                if(originSubNetID==destSubNetID){
                    if(flag_InTrips){
                        fileOut_InTrips_list.get(originSubNetID).println("Origin\t"+origin);
                        flag_InTrips=false;
                    }
                    fileOut_InTrips_list.get(originSubNetID).print("\t"+dest+" :\t"+network.odDemand.get(origin).get(dest)+";");
                }
                else{
                    if(flag_OutTrips){
                        fileOut_OutTrips_list.get(originSubNetID).println("Origin\t"+origin);
                        flag_OutTrips=false;
                    }
                    fileOut_OutTrips_list.get(originSubNetID).print("\t"+dest+" :\t"+network.odDemand.get(origin).get(dest)+";");
                }
            }
            fileOut_InTrips_list.get(originSubNetID).println("\n");
            fileOut_OutTrips_list.get(originSubNetID).println("\n");
        }
        
        for(PrintWriter p: fileOut_InTrips_list)
            p.close();
        for(PrintWriter p: fileOut_OutTrips_list)
            p.close();
    }
    
    public String generateSubNetNames(String netName, int numCluster) throws FileNotFoundException{
        String epoch= Integer.toString((int)(System.currentTimeMillis()/1000.0));
        epoch = "SDDA_"+epoch;
        File dir = new File("Networks/"+netName+"/Inputs/"+epoch);
    
        // attempt to create the directory here
        boolean successful = dir.mkdir();
        if (!successful){
            System.out.println("failed trying to create the directory");
        }
        this.partitionOutputFolderName= "Networks/"+netName+"/Inputs/"+epoch;
        
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File(this.partitionOutputFolderName+"/subnetNames.txt"), false /* append = true */));
        for(int i=0;i<numCluster;i++){
           fileOut.println("subnet_"+i);
        }
        fileOut.close();
        
        return epoch;
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
}
