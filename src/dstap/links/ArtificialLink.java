/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.links;
import dstap.ODs.*;
import dstap.nodes.Node;
/**
 *
 * @author vp6258
 */
public class ArtificialLink extends Link {

    private ArtificialODPair associatedODPair;
    
    public ArtificialLink(Node source, Node dest) {
        super(source, dest);
    }
    
    public ArtificialLink(Node source, Node dest, double fftime, double coef, 
            double power, double capacity, String t){
        super(source, dest, fftime, coef, power, capacity, t);
    }

    public ArtificialODPair getAssociatedODPair() {
        return associatedODPair;
    }

    public void setAssociatedODPair(ArtificialODPair associatedODPair) {
        this.associatedODPair = associatedODPair;
    }
    
    //we can have negative free flow travel time for artificial links
    //the actual travel time should never be negative though!
    public void setFftime(double fftt){
//        if(fftt<0.0){
//            System.out.println("FFTT negative"+fftt);
//            System.exit(1);
//        }
        this.fftime = fftt;
    }

    public void setCoef(double coef) {
        this.coef = coef;
    }
    
}
