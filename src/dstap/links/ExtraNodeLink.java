/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.links;

import dstap.nodes.Node;

/**
 * These links are created in subnetworks
 * They represent a copy of masterNet link which is a part of this subnetwork
 * used to load demands from others subnetwork's boundary node's artificial link
 * onto this subnetwork (previously we used to call these zero-cost link, but
 * they have the same cost as physical links they relate to)
 * @author vp6258
 */
public class ExtraNodeLink extends Link{

    private Link associatedPhysicalLink;
    
    public ExtraNodeLink(Node source, Node dest) {
        super(source, dest);
    }

    public ExtraNodeLink(Node source, Node dest, double fftime, double coef, double power, 
            double capacity, String t) {
        super(source, dest, fftime, coef, power, capacity, t);
    }
    
    

    public void setAssociatedPhysicalLink(Link associatedPhysicalLink) {
        this.associatedPhysicalLink = associatedPhysicalLink;
    }

    public Link getAssociatedPhysicalLink() {
        return associatedPhysicalLink;
    }
    
}
