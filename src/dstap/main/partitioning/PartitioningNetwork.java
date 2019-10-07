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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Extends Network class
 * contains some network specific functions used for partitioning algorithm
 * Not used in DSTAP algorithm...only for partitioning
 * @author vpandey
 */
public class PartitioningNetwork extends Network {
    public Map<Node, Integer> nodeRank; //fopr SDDA algorithm
    protected Map<Integer,Map<Integer,Link>> linksByID;
    protected Map<Integer, Map<Integer, Double>> odDemand; //origin zone to dest zone demand
    protected int firstThruNodeID;
    
    protected Map<Link, Double> UELinkFlow;
    protected Map<Link, Double> UELinkCost;
    
    Map<Integer,Node> centroidsByID;
    Map<Integer,Map<Integer,Link>> centroidConnectorsByID;
    
    public PartitioningNetwork(){
        links=new HashSet<>();
        nodes=new HashSet<>();
        nodeRank = new HashMap<>();
        nodesByID= new HashMap<>();
        linksByID=new HashMap<>();
        odDemand= new HashMap<>();
        centroidsByID = new HashMap<>();
        centroidConnectorsByID = new HashMap<>();
        
        UELinkFlow = new HashMap<>();
        UELinkCost = new HashMap<>();
    }
    
    public Set<Link> getLinks() {
        return links;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Map<Integer, Map<Integer, Link>> getLinksByID() {
        return linksByID;
    }

    public Map<Integer, Node> getNodesByID() {
        return nodesByID;
    }
    
//    @SuppressWarnings("empty-statement")
    public void readBarGeraLinkInput(String netName) throws FileNotFoundException{
        this.networkName= netName;
        System.out.println("Reading link file....");
        Scanner filein = new Scanner(new File("Networks/"+netName+"/"+netName+ "_net.txt"));
        
//        for(int i=0;i<8;i++)
//            filein.nextLine(); //ignore the metadata information
        //read metadata information for first through node
        String line;
        int firstThroughNode = 0;
        while(!(line = filein.nextLine()).contains("<END OF METADATA>")){
            if(line.contains("<FIRST THRU NODE>")){
                firstThroughNode = Integer.parseInt(line.substring(line.indexOf('>')+1).trim());
            }
        }
        while((line = filein.nextLine()).trim().length() == 0); //read till the info begins; also reads the header
        this.firstThruNodeID = firstThroughNode;
        //we will not add centroids and centroid connectors to the network file to ensure that partitions 
        //do not cut the connectors (As that can leave the demand disconnected)
        
        int countCentroidConn = 0;
        while (filein.hasNext()){
            int source_id = filein.nextInt();
            int dest_id = filein.nextInt();

            if(source_id<firstThroughNode || dest_id<firstThroughNode){
                //a centroid connector so do not consider this link
                //temporarily I call both nodes at either end of centroid connector as centroids
                Node source=null, dest=null;
                Link ll;
                if(centroidsByID.containsKey(source_id)){
                    source = centroidsByID.get(source_id);
                } else{
                    centroidsByID.put(source_id, source = new Node(source_id,"Partitioning"));
                }
                if(centroidsByID.containsKey(dest_id)){
                    dest = centroidsByID.get(dest_id);
                } else{
                    centroidsByID.put(dest_id, dest = new Node(dest_id,"Partitioning"));
                }
                //next we pick right node objects for source and dest
//                if(source_id<firstThroughNode){
//                    //then source is centroid and dest must be a regular node
//                    
//                }
//                else if(dest_id<firstThroughNode){
//                    //then source is centroid and dest must be a regular node
//                    if(nodesByID.containsKey(source_id)){
//                        source = nodesByID.get(source_id);
//                    } else{
//                        nodesByID.put(source_id, source = new Node(source_id,"Partitioning"));
//                    }
//                    if(centroidsByID.containsKey(dest_id)){
//                        dest = centroidsByID.get(dest_id);
//                    } else{
//                        centroidsByID.put(dest_id, dest = new Node(dest_id,"Partitioning"));
//                    }
//                }
//                else{
//                    System.out.println("It cannot happen that both source "+ source_id+" and dest "+ dest_id+" are centroids. Check please.");
//                    System.exit(1);
//                }
                
                double capacity = filein.nextDouble();		// units: vph
                double length = filein.nextDouble();		// units: miles
                double fftime = filein.nextDouble(); 	// units: seconds
                double B = filein.nextDouble();
                double power = filein.nextDouble();

                ll = new Link(source, dest, fftime, B, power, capacity, "Partitioning");
                filein.nextLine();
                if(centroidConnectorsByID.containsKey(source_id)){
                    Map<Integer,Link> temp= centroidConnectorsByID.get(source_id);
                    temp.put(dest_id, ll);
                    centroidConnectorsByID.put(source_id, temp);
                }
                else{
                    Map<Integer,Link> temp= new HashMap<>();
                    temp.put(dest_id, ll);
                    centroidConnectorsByID.put(source_id, temp);
                }
                countCentroidConn++;
            }
            else{
                //not a centroid connector so consider this link
                Node source, dest;
                Link ll;
                if(nodesByID.containsKey(source_id)){
                    source = nodesByID.get(source_id);
                } else{
                    nodesByID.put(source_id, source = new Node(source_id,"Partitioning"));
                    nodes.add(source);
                }
                if(nodesByID.containsKey(dest_id)){
                    dest = nodesByID.get(dest_id);
                } else{
                    nodesByID.put(dest_id, dest = new Node(dest_id,"Partitioning"));
                    nodes.add(dest);
                }
                double capacity = filein.nextDouble();		// units: vph
                double length = filein.nextDouble();		// units: miles
                double fftime = filein.nextDouble(); 	// units: seconds
                double B = filein.nextDouble();
                double power = filein.nextDouble();

                ll = new Link(source, dest, fftime, B, power, capacity, "Partitioning");
                links.add(ll);
                filein.nextLine();
                if(linksByID.containsKey(source_id)){
                    Map<Integer,Link> temp= linksByID.get(source_id);
                    temp.put(dest_id, ll);
                    linksByID.put(source_id, temp);
                }
                else{
                    Map<Integer,Link> temp= new HashMap<>();
                    temp.put(dest_id, ll);
                    linksByID.put(source_id, temp);
                }
            }
        }
        System.out.println("No. of links read: "+links.size());
        System.out.println("No. of nodes read: "+ nodesByID.size());
        System.out.println("No of centroids="+centroidsByID.size());
        System.out.println("No of centroid connectors: "+countCentroidConn);
        filein.close();
    }
    
    public void readBarGeraODFile(String netName) throws FileNotFoundException{
        Scanner inputFile = new Scanner(new File("Networks/"+netName+"/"+netName+ "_trips.txt"));
        int numZones = 0;
        String line;
        
        while(!(line = inputFile.nextLine()).contains("<END OF METADATA>")){
            if(line.contains("<NUMBER OF ZONES>")){
                numZones = Integer.parseInt(line.substring(line.indexOf('>')+1).trim());
            }
        }
        
        int origin_id = -1;
        double totalDemand=0.0;
        while(inputFile.hasNext()){
            String next = inputFile.next();
            if(next.equals("Origin")){
                origin_id = inputFile.nextInt();
                if (!nodesByID.containsKey(origin_id) && !centroidsByID.containsKey(origin_id)){
                    System.out.println("Origin not read before\t"+origin_id);
                    System.exit(1);
                }
            }
            else{
                int dest_id = Integer.parseInt(next);
                inputFile.next(); // ":"
                String temp = inputFile.next();
                double trips = Double.parseDouble(temp.substring(0, temp.length()-1));
                totalDemand+= trips;
                if(dest_id==origin_id)
                    continue; //ignore self demand
                
//                totalDemand+= trips;
                if (!nodesByID.containsKey(dest_id) && !centroidsByID.containsKey(dest_id)){
                    System.out.println("No dest node exits\t"+origin_id+"\t"+dest_id);
                    System.exit(1);
                }
                //write the demand
                if(!odDemand.containsKey(origin_id)){
                    Map<Integer, Double> temp2 = new HashMap<>();
                    temp2.put(dest_id, trips);
                    odDemand.put(origin_id, temp2);
                }
                else{
                    Map<Integer, Double> temp2 = odDemand.get(origin_id);
                    temp2.put(dest_id, trips);
                    odDemand.put(origin_id, temp2);
                }
            }
        }
        inputFile.close();
        System.out.println("Total demand="+totalDemand);
//        if(addODPairLinks)
//            createODPairLinks();
    }
    
    public void readCompleteNetLinkFlowsAndCosts() throws FileNotFoundException{
        System.out.println("Reading link file....");
        Scanner filein = new Scanner(new File("Networks/"+this.networkName+"/"+this.networkName+ "_Flow.txt"));
        
        filein.nextLine(); //ignore first line
        while(filein.hasNext()){
            int sourceID = filein.nextInt();
            int destID= filein.nextInt();
            double flow= filein.nextDouble();
            double cost= filein.nextDouble();
            Link l=null;
            if(sourceID<this.firstThruNodeID || destID<this.firstThruNodeID){
                //centroid connector
                l= centroidConnectorsByID.get(sourceID).get(destID);
            }
            else{
                //regular link
                if(this.networkName.equals("Austin_sdb")){
                    //austin network flow files are weird and have extra links not in the network
                    if(!linksByID.containsKey(sourceID))
                        continue;
                    else if(!linksByID.get(sourceID).containsKey(destID))
                        continue;
                }
                l= linksByID.get(sourceID).get(destID);
            }
            this.UELinkFlow.put(l, flow);
            this.UELinkCost.put(l, cost);
        }
        filein.close();
    }

    public int getFirstThruNodeID() {
        return firstThruNodeID;
    }
    
    public UndirectedNetwork convertToUndirected(){
        UndirectedNetwork net = new UndirectedNetwork();
        for(Link l: this.links){
            net.addLink(l);
        }
        net.checkNetwork();
        return net;
    }
    
}
