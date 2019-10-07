/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.ArtificialODPair;
import dstap.ODs.ODPair;
import dstap.main.partitioning.*;
import dstap.links.Link;
import dstap.network.Network;
import dstap.nodes.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Extends Network class
 * this simply contains the function to read complete network file (directly from BarGera format)
 * Not used in DSTAP algorithm...only used for solving the entire network using gradient projection algorithm
 * @author vpandey
 */
public class CompleteNetwork extends Network {
    protected Map<Integer,Map<Integer,Link>> linksByID;
    
//    public CompleteNetwork(int verbosityLevel, String name){
//        super(verbosityLevel);
//        networkName = name;
//    }
    
    public CompleteNetwork(){
        super(3);
        networkName = "completeNet";
        linksByID = new HashMap<>();
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
        System.out.println("Reading link file....");
        Scanner filein = new Scanner(new File("Networks/"+netName+"/"+netName+ "_net.txt"));
        
        for(int i=0;i<8;i++)
            filein.nextLine(); //ignore the metadata information
        
        int countIgnoredLinks=0;
        while (filein.hasNext()){
            int source_id = filein.nextInt();
            int dest_id = filein.nextInt();

//            if((!validNodes.contains(source_id)|| !validNodes.contains(dest_id))){
//                String s=filein.nextLine();
////                System.out.println(s);
//                countIgnoredLinks++;
//                continue;
//            }
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
        filein.close();
    }
    
    public void readBarGeraODFile(String netName, double demandFactor) throws FileNotFoundException{
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
                    nodesByID.put(origin_id, new Node(origin_id, networkName));
                }
            }
            else{
                int dest_id = Integer.parseInt(next);
                inputFile.next(); // ":"
                String temp = inputFile.next();
                double trips = Double.parseDouble(temp.substring(0, temp.length()-1));

                trips = trips*demandFactor;
                Node origin, dest;

                if (!nodesByID.containsKey(dest_id)){
                    nodesByID.put(dest_id, new Node(dest_id, networkName));
                }

                origin = nodesByID.get(origin_id);
                dest = nodesByID.get(dest_id);
                if (trips > 0){
                    tripTable.addODpair(origin, dest, trips);
                }
            }
        }
        inputFile.close();
    }
    
    public void writeFlowAndGapOutput(String folderName){
        try{
            PrintWriter fileOut = new PrintWriter(new File(folderName+"CompleteNetwork_linkFlows.txt"));
            fileOut.println("source \t destination \t fft \t flow \t cost(min)");
            for(Link l : links){
                fileOut.println(l.getSource().getId()+"\t"+l.getDest().getId()+"\t"+l.getFFTime()+"\t"+l.getFlow()+"\t"+l.getTravelTime());
            }
            fileOut.flush();
            fileOut.close();
            
            fileOut = new PrintWriter(new File(folderName+"CompleteNetwork_ODtravelTime.txt"));
            fileOut.println("Origin\tDestination\tDemand\tCost");
            for(Node origin : this.tripTable.getOrigins()){
                for(ODPair od : this.tripTable.byOrigin(origin)){
                    fileOut.println(origin.getId()+"\t"+od.getDest().getId()+"\t"+od.getDemand()+"\t"+od.getODCostAtUE());
                }
            }
            fileOut.flush();
            fileOut.close();
            
            fileOut = new PrintWriter(new File(folderName+"CompleteNetwork_GapValues.txt"));
            fileOut.println("IterationNo\tGap");
            int i=0;
            for(Double d: this.subIterationGapValues)
                fileOut.println((i++)+"\t"+d);
            fileOut.flush();
            fileOut.close();
            
            fileOut = new PrintWriter(new File(folderName+"CompleteNetwork_CompTime.txt"));
            fileOut.println("IterationNo\tsolveNetTime (ms)");
            i=0;
            for(Double d: this.computationTimePerSubItr)
                fileOut.println((i++)+"\t"+d);
            fileOut.flush();
            fileOut.close();
        }
        catch(IOException e){
            e.printStackTrace();
            return;
        }catch(RuntimeException e){
            e.printStackTrace();
            return;
        }
        
    }
    
}
