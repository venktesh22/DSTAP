/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.ODs;

import dstap.links.Link;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author vp6258
 */
public class Path {
    private List<Link> pathLinks;
    private double cost;
    private double flow;
    private double sensitivityCost;
    private double sensitivityFlow; //no physical meaning, but theoretically it is the amount of derivative that flows on this path

    public Path(){
        this(0.0);
    }
    
    public Path(double cost) {
        this.cost = cost;
        pathLinks = new ArrayList<>();
        flow=0.0;
        sensitivityCost = 0.0;
        sensitivityFlow = 0.0;
    }
    
    public void addLinkToFront(Link l){
        pathLinks.add(0,l);
    }
    
    public void updatePathCost(){
        double newCost = 0;
//        System.out.println("--Path "+this+" ");
        for (Link l : this.pathLinks){
            newCost += l.getTravelTime();
//            System.out.println("----Link "+l+" with flow="+l.getFlow()+" and TT="+l.getTravelTime());
        }
        setCost(newCost);
//        System.out.println("--Path "+this+" cost set to "+newCost);
    }
    
    public void updatePathSensitivityCost(){
        double newCost = 0;
        for (Link l : this.pathLinks){
            newCost += l.getdxdX();
        }
        setCost(newCost);
    }

    public double getCost() {
        updatePathCost();
        return cost;
    }

    private void setCost(double cost) {
        this.cost = cost;
    }

    public double getFlow() {
        return flow;
    }

    public void setFlow(double flow) {
        this.flow = flow;
    }

    public double getSensitivityCost() {
        updatePathSensitivityCost();
        return sensitivityCost;
    }

    public void setSensitivityCost(double sensitivityCost) {
        this.sensitivityCost = sensitivityCost;
    }

    public double getSensitivityFlow() {
        return sensitivityFlow;
    }

    public void setSensitivityFlow(double sensitivityFlow) {
        this.sensitivityFlow = sensitivityFlow;
    }

    public List<Link> getPathLinks() {
        return pathLinks;
    }
    
    public void addToPathFlow(double change){
        flow += change;
        for(Link l : this.pathLinks){
            l.addToFlow(change);
        }
        if (flow < -1E-10){
            System.out.println("Path "+ this+" has negative flow of\t"+flow);
            System.exit(1);
        }
    }

    @Override
    public String toString() {
        String s="[";
        for(Link l:pathLinks)
            s += (Integer.toString(l.getSource().getId())+",");
        s+= Integer.toString(pathLinks.get(pathLinks.size()-1).getDest().getId());
        s+="]";
        return s;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.pathLinks);
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
        final Path other = (Path) obj;
        if (!Objects.equals(this.pathLinks, other.pathLinks)) {
            return false;
        }
        return true;
    }
    
    

}
