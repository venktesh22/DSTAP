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
 * contains some network specific functions used for SDDA algorithm
 * Not used in DSTAP algorithm...only for partitioning
 * @author vpandey
 */
public class SDDANetwork extends Network {
    public Map<Node, Integer> nodeRank;
    protected Map<Integer,Map<Integer,Link>> linksByID;
    protected Map<Integer, Map<Integer, Double>> odDemand; //origin zone to dest zone demand
    
    public SDDANetwork(){
        links=new HashSet<>();
        nodes=new HashSet<>();
        nodeRank = new HashMap<>();
        nodesByID= new HashMap<>();
        linksByID=new HashMap<>();
        odDemand= new HashMap<>();
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
    
    public void readBarGeraLinkInput(String netName) throws FileNotFoundException{
        List<Integer> validNodes= new ArrayList<>();
        
        boolean isSamNetwork=false;
        if(isSamNetwork){
            Scanner filein2 = new Scanner(new File("Networks/SAM/BFSResults.txt"));
            int countInvalidNodes=0;
            filein2.nextLine();
            while(filein2.hasNext()){
                int nodeID= filein2.nextInt();
                String category= filein2.next();
                if(category.equals("1"))
                    validNodes.add(nodeID);
                else
                    countInvalidNodes++;
            }
            filein2.close();
            System.out.println("Number of invalid nodes:"+countInvalidNodes);
        }
        
        
        System.out.println("Reading link file....");
        Scanner filein = new Scanner(new File("Networks/"+netName+"/"+netName+ "_net.txt"));
        
        for(int i=0;i<8;i++)
            filein.nextLine(); //ignore the metadata information
        
        int countIgnoredLinks=0;
        while (filein.hasNext()){
            int source_id = filein.nextInt();
            int dest_id = filein.nextInt();

            if(isSamNetwork && (!validNodes.contains(source_id)|| !validNodes.contains(dest_id))){
                String s=filein.nextLine();
//                System.out.println(s);
                countIgnoredLinks++;
                continue;
            }
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
        System.out.println("No. of links read: "+links.size());
        System.out.println("No. of nodes read: "+ nodesByID.size());
        System.out.println("No. of ignored links:"+countIgnoredLinks);
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

        while(inputFile.hasNext()){
            String next = inputFile.next();

            if(next.equals("Origin")){
                origin_id = inputFile.nextInt();

                if (!nodesByID.containsKey(origin_id)){
                    System.out.println("Origin not read before\t"+origin_id);
                    System.exit(1);
                }
                
            }
            else{
                int dest_id = Integer.parseInt(next);
                inputFile.next(); // ":"
                String temp = inputFile.next();
                double trips = Double.parseDouble(temp.substring(0, temp.length()-1));

                if(dest_id==origin_id)
                    continue;
                if (!nodesByID.containsKey(dest_id)){
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
//        if(addODPairLinks)
//            createODPairLinks();
    }
    
    
    
}
