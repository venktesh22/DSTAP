/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.ODPair;
import dstap.links.Link;
import dstap.nodes.Node;
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
    
    public FullNetwork(MasterNetwork master, List<SubNetwork> subNets, String name, String verbosity){
        super(verbosity);
        subNetworks = new HashSet<SubNetwork>();
        
        boundaryNodes = new HashSet<Node>();
        boundaryLinks = new HashSet<Link>();
        
        for(SubNetwork s: subNets)
            subNetworks.add(s);
//        subNetworks.add(north);
        masterNet = master;
        networkName = name;
        otherNetLinkToFullLink = new HashMap<Link, Link>();
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
}
