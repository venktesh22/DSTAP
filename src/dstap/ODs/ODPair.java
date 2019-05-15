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
    protected Node origin, dest;
    protected double demand;
    protected String belongsToNet;
    private Stem stem;

    public ODPair(Node origin, Node dest) {
        this.origin = origin;
        this.dest = dest;
        demand = 0;
    }

    public ODPair(Node origin, Node dest, double demand) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
    }

    public ODPair(Node origin, Node dest, double demand, String belongsToNet) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
        this.belongsToNet = belongsToNet;
        
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
    
    public void addToDemand(double d){
        demand += d;
    }

    public Stem getStem() {
        return stem;
    }

    public void setStem(Stem stem) {
        this.stem = stem;
    }

    @Override
    public String toString() {
        return "{" + origin + "-->" + dest + '}';
    }
    
    
}
