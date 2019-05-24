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
    private double demandDueToALink;

    public ArtificialODPair(Node origin, Node dest) {
        super(origin, dest);
        this.demandDueToALink = 0.0;
    }
    
    public ArtificialODPair(Node origin, Node dest, double d) {
        super(origin, dest, d);
        this.demandDueToALink = 0.0; //when a OD pair is first demand demandDueToALink is zero
    }

    public ArtificialLink getAssociatedALink() {
        return associatedALink;
    }

    public void setAssociatedALink(ArtificialLink associatedALink) {
        this.associatedALink = associatedALink;
    }
    
    @Override
    public double getDemand() {
        return this.demand + this.demandDueToALink;
    }
    
    public double getOriginalDemand(){
        return this.demand;
    }
    
    public double getDemandDueToALink(){
        return this.demandDueToALink;
    }

    //updates the demandDueToALink to the newDemand
    //If total demand = 0, then it resets the paths for the OD's stem (clears those)
    //If there are current paths for the ODPair's stem and totalDemand>0, then it adjust path flows
    //If no current paths and there is new demand, then do nothing but update the demand for next iteration
    public void updateDemandDueToALink(double newDemand) {
        double changeInDemand = newDemand - this.demandDueToALink;

        if (getDemand()+newDemand==0){
            this.getStem().getPathSet().clear();
        }
        else if (this.getStem().getPathSet().size() > 0){
            this.getStem().assignExtraDemandToPathFlows(changeInDemand);
        }
        
//        System.out.print("Updating "+this+" od pair's demandDueToALink from "+this.demandDueToALink+" to "+newDemand);
        this.demandDueToALink = newDemand;
//        System.out.print(" where updated total demand="+getDemand()+"\n");
        this.getStem().updateCost();
    }
    
    
}
