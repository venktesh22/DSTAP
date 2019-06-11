/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.ArtificialODPair;
import dstap.ODs.ODPair;
import dstap.links.*;
import dstap.nodes.Node;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public class MasterNetwork extends Network{
    protected Map<Integer, Node> boundaryNodesByID;// nodes at the urban boundaries
    protected  Set<SubNetwork> subNetworks;//list of subnetworks modeld in the master network
    //protected Map<Link, Set<ODPair>> physicalLink_subNetODSet; 
    
    protected Set<ArtificialLink> artificialLinks;
    /*
    for each artificial link in the master net saves the associated OD pair in sub network
    shouldn't it be called artificialLink to subNetODSet? No longer needed as each artificial
    link class has an associated OD pair on its own
     */
    
    public MasterNetwork(int verbosityLevel, String name){
        super(verbosityLevel);
        networkName = name;
        boundaryNodesByID = new HashMap<>();
        
        subNetworks = new HashSet<>();
        //physicalLink_subNetODSet = new HashMap<>();
        
        artificialLinks = new HashSet<>();
    }
    
    /**
     * Reads regional links; creates link objects and adds those to links and physicalLinks variable
     * Also stores nodes in nodesByID (not in nodes variable yet)
     * @param fileName 
     */
    public void readNetwork(String fileName){
        try (Scanner inputFile = new Scanner(new File(fileName))){
            if(this.printVerbosityLevel>=3)
                System.out.println("Reading master network..");
            inputFile.nextLine();//skip the header line

            while (inputFile.hasNext()){
                int originId = inputFile.nextInt();
                int destId = inputFile.nextInt();
                double fft = inputFile.nextDouble();
                double coef = inputFile.nextDouble();
                double power = inputFile.nextDouble();
                double cap = inputFile.nextDouble();
                inputFile.nextLine();

                Node origin = null, dest = null;
                if (!nodesByID.containsKey(originId)){
                    nodesByID.put(originId, origin = new Node(originId, "masterNet"));
                    boundaryNodesByID.put(originId, origin);
                }
                if (!nodesByID.containsKey(destId)){
                    nodesByID.put(destId, dest = new Node(destId, "masterNet"));
                    boundaryNodesByID.put(destId, dest);
                }
                origin = nodesByID.get(originId);
                dest = nodesByID.get(destId);
                /*
                create the link in master net
                */
                Link l=new Link(origin, dest, fft, coef, power, cap, "masterNet");
                links.add(l);
                physicalLinks.add(l);
            }
            if(this.printVerbosityLevel >=3)
                System.out.println("Master network reading complete..");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void addSubNetwork(SubNetwork s){
        subNetworks.add(s);
    }
    
    /**
     * these are OD pairs from subnetwork where one of the endpoints is in the other subnet
     * here we create the nodes in master net and the associated OD pair
     * still need to create links between them and the boundary nodes
     * @param origin_id
     * @param dest_id
     * @param demand
     */
    public void addSubNetODPairs(int origin_id, int dest_id, double demand){
        Node origin =null, dest = null;
        if (!nodesByID.containsKey(origin_id)){
            nodesByID.put(origin_id, new Node(origin_id, this.networkName));
        }

        if (!nodesByID.containsKey(dest_id)){
            nodesByID.put(dest_id, new Node(dest_id, this.networkName));
        }
        origin = nodesByID.get(origin_id);
        dest = nodesByID.get(dest_id);

        if (demand > 0){
            tripTable.addODpair(origin, dest, demand);
        }
    }
    
    /**
     * create the artificial links in the master network between the regional origin/dest and boundary nodes
     * the initial tt=ftt
     * @param origin_id
     * @param dest_id
     * @param fft
     * @param subNetODPair the OD pair for this artificial link
     * @return the artificial link created in the master network
     */
    public ArtificialLink createArtificialLinks(int origin_id, int dest_id, 
            double fft, ArtificialODPair subNetODPair){
        ArtificialLink l=null;
        boolean doesItAlreadyExist= false;
        /*
        (t_h-t'x_h)(1+{t'}/{t_h-t'x_h} x))
         */
        Node origin = nodesByID.get(origin_id);
        Node dest = nodesByID.get(dest_id);
        double fftime = fft;
        double coef = 0;
        int power = 1;
        double cap = 1;
        
        for(Link l2: origin.getOutgoing()){
            if(l2.getDest().equals(dest) && l2 instanceof ArtificialLink){
                doesItAlreadyExist=true;
                l=(ArtificialLink)l2;
                break;
            }
        }
        
        if(!doesItAlreadyExist)
            links.add(l = new ArtificialLink(origin, dest, fftime, 
                    coef, power, cap, networkName));

        artificialLinks.add(l);
        if(subNetODPair!=null)
            l.setAssociatedODPair(subNetODPair);
        else{
            System.out.println("Subnetwork OD pair for link "+l+" is "+null);
            System.exit(1);
        }
            
        return l;
    }
    
    /**
     * This function updates the demand of the subnetwork OD pair using flow on artificial links
     */
    public void updateSubnetODsDemand(){
        if(this.printVerbosityLevel >= 1.0){
            System.out.println("Updating demand for subnetwork ODs corresponding to master artificial links");
        }
        for(ArtificialLink l: this.artificialLinks){
            l.getAssociatedODPair().updateDemandDueToALink(l.getFlow());
        }
        System.out.println("Done");
    }
    
    public void printMasterNetworkStatistics(){
        printNetworkStatistics();
        System.out.println(" Boundary Nodes = "+boundaryNodesByID.keySet());
        System.out.println(" No of artificial links = "+artificialLinks.size());
        System.out.println(" Artificial Links = "+artificialLinks);
        
        int artifiODPairsNumber =0;
        if(this.printVerbosityLevel>=3)
            System.out.println("Artificial OD pairs");
        for(Node origin: tripTable.getOrigins()){
            for(ODPair od: tripTable.byOrigin(origin)){
                if(od instanceof ArtificialODPair){
                    if(this.printVerbosityLevel>=3)
                        System.out.print("\t"+od);
                    artifiODPairsNumber++;
                }
            }
        }
        System.out.println("");
        System.out.println("Total no of artificial OD pairs = "+artifiODPairsNumber);
        //System.out.println(" Physical Link to SubNetODPair mapping is "+physicalLink_subNetODSet);
    }
    
}
