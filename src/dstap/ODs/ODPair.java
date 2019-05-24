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
    protected double demand; //this is the true demand in the network
    protected String belongsToNet;
    private Stem stem;

    public ODPair(Node origin, Node dest) {
        this(origin, dest, 0.0);
    }

    public ODPair(Node origin, Node dest, double demand) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
        this.stem = new Stem(this);
    }

    public ODPair(Node origin, Node dest, double demand, String belongsToNet) {
        this.origin = origin;
        this.dest = dest;
        this.demand = demand;
        this.belongsToNet = belongsToNet;
        this.stem = new Stem(this);
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
    
    public double getODCostAtUE(){
        if(this.getDemand()>0){
            double UEcost=0.0;
            if(this.getStem().getPathSet().isEmpty()){
                System.out.println("\nOD "+this+ "has demand="+ this.getDemand()+" yet there is no path when the function getODCostAtUE is called. Exiting!");
                System.exit(1);
            }
            for(Path p: this.getStem().getPathSet()){
                UEcost += (p.getCost() * p.getFlow()/getDemand());
            }
            //@todo: put exception for negative or infinite costs
            return UEcost;
        }
        else
            return 0.0; //if there is no path or OD demand is zero, we do not bother finding the cost!
    }

    @Override
    public String toString() {
        return "{" + origin + "-->" + dest + '}';
    }
    
    /**
     * return false if sum of path flows doesn't match od flow
     * Flow consistency isn't always satisfied (especially after having updated subnet demand etc.)
     * But at end of every iteration each network OD should have flow consistency
     * @return
     */
    public boolean checkFlowConsistency(){
        double pathFlow = 0;
        for (Path p : this.getStem().getPathSet()){
            pathFlow += p.getFlow();
        }

        if ( Math.abs( pathFlow - getDemand()) > 1E-3 ){
            System.out.println("Error in checking flow consistency: ODpair "+this+" has od demand of "+demand+" but the sum of path flows is "+pathFlow);
//            System.exit(10);
            return false;
        } else{
            return true;
        }
    }
}
