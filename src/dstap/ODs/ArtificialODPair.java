/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.ODs;

import dstap.links.ArtificialLink;
import dstap.nodes.Node;

/**
 * Represents an OD pair associated with a network which is not true part of the network
 * but created to represent flow from other networks
 * @author vp6258
 */
public class ArtificialODPair extends ODPair{
    private ArtificialLink associatedALink;
    private Stem bush; //only an artifical OD pair has a bush associated w it

    public ArtificialODPair(Node origin, Node dest) {
        super(origin, dest);
    }
    
    public ArtificialODPair(Node origin, Node dest, double d) {
        super(origin, dest, d);
    }

    public ArtificialLink getAssociatedALink() {
        return associatedALink;
    }

    public void setAssociatedALink(ArtificialLink associatedALink) {
        this.associatedALink = associatedALink;
    }

    public Stem getBush() {
        return bush;
    }

    public void setBush(Stem bush) {
        this.bush = bush;
    }
    
}
