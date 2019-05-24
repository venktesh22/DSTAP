/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.*;
import dstap.links.ArtificialLink;
import dstap.links.ExtraNodeLink;
import dstap.links.Link;
import dstap.nodes.ExtraNode;
import dstap.nodes.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public class SubNetwork extends Network {
    private final MasterNetwork masterNet;
    
    //each subnetwork has an origin in master net if there is a demand from that origin to dest in another subnet.
    //the following variable stores those origins so we can easily access those for updating a-link parameters
    private Set<Node> originsInMasterNet; 
    private Set<Node> destsInMasterNet;
    public Set<Node> originsInThisSubnetDueToOtherSubnet;
    public Set<Node> destInThisSubnetDueToOtherSubnet;
    
    private List<SubNetwork> otherSubNet = new ArrayList<>();
    private Set<Node> boundaryNodes;//the boundary nodes for this subnet
    private Set<ExtraNode> extraNodes; //other subnetwork boundary nodes modeled as extra nodes
    
    protected Set<ArtificialLink> artificialLinks;
    private Set<ExtraNodeLink> extraNodeLinks;
    
    //private Set<ArtificialODPair> artificialODs;
    //useful for running dijkstra once by origin
    //private Map<Node, Set<ArtificialODPair>> artificialODsByOrigin;
    
    public SubNetwork(MasterNetwork masterNetwork, String verbosityLevel, String name){
        super(verbosityLevel);
        networkName = name;
        
        masterNet = masterNetwork;
        boundaryNodes = new HashSet<>();
        extraNodes = new HashSet<>();
        extraNodeLinks = new HashSet<>();
        artificialLinks = new HashSet<>();
//        artificialODs = new HashSet<>();
//        artificialODsByOrigin = new HashMap<>();
        
        originsInMasterNet = new HashSet<>();
        destsInMasterNet = new HashSet<>();
        originsInThisSubnetDueToOtherSubnet= new HashSet<>();
        destInThisSubnetDueToOtherSubnet= new HashSet<>();
    }
    
    /*=====================================
    ============Read networks==============
    =======================================
    */
    
    public void readNetwork(String fileName){
        try(Scanner inputFile = new Scanner(new File(fileName))){
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Reading network data for subnetwork "+ networkName+"..");
            inputFile.nextLine();
            
            while (inputFile.hasNext()){
                int origin_id = inputFile.nextInt();
                int dest_id = inputFile.nextInt();

                Node source, dest;

                if (!nodesByID.containsKey(origin_id)){
                    nodesByID.put(origin_id, new Node(origin_id, networkName));
                }
                source = nodesByID.get(origin_id);

                if (!nodesByID.containsKey(dest_id)){
                    nodesByID.put(dest_id, new Node(dest_id, networkName));
                }
                dest = nodesByID.get(dest_id);
                double fftime = inputFile.nextDouble();
                double coef = inputFile.nextDouble();
                double power = inputFile.nextDouble();
                double capacity = inputFile.nextDouble();		// units: vph

                Link l=new Link(source, dest, fftime, coef, power, capacity, networkName);
                if(l==null)
                    System.out.println("haha");
                links.add(l);
                physicalLinks.add(l);
                /*
                see if these are boundary nodes
                 */
                if (masterNet.boundaryNodesByID.containsKey(origin_id)){
                    if (!boundaryNodes.contains(source))
                        boundaryNodes.add(source);
                }
                if (masterNet.boundaryNodesByID.containsKey(dest_id)){
                    if (!boundaryNodes.contains(dest))
                        boundaryNodes.add(dest);
                }
                inputFile.nextLine();
            }
            inputFile.close();
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Completed..");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void readInTrips(String fileName, double demandFactor){
        try(Scanner inputFile = new Scanner(new File(fileName))){
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Reading in-trips for subnetwork "+ networkName+"..");
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
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Completed..");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void readOutTrips(String fileName, double demandFactor){
        try(Scanner inputFile = new Scanner(new File(fileName))){
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Reading out-trips for subnetwork "+ networkName+"..");
            int origin_id = -1;
            while(inputFile.hasNext()){
                String next = inputFile.next();
                if(next.equals("Origin")){
                    origin_id = inputFile.nextInt();
                    if (!nodesByID.containsKey(origin_id)){
                        System.out.println("SubNet - origin "+origin_id+" not read before\t");
                        System.exit(1);
                    }
                    originsInMasterNet.add(nodesByID.get(origin_id));
                }
                else{
                    int dest_id = Integer.parseInt(next);
                    inputFile.next(); // ":"
                    String temp = inputFile.next();
                    double trips = Double.parseDouble(temp.substring(0, temp.length()-1));
                    trips = trips*demandFactor;

                    if(trips==0)
                        continue;
                    if (nodesByID.containsKey(dest_id)){
                        System.out.println("SubNet - destination "+dest_id+" belongs to same subnet but is put in Out-trips file");
                        System.exit(1);
                    }

                    /*
                    checks if the node dest_id belongs to each subNetwork and adds Dest in masterNet only for that subNetwork
                    add this dest to the list of dest in master net for other sub net
                     */
                    boolean destIdIsThereInSomeSubNet = false;
                    for(SubNetwork otherNet: otherSubNet){
                        destIdIsThereInSomeSubNet = otherNet.addDestInMasterNet(dest_id);
                        if(destIdIsThereInSomeSubNet)
                            break;
                    }

                    if(!destIdIsThereInSomeSubNet){
                        System.out.println("SubNet - dest in master not read before\t"+dest_id);
                        System.exit(1);
                    }

                    //add the information of this OD pair to master net
                    masterNet.addSubNetODPairs(origin_id, dest_id, trips);
                }
            }
            inputFile.close();
            if("MEDIUM".equals(printVerbosityLevel))
                System.out.println("Completed..");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void setOtherSubNet(List<SubNetwork> temp){
        otherSubNet = temp;
    }
    
    /**
     *this node has demand from other subnetwork
     *add it the the list of dest in master net
    */
    public boolean addDestInMasterNet(int dest_id){
        if (!nodesByID.containsKey(dest_id)){
            return false;
        }
        destsInMasterNet.add(nodesByID.get(dest_id));
        return true;
    }
    
    public void generateOriginsAndDestDueToOtherSubnet(){
        for(SubNetwork otherNet: otherSubNet){
            for(Node o: otherNet.boundaryNodes){
                for(Node d: otherNet.boundaryNodes){
                    if(o.getId()!=d.getId())
                        this.addOtherSubNetODPairs(o.getId(), d.getId(), 0.0);
                }
            }
        }
    }
    
    /**
     * these are OD pairs created in THIS subnetwork where the endpoints are in the other subnet
     * We call these "EXTRA" nodes
     * origin artificial nodes have 10000 added to them and destinations have 20000 added to them
     * here we create the nodes in this subnetwork (extra nodes) and the associated regular OD pair
     * @param origin_id
     * @param dest_id
     * @param demand
     */    
    public void addOtherSubNetODPairs(int origin_id, int dest_id, double demand){
        Node origin =null, dest = null;
        if (!nodesByID.containsKey(origin_id+10000))
        {
            ExtraNode n = new ExtraNode(origin_id+10000, networkName);
            nodesByID.put(origin_id+10000, n);
            extraNodes.add(n);
        }

        if (!nodesByID.containsKey(dest_id+20000))
        {
            ExtraNode n = new ExtraNode(dest_id+20000, networkName);
            nodesByID.put(dest_id+20000, n);
            extraNodes.add(n);
        }
        origin = nodesByID.get(origin_id+10000);
        dest = nodesByID.get(dest_id+20000);

        //Create a copy of masternetLink called ExtraNode link from each of origin and destination
        generateExtraNodeLinkForEquivalentRegionalLink(origin, dest);
        
        tripTable.addODpair(origin, dest, demand); //this is a regular OD pair because artificialODpairs can only parallel an artificial link
        
        if(!originsInThisSubnetDueToOtherSubnet.contains(origin))
            originsInThisSubnetDueToOtherSubnet.add(origin);
        if(!destInThisSubnetDueToOtherSubnet.contains(dest))
            destInThisSubnetDueToOtherSubnet.add(dest);
        
        if("MEDIUM".equals(printVerbosityLevel))
            System.out.println("Subnetwork "+this+" has following extraNodeLinks "+extraNodeLinks);
    }
    
    /**
     * Finds the regional links for all links from origin in master network
     * @todo: modularize the function. It is doing two very different things together.
     * @param o
     * @param d 
     */
    private void generateExtraNodeLinkForEquivalentRegionalLink(Node o, Node d){
        for(Link l: masterNet.nodesByID.get(o.getId()-10000).getOutgoing()){
            if(masterNet.physicalLinks.contains(l)){
                //create links only for physical links in master net
                
                Node source= nodesByID.get(o.getId()); //as o is already added to nodesByID
                Node dest= nodesByID.get(l.getDest().getId());

                ExtraNodeLink newLink = new ExtraNodeLink(source, dest, l.getFFTime(),
                        l.getCoef(), 
                        l.getPower(), l.getCapacity(), networkName);
                if(!links.contains(newLink)){
                    links.add(newLink);
                    extraNodeLinks.add(newLink);
                    newLink.setAssociatedPhysicalLink(l);
                }
                //Also create an OD pair
                //@todo: for now not doing it;
//                if(tripTable.getTrips().containsKey(source)){
//                    if (!tripTable.getTrips().get(source).containsKey(dest)){
//                        tripTable.addODpair(source, dest, 0);
//                    }
//                }
//                else
//                    tripTable.addODpair(source, dest, 0);
//                ODPair subNetODPair = tripTable.getODPair(source, dest);
//                
//                Set<ODPair> temp = new HashSet<>();
//                if(masterNet.physicalLink_subNetODSet.containsKey(l))
//                    temp= masterNet.physicalLink_subNetODSet.get(l);
//                temp.add(subNetODPair);
                
            }
        }
        for(Link l: masterNet.nodesByID.get(d.getId()-20000).getIncoming()){
            if(masterNet.physicalLinks.contains(l)){
                Node source= nodesByID.get(l.getSource().getId()); //as o is already added to nodesByID
                Node dest= nodesByID.get(d.getId());

                ExtraNodeLink newLink = new ExtraNodeLink(source, dest, l.getFFTime(), l.getCoef(), 
                        l.getPower(), l.getCapacity(), networkName);
                if(!links.contains(newLink)){
                    links.add(newLink);
                    extraNodeLinks.add(newLink);
                    newLink.setAssociatedPhysicalLink(l);
                }
                
                //Also create an OD pair
                //@todo: for now not doing it;
//                if(tripTable.getTrips().containsKey(source)){
//                    if (!tripTable.getTrips().get(source).containsKey(dest)){
//                        tripTable.addODpair(source, dest, 0);
//                    }
//                }
//                else
//                    tripTable.addODpair(source, dest, 0);
//                ODPair subNetODPair = tripTable.getODPair(source, dest);
//                
//                Set<ODPair> temp = new HashSet<>();
//                if(masterNet.physicalLink_subNetODSet.containsKey(l))
//                    temp= masterNet.physicalLink_subNetODSet.get(l);
//                temp.add(subNetODPair);
            }
        }
    }
    
    //for origins in master network, connect that origin to all boundary nodes reachable by the origin
    
    public void createMasterNetArtificialLinksAndItsODPair(){
        //OD pair and artificial link from origin to boundary nodes
        for (Node origin : originsInMasterNet){
            ///see which boundary nodes are accessible from this origin/
            dijkstras(origin);
            for (Node dest : boundaryNodes){
                if (dest.label < Double.MAX_VALUE && dest.getId()!=origin.getId()){
                    //dest node is accessible from origin
//                    if(tripTable.getOrigins().contains(origin)){
//                        if(!tripTable.getTrips().get(origin).containsKey(dest))
//                            tripTable.addArtificialODPair(origin, dest, 0.0);
//                    }
//                    else
                    tripTable.addArtificialODPair(origin, dest, 0.0);
                    ArtificialODPair subNetODPair = tripTable.getArtificialODPair(origin, dest);

                    /*
                       create the link in master net
                       cost is fftt
                    */
                    ArtificialLink masterArtifiLink = masterNet.createArtificialLinks(origin.getId(), 
                            dest.getId(), dest.label, subNetODPair);
                    
                    subNetODPair.setAssociatedALink(masterArtifiLink);
                    //@todo: make sure artificialODPairs can be accessed by origin
                    
                }
            }
        }
        //OD pair and artificial link from boundary to dest
        for (Node origin : boundaryNodes){
            dijkstras(origin);
            for (Node dest : destsInMasterNet){
                if (dest.label < Double.MAX_VALUE && dest.getId()!=origin.getId()){
                    //dest node accessible from origin
//                    if(tripTable.getOrigins().contains(origin)){
//                        if(!tripTable.getTrips().get(origin).containsKey(dest))
//                            tripTable.addArtificialODPair(origin, dest, 0.0);
//                    }
//                    else
                    tripTable.addArtificialODPair(origin, dest, 0);
                    //tripTable.addODpair(origin, dest, 0);
                    ArtificialODPair subNetODPair = tripTable.getArtificialODPair(origin, dest);

                    /*
                       create the link in master net
                    */
                    ArtificialLink masterArtifiLink = masterNet.createArtificialLinks(origin.getId(), 
                            dest.getId(), dest.label, subNetODPair);
                    
                    subNetODPair.setAssociatedALink(masterArtifiLink);
                }
            }
        }
    }
    
    public void createThisSubnetODPairs_otherSubnetALink(){
        for(Node origin: originsInThisSubnetDueToOtherSubnet){
            dijkstras(origin);
            for(Node dest: destInThisSubnetDueToOtherSubnet){
                if (dest.label < Double.MAX_VALUE && dest.getId()!=origin.getId()){
                    if(origin.getId()-10000 == dest.getId()-20000)
                        continue; //same nodes so skip creating any OD pair or so
//                    if(tripTable.getOrigins().contains(origin)){
//                        if(!tripTable.getTrips().get(origin).containsKey(dest))
//                            tripTable.addArtificialODPair(origin, dest, 0.0);
//                    }
//                    else
                    tripTable.addArtificialODPair(origin, dest, 0);
                    ArtificialODPair subNetODPair = tripTable.getArtificialODPair(origin, dest);

                    //create the link in other subnetwork
                    //@todo: handle more than two partitions for creating subnetwork artificial links
                    if(otherSubNet.size()>1){
                        System.out.println("More than two partitions cannot be handled for now");
                        System.exit(1);
                    }
                    for(SubNetwork otherNet: otherSubNet){
                        ArtificialLink otherSubnetALink = otherNet.createSubNetArtificialLinks(origin.getId()-10000, 
                                dest.getId()-20000, dest.label, subNetODPair);
                        otherSubnetALink.setAssociatedODPair(subNetODPair);
//                        thisSubnetOD_OtherSubnetALink.put(subNetODPair, otherSubnetALink);
//                        if(!artificialODPairsByOriginThisSubnet.containsKey(origin))
//                            artificialODPairsByOriginThisSubnet.put(origin, new HashSet<>());
//                        artificialODPairsByOriginThisSubnet.get(origin).add(subNetODPair);
//                        artificialODPairsThisSubnet.add(subNetODPair);
                    }
                }
            }
        }
    }
    /**
     * Creates an artificial link in current subnetwork object (note it is called
     * by other subnetwork artificial OD pair)
     * @param origin_id
     * @param dest_id
     * @param fft
     * @param subNetODPair
     * @return the created link
     */
    public ArtificialLink createSubNetArtificialLinks(int origin_id, int dest_id, double fft, ArtificialODPair subNetODPair){
        ArtificialLink l=null;
        boolean doesItAlreadyExist= false;
        if(Double.isInfinite(fft) || Double.isNaN(fft)){
            System.out.println("fft passed for creating subNet artificial links is "+fft+". Exiting...");
            System.exit(1);
        }
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
            if(l2.getDest().equals(dest) && l instanceof ArtificialLink){
                doesItAlreadyExist=true;
                l=(ArtificialLink)l2;
                break;
            }
        }
        if(!doesItAlreadyExist)
            links.add(l = new ArtificialLink(origin, dest, 
                    fftime, coef, power, cap, networkName));

        l.setAssociatedODPair(subNetODPair);
        this.artificialLinks.add(l);
        return l;
    }
    
    public void printSubNetworkStatistics(){
        printNetworkStatistics();
        System.out.println(" Boundary Nodes = "+boundaryNodes);
        System.out.println(" Extra Nodes = " + extraNodes);
        System.out.println(" Extra Node Links = "+ extraNodeLinks);
        System.out.println(" Artificial Links = "+artificialLinks);
        System.out.println(" Origins in Master Net = "+originsInMasterNet);
        System.out.println(" Dests in Master Net="+destsInMasterNet);
        System.out.println(" Origins in this subnetwork due to other subnetworks = "+originsInThisSubnetDueToOtherSubnet);
        System.out.println(" Dests in this subnetwork due to other subnetworks = "+destInThisSubnetDueToOtherSubnet);
        
        int artifiODPairsNumber =0;
        System.out.println("Artificial OD pairs");
        for(Node origin: tripTable.getOrigins()){
            for(ODPair od: tripTable.byOrigin(origin)){
                if(od instanceof ArtificialODPair){
                    System.out.print("\t"+od);
                    artifiODPairsNumber++;
                }
            }
        }
        System.out.println("");
        System.out.println("Total no of artificial OD pairs = "+artifiODPairsNumber);
        
    }
    
    //=============================================//
    //--methods for doing bush-based sensitivity---//
    //=============================================//
    public void updateArtificialLinks(boolean costFunc, double gap){
        for (Node origin : this.tripTable.getArtificialODPairs().keySet()){
            boolean shortestPathExists = false;
            for(Node dest: this.tripTable.getArtificialODPairs().get(origin).keySet()){
                ArtificialODPair od = this.tripTable.getArtificialODPairs().get(origin).get(dest);
                Stem stem = od.getStem();
                double d_0 = od.getDemandDueToALink();
                double t_0 = 0;
                double t_p = 0;
                
                //there may already be a path from true demand
                if(d_0 == 0 && stem.getPathSet().isEmpty()){
                    //there may be no path from origin to dest so bush-based sensitivity might be flawed
                    //@todo: handle this case. I kinda think that we don't need to do anything for this case
                    //For now, I am running dijkstra and adding one path...
                    //BUT what about the case where d_0=0, but there is true demand between OD pair, if you clear the path
                    //then the path flows will become zero and we will loose. Key is in knowing that some ODs may have
                    if (!shortestPathExists){
                        dijkstras(origin);
                        shortestPathExists = true;
                    }
                    Path temp = trace(od);
                    if(!temp.getPathLinks().isEmpty()){
                        stem.getPathSet().clear();
                        stem.addPath(temp);
                    }
                    t_0 = od.getDest().label;
                }
                else{
                    t_0 = od.getODCostAtUE();
                }
                runStemBasedSensitivityAnalysis(stem, gap);
                ArtificialLink aLink = od.getAssociatedALink();
                t_p = stem.getTimeDerivative();
                
                double aLinkFFTT = t_0 - t_p * d_0;
                double aLinkCoef=0;
                if(t_p / (t_0 - t_p * d_0) != 0)
                    aLinkCoef= t_p / (t_0 - t_p * d_0);
                else{
                    System.out.println("Artificial link ffTT is not set right. It cannot be zero or negative");
                    System.out.println("aLinkFFTT="+aLinkFFTT+" where t_0="+t_0+", t_p="+t_p+" and d_0="+d_0);
                    System.exit(1);
                }
                
//                System.out.println("Alink being updated="+aLink);
//                System.out.println("--New TT function="+aLinkFFTT+"(1+"+aLinkCoef+"x), where the demand using the aLink="+d_0);
                aLink.setFftime(aLinkFFTT);
                aLink.setCoef(aLinkCoef);
            }
        }

        if (this.printVerbosityLevel.equals("MEDIUM"))
            System.out.println("Subnetwork "+this+" sensitivity analysis completed");
    }
    
    /**
     * The method below is similar to the solver for traffic assignment but operating
     * in terms of sensitivity analysis parameters. Finds the time derivative for a stem
     * sets dx_ij/dX = 0 for all links
     * and distribute one unit flow equally between bush paths, 
     * updates dx_ij/dX and the path cost as sum of dt_ij/dX
     * @param stem: the stem over which sensitivity is being done
     * @param gap : relative gap to which the sensitivity analysis problem should be solved
     */
    public void runStemBasedSensitivityAnalysis(Stem stem, double gap){
        resetStemForSensitivity(stem);
        switch(stem.getPathSet().size()){
            case 0: System.out.println("Subnet - stem "+stem+ " with no path"+networkName);
                    System.exit(1);
                    break;
                
            case 1: stem.updateTimeDerivative();
                    break;
                
            default: int count=0;
                int odCounter = 0;
                double costDiff = stem.getCostDiffBetLongestAndShortestPaths(true);
                boolean isBushSensitivityConverged = (costDiff<gap);
//                            System.out.println("===Cost difference for this OD is "+costDiff);

                while (!isBushSensitivityConverged){// && odCounter<5){
                    odCounter++;
                    /*
                    shift flow between longest and shortest paths and update their associated link flow
                     */
//                                od.limitedShiftFlow(rate);
                    stem.shiftLimitedFlowFromAllPathstoSP(1.0,true);

                    stem.updateCost(true);
                    //od.getStem().dropPathsWFlowltThresh(1E-9); /* removes unused paths, but we ignore this in bush analysis */
                    costDiff = stem.getCostDiffBetLongestAndShortestPaths(true);
                    isBushSensitivityConverged = (costDiff<gap);
//                                isODConverged = true; //Forcing one iteration of Netwon's method for flow shifting.
                }
                stem.updateTimeDerivative();
                break;
        }
    }
    
    private void resetStemForSensitivity(Stem stem){
        for (Path p : stem.getPathSet()){
            for (Link l : p.getPathLinks()){
                l.setdxdX(0);
            }
        }
        /**
         * split one unit of demand equally between bush paths
         * assign flow to links
         */
        for (Path p : stem.getPathSet()){
            p.setSensitivityFlow((double)1/stem.getPathSet().size());
            p.updatePathSensitivityCost();/* updates the path cost as sum of dt_ij/dx_ij * dx_ij/dX */
        }
    }


    public Set<Node> getBoundaryNodes() {
        return boundaryNodes;
    }

    @Override
    public String toString() {
        return "" + this.networkName;
    }
    
    
}
