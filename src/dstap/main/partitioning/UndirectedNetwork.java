/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import dstap.links.Link;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Undirected version of the partitioning network
 * @author vp6258
 */
public class UndirectedNetwork {
    protected Set<UndirectedLink> links;
    protected Set<UndirectedNode> nodes;
    protected Map<Set<UndirectedNode>, UndirectedLink> linksByNodeSet;
    protected Map<Integer, UndirectedNode> nodesById;
    
    public Map<Integer, Integer> nodeRank;
    
    public UndirectedNetwork(){
        links = new HashSet<>();
        nodes = new HashSet<>();
        linksByNodeSet = new HashMap<>();
        nodesById = new HashMap<>();
        
        nodeRank = new HashMap<>();
    }
    
    public void addLink(Link l){
        UndirectedNode n1 = null;
        if(this.nodesById.containsKey(l.getSource().getId()))
            n1= nodesById.get(l.getSource().getId());
        else{
            n1=new UndirectedNode(l.getSource().getId());
            nodesById.put(l.getSource().getId(), n1);
            nodes.add(n1);
        }
        UndirectedNode n2 = null;
        if(this.nodesById.containsKey(l.getDest().getId()))
            n2= nodesById.get(l.getDest().getId());
        else{
            n2=new UndirectedNode(l.getDest().getId());
            nodesById.put(l.getDest().getId(), n2);
            nodes.add(n2);
        }
        
        Set<UndirectedNode> nodeSet = new HashSet<>();
        nodeSet.add(n1); nodeSet.add(n2);
        
        //only add link if not already added
        if(!linksByNodeSet.containsKey(nodeSet)){
            UndirectedLink l1 = new UndirectedLink(nodeSet);
            linksByNodeSet.put(nodeSet, l1);
            links.add(l1);
        }
    }
    
    public void checkNetwork(){
        for(UndirectedNode n: this.nodes){
            if(n.getConnectedNodes().size()!= n.getLinks().size()){
                System.out.println("We found a discrepancy in undirected graph. Fix");
                System.exit(1);
            }
        }
    }
    
    public int getLowestDegreeNodeId(){
        int lowestRank=10000;
        int nodeIdWithLowestRank = -1;
        for(UndirectedNode n: this.nodes){
            int rank= n.getLinks().size();
            this.nodeRank.put(n.getId(),rank );
            if(rank<lowestRank){// && n.getId()!=5){
                lowestRank=rank;
                nodeIdWithLowestRank=n.getId();
            }
        }
        return nodeIdWithLowestRank;
    }

    public Set<UndirectedLink> getLinks() {
        return links;
    }

    public Set<UndirectedNode> getNodes() {
        return nodes;
    }

    public Map<Set<UndirectedNode>, UndirectedLink> getLinksByNodeSet() {
        return linksByNodeSet;
    }

    public Map<Integer, UndirectedNode> getNodesById() {
        return nodesById;
    }
}