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
    protected Map<Link, Set<ODPair>> physicalLink_subNetODSet;
    /*
    for each artificial link in the master net saves the associated OD pair in sub network
     */
    
    public MasterNetwork(String verbosityLevel, String name){
        super(verbosityLevel);
        networkName = name;
        boundaryNodesByID = new HashMap<>();
        
        subNetworks = new HashSet<>();
        physicalLink_subNetODSet = new HashMap<>();
    }
    
    /**
     * Reads regional links; creates link objects and adds those to links and physicalLinks variable
     * Also stores nodes in nodesByID (not in nodes variable yet)
     * @param fileName 
     */
    public void readNetwork(String fileName){
        try (Scanner inputFile = new Scanner(new File(fileName))){
            if("MEDIUM".equals(printVerbosityLevel))
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
                if (!nodesByID.containsKey(originId))
                {
                    nodesByID.put(originId, origin = new Node(originId, "masterNet"));
                    boundaryNodesByID.put(originId, origin);
                }
                if (!nodesByID.containsKey(destId))
                {
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
            if("MEDIUM".equals(printVerbosityLevel))
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
            double fft, ArtificialODPair subNetODPair)
    {
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

        if(subNetODPair!=null)
            l.setAssociatedODPair(subNetODPair);
        else{
            System.out.println("Subnetwork OD pair for link "+l+" is "+null);
            System.exit(1);
        }
            
        return l;
    }
}