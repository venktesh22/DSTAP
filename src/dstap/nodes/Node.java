/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.nodes;

import dstap.links.Link;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public class Node {
    private Set<Link> incoming;
    private Set<Link> outgoing;

    private final int id;
    private final String type;//whether this is a node in master net (masterNet) or subnet (subNet) and/or fullNet

    public double label; // used for shortest path
    public Link prev;

    public Node(int id, String t){
        incoming = new HashSet<>();
        outgoing = new HashSet<>();
        type = t;
        this.id = id;
    }

    @Override
    public boolean equals(Object o){
        Node rhs = (Node)o;
        return rhs.id == id;
    }

    public void addLink(Link l){
        if(l.getSource() == this){
            outgoing.add(l);
        }
        else if(l.getDest() == this){
            incoming.add(l);
        }
    }

    public Set<Link> getIncoming(){
        return incoming;
    }

    public Set<Link> getOutgoing(){
        return outgoing;
    }

    // return true if the source node is upstream of the node object
    public Link getIncomingLink(Node source){
        for(Link l : incoming){
            if(l.getSource() == source){
                return l;
            }
        }
        return null;
    }

    public Link getOutgoingLink(Node dest){
        for(Link l : outgoing){
            if(l.getDest() == dest){
                return l;
            }
        }
        return null;
    }

    public int getId(){
        return id;
    }

    public String getType(){
        return type;
    }

    @Override
    public int hashCode(){
        return id;
    }

    @Override
    public String toString(){
        return "("+id+","+type+")";
    }
}
