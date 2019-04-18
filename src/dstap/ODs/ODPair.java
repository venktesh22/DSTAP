/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.ODs;

import dstap.nodes.Node;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public class ODPair {
    private Node origin, dest;
    private double demand;
    private String belongsToNet;
    
    public Set<Path> pathSet;
    private Path shortestPath;
    private Path longestPath;
    
    private Bush bush;

    public ODPair(Node origin, Node dest) {
        this.origin = origin;
        this.dest = dest;
        demand = 0;
        pathSet = new HashSet<>();
    }

    public ODPair(Node origin, Node dest, double demand) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
        pathSet = new HashSet<Path>();
    }

    public ODPair(Node origin, Node dest, double demand, String belongsToNet) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
        this.belongsToNet = belongsToNet;
        
        pathSet = new HashSet<Path>();
    }

    public Node getOrigin() {
        return origin;
    }

    public Node getDest() {
        return dest;
    }

    public double getDemand() {
        return demand;
    }

    public Bush getBush() {
        return bush;
    }
    
    public void addToDemand(double d){
        demand += d;
    }

    @Override
    public String toString() {
        return "{" + origin + "-->" + dest + '}';
    }
    
    
}
