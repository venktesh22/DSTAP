/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public class UndirectedNode{
    private int id;
    private Set<UndirectedLink> links;
    private Set<UndirectedNode> connectedNodes;
    public int label;

    public UndirectedNode(int id) {
        this.id=id;
        links = new HashSet<>();
        connectedNodes = new HashSet<>();
    }
    
    public int getId(){
        return this.id;
    }
    
    public void addLink(UndirectedLink l){
        links.add(l); // we are assured that duplicate values won't be passed
        
    }
    
    public void addNode(UndirectedNode n){
        connectedNodes.add(n);
    }
    
    public Set<UndirectedLink> getLinks(){
        return this.links;
    }

    public Set<UndirectedNode> getConnectedNodes() {
        return connectedNodes;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UndirectedNode other = (UndirectedNode) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UNode{"+ id + '}';
    }
}
