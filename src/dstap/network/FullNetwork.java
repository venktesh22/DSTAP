/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.ODPair;
import dstap.ODs.Path;
import dstap.links.ArtificialLink;
import dstap.links.Link;
import dstap.nodes.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains the complete network
 * @author vp6258
 */
public class FullNetwork extends Network{
    public Set<Link> boundaryLinks;//links connecting south and north subnetworks
    public Set<Node> boundaryNodes;//nodes connecting south and north subnetworks
    
    public Set<SubNetwork> subNetworks;
    public  MasterNetwork masterNet;
    public Map<Link, Link> otherNetLinkToFullLink;
    public Map<ODPair, ODPair> subnetODToFullnetOD;
    public Map<ODPair, ODPair> masternetODToFullnetOD;
    
    public FullNetwork(MasterNetwork master, List<SubNetwork> subNets, String name, String verbosity){
        super(verbosity);
        subNetworks = new HashSet<>();
        
        boundaryNodes = new HashSet<>();
        boundaryLinks = new HashSet<>();
        
        for(SubNetwork s: subNets)
            subNetworks.add(s);
//        subNetworks.add(north);
        masterNet = master;
        networkName = name;
        otherNetLinkToFullLink = new HashMap<>();
        subnetODToFullnetOD = new HashMap<>();
        masternetODToFullnetOD = new HashMap<>();
    }
    
    /**
     * Copies boundary links and OD pairs from the master network
     * We assume that there are no extra nodes in masternetwork not
     * part of any other subnetwork
     * @todo: fix this code to copy dangling master nodes to fullNetwork
     */
    public void copyMasterNet(MasterNetwork masterNet){
        Link ll;
        for (Link l : masterNet.links){
            boundaryLinks.add(ll = new Link(nodesByID.get(l.getSource().getId()), 
                    nodesByID.get(l.getDest().getId()), l.getFFTime(), l.getCoef(),
                    l.getPower(), l.getCapacity(), networkName));
            this.links.add(ll);
            this.physicalLinks.add(ll);
            otherNetLinkToFullLink.put(l, ll);
        }

        for(Node origin: masterNet.tripTable.getOrigins()){
            for(ODPair od: masterNet.tripTable.byOrigin(origin)){
                this.tripTable.addODpair(nodesByID.get(od.getOrigin().getId()), 
                        nodesByID.get(od.getDest().getId()), od.getDemand(), networkName);
            }
        }
    }
    
    /**
     * Copies subnetwork nodes to fullnetwork (hard copy)
     * @param subNet 
     */
    public void copySubNetwork(SubNetwork subNet){
        Node nn;
        for (Integer id : subNet.nodesByID.keySet()){
            this.nodes.add(nn = new Node(id, networkName));
            this.nodesByID.put(nn.getId(), nn);
        }
         /*
        copy links
         */
        Link ll;
        for (Link l : subNet.links){
            if(subNet.physicalLinks.contains(l)){
                this.links.add(ll = new Link(nodesByID.get(l.getSource().getId()), 
                        nodesByID.get(l.getDest().getId()), l.getFFTime(), 
                        l.getCoef(), l.getPower(), l.getCapacity(), networkName));
                this.physicalLinks.add(ll);
                otherNetLinkToFullLink.put(l, ll);
            }
        }
        for (Node n : subNet.getBoundaryNodes()){
            boundaryNodes.add(new Node(n.getId(), networkName));
        }
        
        for (Node origin : subNet.tripTable.getOrigins()){
            for (ODPair od : subNet.tripTable.byOrigin(origin)){
                this.tripTable.addODpair(nodesByID.get(od.getOrigin().getId()), 
                        nodesByID.get(od.getDest().getId()), od.getDemand(), networkName);
            }
        }
    }
    
    public void createODPairMappings(){
        for (SubNetwork subNet : subNetworks){
            for (Node subnetOrig : subNet.tripTable.getTrips().keySet()){
                for(Node subnetDest : subNet.tripTable.getTrips().get(subnetOrig).keySet()){
                    ODPair subnetOD = subNet.tripTable.getTrips().get(subnetOrig).get(subnetDest);
                    Node fullnetOrig = this.nodesByID.get(subnetOrig.getId());
                    Node fullnetDest = this.nodesByID.get(subnetDest.getId());
                    if(this.tripTable.getTrips().containsKey(fullnetOrig)){
                        if(this.tripTable.getTrips().get(fullnetOrig).containsKey(fullnetDest)){
                            ODPair fullOD = this.tripTable.getTrips().get(fullnetOrig).get(fullnetDest);
                            this.subnetODToFullnetOD.put(subnetOD, fullOD);
                        }
                    }
                }
            }
        }
        
        for (Node masternetOrig : masterNet.tripTable.getTrips().keySet()){
            for(Node masternetDest : masterNet.tripTable.getTrips().get(masternetOrig).keySet()){
                ODPair masternetOD = masterNet.tripTable.getTrips().get(masternetOrig).get(masternetDest);
                Node fullnetOrig = this.nodesByID.get(masternetOrig.getId());
                Node fullnetDest = this.nodesByID.get(masternetDest.getId());
                if(this.tripTable.getTrips().containsKey(fullnetOrig)){
                    if(this.tripTable.getTrips().get(fullnetOrig).containsKey(fullnetDest)){
                        ODPair fullOD = this.tripTable.getTrips().get(fullnetOrig).get(fullnetDest);
                        this.masternetODToFullnetOD.put(masternetOD, fullOD);
                    }
                }
            }
        }
        
    }
    
    /**
     * Maps OD flow from each subnetwork and master network to full net
     * Order of execution is relevant, especially if there are no subnetwork artificial links
     * @todo: rethink how this will change if we have subnet A links
     */
    public void mapDSTAPnetFlowToFullNet(){
        for (Link l : links)
            l.setFlow(0.0);
        
        //map subnetworks
        for (ODPair subnetOD: this.subnetODToFullnetOD.keySet()){
            mapSubnetODFlowsToFullOD(subnetOD, this.subnetODToFullnetOD.get(subnetOD));
            boolean subnetODConsistent = subnetOD.checkFlowConsistency();
            boolean fullODConsistent = this.subnetODToFullnetOD.get(subnetOD).checkFlowConsistency();
            if(!(subnetODConsistent) || !fullODConsistent){
                System.out.println("Flows on ODs are not consistent. subnetODConsistent="+subnetODConsistent+" and fullODConsistent="+fullODConsistent);
                System.exit(1);
            }
        }
        
        //find full net ODs corresponding to master net ODs and then determine
        for (ODPair masterOD : this.masternetODToFullnetOD.keySet()){
            mapMasterODFlowsToFullOD(masterOD, this.masternetODToFullnetOD.get(masterOD));
            boolean masterODConsistent = masterOD.checkFlowConsistency();
            boolean fullODConsistent = this.masternetODToFullnetOD.get(masterOD).checkFlowConsistency();
            if(!(masterODConsistent) || !fullODConsistent){
                System.out.println("Flows on ODs are not consistent. masterODConsistent="+masterODConsistent+" and fullODConsistent="+fullODConsistent);
                System.exit(1);
            }
        }
    }
    
    public void mapMasterODFlowsToFullOD(ODPair masterOD, ODPair fullOD){
        fullOD.getStem().getPathSet().clear();
        fullOD.getStem().setPathSet(new HashSet<>());
        double demand = 0;
        
        for (Path p : masterOD.getStem().getPathSet()){
//            System.out.println("!!Master Path="+p+" with flow="+p.getFlow());
            //A path in master net consists of artificial links and physical link
            //Each artificial link is a set of paths on its own
            //so the following variable stores list of path connecting every node and a list of those for every nodes on a path
            List<List<Path>> setOfPathsForPathP = new ArrayList<>(); 
            for (Link dstapLink : p.getPathLinks()){
                
                if (dstapLink instanceof ArtificialLink){
                    //this is an artificial link
                    ArtificialLink aLink = (ArtificialLink)dstapLink;
                    List<Path> subPathsReplacingThisALink = new ArrayList<>();
                    ODPair subnetOD = aLink.getAssociatedODPair();
                    for (Path subnetPath : subnetOD.getStem().getPathSet()){
                        //We will not skip any path for DSTAP as heuristic
                        //For DSTAP as algorithm this subnetOD may have paths with subnet artificial links
                        //skip that path in that case
                        boolean skipThisPath=false; 
                        for(Link l: subnetPath.getPathLinks()){
                            if(!otherNetLinkToFullLink.containsKey(l)){
                                skipThisPath=true;
                                break;
                            }
                        }
                        if(skipThisPath)
                            continue;
                        subPathsReplacingThisALink.add(copyPath(subnetPath));
                    }
                    setOfPathsForPathP.add(subPathsReplacingThisALink);
                }
                else{
                    //boundary links @todo: these could be simply the physical links as well
                    Link bLink = otherNetLinkToFullLink.get(dstapLink);
                    Path tempPath = new Path(0);
                    tempPath.addLinkToFront(bLink);
                    tempPath.setFlow(p.getFlow()); //since only 1 link we can set the flow of path p directly
                    
                    List<Path> tempSubPaths = new ArrayList<>();
                    tempSubPaths.add(tempPath);
                    
                    setOfPathsForPathP.add(tempSubPaths);
                }
            }

            /**
             * The goal of code below is to generate combinations of paths. Say 4 paths replace first link, 2 paths replace second link, 1 path replaces third link, and 4 paths replace fourth link,
             * then the code samples all the 4*2*1*4=32 path one by one and adds it to the fullPath
             * For an artificial link, the flow on replaced paths is assigned in proportion to the flows on the paths in the actual subnetwork which they replace
            */
            List<Double> flowsOnEachSetOfPaths = new ArrayList<>();
            List<Integer> sizeOfSubList = new ArrayList<>();
            List<Integer> setByProduct = new ArrayList<>();
            for(int i=0; i< setOfPathsForPathP.size();i++){
                flowsOnEachSetOfPaths.add(i,0.0);
                sizeOfSubList.add(i,setOfPathsForPathP.get(i).size());
                setByProduct.add(i,0);
                for(Path p_loop: setOfPathsForPathP.get(i))
                    flowsOnEachSetOfPaths.set(i, flowsOnEachSetOfPaths.get(i)+p_loop.getFlow());
            }
            
            int numberOfCombinations = 1;
            
            for(int i=sizeOfSubList.size()-1;i>=0;i--){
                if(i==sizeOfSubList.size()-1)
                    setByProduct.set(i, 1);
//                else if(i==sizeOfSubList.size()-2)
//                    setByProduct.set(i, sizeOfSubList.get(i+1));
                else{
                    setByProduct.set(i, sizeOfSubList.get(i+1)*setByProduct.get(i+1));
                }
                numberOfCombinations*= sizeOfSubList.get(i);
            }
            
            for(int index=0; index< numberOfCombinations; index++){
                List<Integer> idsToPick = new ArrayList<>();
                for(int i=0;i< setByProduct.size();i++){
                    idsToPick.add(i,(index/setByProduct.get(i))%sizeOfSubList.get(i));
                }
                List<Path> pathsToCombine = new ArrayList<>();
                double fullPathFlow = p.getFlow();
                
                for(int i=0;i< setOfPathsForPathP.size();i++){
                    Path temp = setOfPathsForPathP.get(i).get(idsToPick.get(i));
                    pathsToCombine.add(temp);
                    if(flowsOnEachSetOfPaths.get(i)==0)
                        fullPathFlow =0;
                    else
                        fullPathFlow = fullPathFlow* (temp.getFlow()/flowsOnEachSetOfPaths.get(i));
                }
                Path fullPath = combinePathList(pathsToCombine);
//                System.out.println("!!!!Equivalent fullnet paths="+fullPath+" with flow="+fullPathFlow);
                //if(fullPathFlow)
                
                if(fullOD.getStem().getPathSet().contains(fullPath)){
                    Path samePathAlreadySaved = null;
                    for(Path temporaryP: fullOD.getStem().getPathSet()){
                        if(temporaryP.equals(fullPath)){
                            samePathAlreadySaved= temporaryP;
                            break;
                        } 
                    }
                    double tempPathFlow= samePathAlreadySaved.getFlow();
                    fullPath.setFlow(fullPathFlow+tempPathFlow);
                    fullPath.assignPathFlowToLinks(fullPathFlow);
                    fullOD.getStem().getPathSet().remove(samePathAlreadySaved);
                }
                else{
                    fullPath.setFlow(fullPathFlow);
                    fullPath.assignPathFlowToLinks();
                }
                fullOD.getStem().getPathSet().add(fullPath);
                demand+= fullPathFlow;
                //demand+= fullPath.getFlow();
//                System.out.println("Comparing path flow to total demand sum "+p.getFlow()+"\t"+demand);
            }
            
            double totalFlowBetODInFullNet=0.0;
            for(Path a: fullOD.getStem().getPathSet())
                totalFlowBetODInFullNet+= a.getFlow();
            if (Math.abs(totalFlowBetODInFullNet - demand) > 1E-10){
                System.out.println("Full net: inconsistency in path flows master network "+"\t"+totalFlowBetODInFullNet+"\t"+demand);
                
                totalFlowBetODInFullNet=0.0;
                for(Path a: fullOD.getStem().getPathSet())
                    totalFlowBetODInFullNet+= a.getFlow();
                
                System.out.println("Links in the path with this issue:");
                for(Link l:p.getPathLinks()){
                    System.out.print(l.getSource().getId()+"-->"+l.getDest().getId()+"\t");
                }
                System.exit(1);
            }
        }
    }
    
    public void mapSubnetODFlowsToFullOD(ODPair subOD, ODPair fullOD){
        fullOD.getStem().getPathSet().clear();
        fullOD.getStem().setPathSet(new HashSet<>());
        double ratioOfSubODDemandToFullODDemand = fullOD.getDemand()/subOD.getDemand(); //recall subOD.getDemand gives artificial link flow as well
        for (Path p : subOD.getStem().getPathSet()){
            Path fullPath = copyPath(p); //all links on p must be physical links as part of the full network
//            System.out.println("Full path "+fullPath+ "flow="+ fullPath.getFlow());
            fullPath.setFlow(fullPath.getFlow()*ratioOfSubODDemandToFullODDemand);
            fullPath.assignPathFlowToLinks();
            fullOD.getStem().getPathSet().add(fullPath);
        }
    }
    
    /* create a path on full net for dstapPath
     */
    public Path copyPath(Path dstapPath){
        Path fullPath = null;
        fullPath = new Path(0);//start with zero cost

        for (int index= dstapPath.getPathLinks().size()-1; index>=0; index--){
            Link l= dstapPath.getPathLinks().get(index);
            fullPath.addLinkToFront(this.otherNetLinkToFullLink.get(l));
        }
        //assigns the flow the path links
        fullPath.setFlow(dstapPath.getFlow());
        return fullPath;
    }
    
    public Path combinePathList(List<Path> pathList)
    {
        Path fullPath = new Path(0);//start with zero cost
        for(int i=0;i<pathList.size();i++){
            for(Link l: pathList.get(i).getPathLinks()){
                if(l==null){
                    System.out.println("Null link in the path"+pathList.get(i));
                    continue;
//                    System.exit(1);
                }
                fullPath.addLinkToBack(l);
            }
        }
        return fullPath;
    }
    
    public void printFullNetworkStatistics(){
        this.printNetworkStatistics();
        System.out.println("Mapping of otherNets link to full net links\n");
        for(Link l:this.otherNetLinkToFullLink.keySet()){
            System.out.println(l+"-->"+this.otherNetLinkToFullLink.get(l));
        }
        System.out.println("Mapping of subnetODs to fullnet ODs");
        for(ODPair sOD: subnetODToFullnetOD.keySet()){
            System.out.println(sOD+"-->"+subnetODToFullnetOD.get(sOD));
        }
        System.out.println("Mapping of masternetODs to fullnet ODs");
        for(ODPair mOD: masternetODToFullnetOD.keySet()){
            System.out.println(mOD+"-->"+masternetODToFullnetOD.get(mOD));
        }
    }
    
    public double getFullNetGap(){
        this.setSPTT(0.0);
        resetLinkPrevItrFlows();
        for(Node origin : this.tripTable.getOrigins())
            dijkstras(origin); //updates SPTT
        return this.getGap();
    }
}
