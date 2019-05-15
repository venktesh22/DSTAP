/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.ODs;
import dstap.links.Link;
import dstap.nodes.Node;
import java.util.HashSet;
import java.util.Set;

/**
 * A stem is an acyclic subgraph connecting all used paths from an origin to a destination
 * We track bushes between each OD pair (also referred as stems in Boyles (2012) paper)
 * 
 * Same class methods can be used to solve for equilibrium flows (for solving regular UE)
 * and to solve for the dxdX (rate of change of flow on link wrt changes in demand) for sensitivity analysis
 * 
 * boolean argument sensitivityCase for most of the functions determines which case it is 
 * @author vp6258
 */
public class Stem {
    private ODPair od; //associated OD pair
    private Set<Path> pathSet; //all paths for this stem connecting the origin to the destination
    
    private double timeDerivative; //this is the derivative of OD travel time wrt changes in OD pair's demand
    private double timeOriginToDest;
    
    private Path shortestPath;
    private Path longestPath;
        
    public Stem(ODPair od){
        this.od = od;
        pathSet = new HashSet<>();
    }
    
    public void updateCost(){
        updateCost(false);
    }
    
    // methods for adding a path to pathSet, setting shortest path, getting shortest path
    public void updateCost(boolean sensitivityCase){
        for (Path p : pathSet){
            if(!sensitivityCase)
                p.updatePathCost();
            else
                p.updatePathSensitivityCost();
        }
    }

    public Set<Path> getPathSet() {
        return pathSet;
    }
    
    public void addPath(Path np){
        boolean exists = false;
        for (Path p : pathSet){
            if (p.equals(np)){
                exists = true;
                break;
            }
        }
        if (!exists){
            pathSet.add(np);
        }
    }
    
    public void setShortestPath(){
        double cost = Double.MAX_VALUE;
        Path sPath = null;

        for (Path d : pathSet){
            if (d.getCost() < cost){
                cost = d.getCost();
                sPath = d;
            }
        }
        setShortestPath(sPath);
    }

    public void setShortestPath(Path p){
        if (p != null)
            shortestPath = p;
        //shortest path may be null if pathSet is empty
//        else{
//            System.out.println("Shortest path is NULL. Exiting");
//            System.exit(1);
//        }
    }


    public void setLongestPath(){
        double cost = -100;//setting cost of negative instead of 0 as some paths may have zero cost regardless of flow
        Path lPath = null;

        for (Path d : pathSet){
            if (d.getCost() > cost){
                cost = d.getCost();
                lPath = d;
            }
        }
        setLongestPath(lPath);
    }

    public void setLongestPath(Path p){
        if (p != null)
            longestPath = p;
    }

    public Path getShortestPath() {
        setShortestPath();
        return shortestPath;
    }

    public Path getLongestPath() {
        setLongestPath();
        return longestPath;
    }
    
    public void updateAllPathsCost(){
        for (Path p : this.pathSet){
            p.updatePathCost();
        }
    }
    
    public double getCostDiffBetLongestAndShortestPaths(){
        Path sPath = getShortestPath();
        Path lPath = getLongestPath();
        if(lPath==null && sPath==null)
            return 0.0;
        double lCost = lPath.getCost();
        double sCost = sPath.getCost();
        if(lCost<sCost){
            System.out.println("Longest path cost("+lCost+") is lower than shortest path cost("+sCost+"). Exiting!");
            System.exit(1);
        }
        return (lCost-sCost);
    }
    
    public void dropPathsWFlowltThresh(double threshold){
        //balance the removed flow on all other paths
        Set<Path> pathSetCopy = new HashSet<>();
        double residual = 0;
        for (Path d : pathSet){
            if (d.getFlow() <= threshold){
                pathSetCopy.add(d);
                residual += d.getFlow();
            }
        }
        for (Path p : pathSetCopy){
            p.addToPathFlow(-p.getFlow());
            int sizeB = pathSet.size();
            removePath(p);
            int sizeA = pathSet.size();
            if (sizeA == sizeB)
                System.out.println("ODpair - path with small flow was not removed"+p);
            if (pathSet.contains(p))
                System.out.println("ODpair: path with small was nor deleted\t"+this+"\t"+p.getFlow());
        }

        getShortestPath().addToPathFlow(residual);

        for (Path p : pathSet){
            if (p.getFlow() < threshold){
                System.out.println("OD - path was not removed"+p.getFlow());
                System.exit(1);
            }
        }
    }
    
    private void removePath(Path p){
        this.pathSet.remove(p);
    }
    
    //functions for shifting the flow(regularFlow or sensitivityFlow) from all long paths to shortest path
    //at a certain rate (we don't want all flow to go from one path to shortest, instead shift reasonable amount)
    public void shiftLimitedFlowFromAllPathstoSP(double rate, boolean sensitivityCase){
        for (Path lPath : pathSet){
            Path sPath = getShortestPath();
            if (lPath != sPath){
                double shiftValue = 0;
                if(lPath.getCost() - sPath.getCost() !=0)
                    shiftValue = Math.min ( (lPath.getCost() - sPath.getCost()) / calDenom(sPath, lPath), lPath.getFlow() );
                if(!sensitivityCase)
                    shiftValue = Math.max(shiftValue, 0.0); //only positive flow can be shifted

                if(Double.isNaN(shiftValue) || Double.isInfinite(shiftValue)){
                    System.out.println("The flow requested to be shifted between the paths sPath:"+sPath+" and lPath:"+lPath+" has value of NaN or Infinity");
                    System.out.println("In particular, numerator lPath.getCost() - sPath.getCost()="+(lPath.getCost() - sPath.getCost()));
                    System.out.println("and denominator calDenom(sPath, lPath)="+calDenom(sPath, lPath));
                    System.exit(1);
                }
                shiftValue *= rate;
		shiftValue  = Math.min(shiftValue, lPath.getFlow());

                sPath.addToPathFlow(shiftValue);
                lPath.addToPathFlow(-shiftValue);
            }
        }
    }
    
    public double calDenom(Path sP, Path lP){
        double denomValue = 0;
        for (Link l : sP.getPathLinks())
            if (!lP.getPathLinks().contains(l))
                denomValue += l.calcDer();

        for (Link l : lP.getPathLinks())
            if (!sP.getPathLinks().contains(l))
                denomValue += l.calcDer();
        
        if(Double.isNaN(denomValue)){
            System.out.println("Calculated denominator value is NaN for two paths: sPath:"+sP+" lPath:"+lP);
            System.exit(1);
        }

        return denomValue;
    }
}
