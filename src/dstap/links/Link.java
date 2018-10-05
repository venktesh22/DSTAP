/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.links;

import dstap.nodes.Node;

/**
 *
 * @author Venktesh
 */
public class Link {
    protected final Node source, dest;
    protected double capacity, fftime, power, coef;

    protected String type;// subNet, masterNet, fullNet

    double flow; // flow
     //dx_ij/dX (derivative of link flow wrt change in demand between an OD pair
    protected double dxdX;
    
    public double x_star; // new flow
    protected double centralizedFlow; //flow on the complete network

    public Link(Node source, Node dest){
        this.source = source;
        this.dest = dest;
        flow = 0;
        source.addLink(this);
        dest.addLink(this);
    }

    // link performance function = fftime * (1+coef(x/capacity)^power)
    public Link(Node source, Node dest, double fftime, double coef, double power, double capacity, String t){
        this.source = source;
        this.dest = dest;
        type = t;

        flow = 0;
        x_star = 0;

        source.addLink(this);
        dest.addLink(this);

        this.capacity = capacity;
        this.fftime = fftime;
        this.coef = coef;
        this.power = power;//power
    }

    public void updateFftime(double fft){
        fftime = fft;
    }

    public void updateCoef(double cof){
        this.coef = cof;
    }

    public void updateFlow(double x){
        if(Double.isNaN(x)){
            System.out.println("Flow update value for link "+this+ "is NaN. Exiting");
            System.exit(1);
        }
        flow += x;
        if (flow < 1E-10){
            flow = 0;
        }
    }

    public void setdxdX(double a){
        dxdX = a;
    }

//    public void setUrbanODPair(ODPair od){
//        urbanODPair = od;
//    }

//     Get methods
    public double getCapacity(){
        return capacity; //vph
    }

    public double getFFTime(){
        return fftime; //seconds
    }

    public double getCoef(){
        return coef;
    }

    public double getPower(){
        return power;
    }

    public Node getSource(){
        return source;
    }

    public Node getDest(){
        return dest;
    }

    public String getType(){
        return type;
    }

    public double getTravelTime(){
        return getTravelTime(getFlow());
    }

    public double getTravelTime(double x){
        return fftime * (1 + coef * Math.pow(x / capacity, power) );
    }

    public double getFlow(){
        return flow;
    }

    public double getdxdX(){   
        // required for setting up the aggregatded network
        return dxdX;
    }

    public double getBushCost()   
    {
        //dt/dX=dt/dx * dx/dX
        return dxdX * calcDer();
    }

//    public ODPair getUrbanODPair(){
//        return urbanODPair;
//    }

    public double getIntegration(){
        return fftime * (flow+ ( Math.pow(fftime, power+1) * coef / ((1+power)*Math.pow(capacity, power)) ) );
    }


//  Computational methods
    public double calcDer(){
        return calcDer(getFlow());
    }

    public double calcDer(double x){
        //dx/dt
        return (double) fftime * coef * power * Math.pow(x / capacity, power - 1) / capacity;
    }

    @Override
    public String toString(){
        return "["+source.getId()+", "+dest.getId()+" , "+type+"]";//+", "+fftime+", "+alpha+", "+capacity+", "+beta+"]";
    }
    
    @Override
    public boolean equals(Object o){
        Link rhs= (Link)o;
        boolean theSame= false;
        if(this.source.getId()== rhs.source.getId() && this.dest.getId()==rhs.dest.getId() && this.type== rhs.type)
            theSame=true;

        return theSame;
    }
    
    @Override
    public int hashCode(){
        int tempSource= this.source.getId()%1000000;
        int tempDest= this.dest.getId()%2000000;
        return (this.source.getId()*(10000)+this.dest.getId())*100 + this.type.length();
    }
}
