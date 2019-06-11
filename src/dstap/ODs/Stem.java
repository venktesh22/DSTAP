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
        timeDerivative = 0;
        timeOriginToDest = 0;
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
        setShortestPath(false);
    }
    
    public void setShortestPath(boolean sensitivityCase){
        double cost = Double.MAX_VALUE;
        Path sPath = null;

        for (Path d : pathSet){
            double pathCost = sensitivityCase?d.getSensitivityCost():d.getCost();
            if (pathCost < cost){
                cost = pathCost;
                sPath = d;
            }
        }
        if(sPath==null){
            System.out.println("Shortest path is null because pathSet.size()="+pathSet.size());
        }
        setShortestPath(sPath);
    }

    public void setShortestPath(Path p){
        shortestPath = p;
        //shortest path may be null if pathSet is empty
//        else{
//            System.out.println("Shortest path is NULL. Exiting");
//            System.exit(1);
//        }
    }

    //overloaded function to first find longest path (among the current paths) and then set longest path
    public void setLongestPath(){
        setLongestPath(false);
    }

    public void setLongestPath(boolean sensitivityCase){
        double cost = -100;//setting cost of negative instead of 0 as some paths may have zero cost regardless of flow
        Path lPath = null;

        for (Path d : pathSet){
            double pathCost = sensitivityCase?d.getSensitivityCost():d.getCost();
            if (pathCost > cost){
                cost = pathCost;
                lPath = d;
            }
        }
        setLongestPath(lPath);
    }

    public void setLongestPath(Path p){
        longestPath = p;
    }
    
    public double getShortestPathCost(){
        setShortestPath();
        return getShortestPath().getCost();
    }
    
    public Path getShortestPath(){
        return getShortestPath(false);
    }

    public Path getShortestPath(boolean sensitivityCase) {
        setShortestPath(sensitivityCase);
        return shortestPath;
    }
    
    public Path getLongestPath(){
        return getLongestPath(false);
    }

    public Path getLongestPath(boolean sensitivityCase) {
        setLongestPath(sensitivityCase);
        return longestPath;
    }
    
//    public void updateAllPathsCost(){
//        for (Path p : this.pathSet){
//            p.updatePathCost();
//        }
//    }
    public double getCostDiffBetLongestAndShortestPaths(){
        return getCostDiffBetLongestAndShortestPaths(false);
    }
    
    public double getCostDiffBetLongestAndShortestPaths(boolean sensitivityCase){
        Path sPath = getShortestPath(sensitivityCase);
        Path lPath = getLongestPath(sensitivityCase);
        if(lPath==null || sPath==null)
            return 0.0;
        double lCost = sensitivityCase?lPath.getSensitivityCost():lPath.getCost();
        double sCost = sensitivityCase?sPath.getSensitivityCost():sPath.getCost();
        if(lCost<sCost){
            System.out.println("Longest path cost("+lCost+") is lower than shortest path cost("+sCost+"). Exiting!");
            System.exit(1);
        }
        return (lCost-sCost);
    }
    
    public void dropPathsWFlowltThresh(double threshold){
        if(threshold>1.0){
            System.out.println("not encouraged to drop paths with flow higher than 1.0");
            System.exit(1);
        }
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
            
//            System.out.println("~~~removing path "+p+" and it has flow="+p.getFlow());
            p.addToPathFlow(-p.getFlow());
            int sizeB = pathSet.size();
            removePath(p);
            
            int sizeA = pathSet.size();
            if (sizeA == sizeB)
                System.out.println("ODpair - path with small flow was not removed"+p);
            if (pathSet.contains(p))
                System.out.println("ODpair: path with small was nor deleted\t"+this+"\t"+p.getFlow());
        }

        Path sp = getShortestPath();
        if(sp!=null){
            getShortestPath().addToPathFlow(residual);
        }

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
            Path sPath = getShortestPath(sensitivityCase);
            if (lPath != sPath){
                double lCost = sensitivityCase?lPath.getSensitivityCost():lPath.getCost();
                double sCost = sensitivityCase?sPath.getSensitivityCost():sPath.getCost();
                double lPathFlow = sensitivityCase?lPath.getSensitivityFlow():lPath.getFlow();
                
                double shiftValue = 0;
                if(lCost - sCost !=0)
                    shiftValue =  (lCost - sCost) / calDenom(sPath, lPath, sensitivityCase) ;
                if(!sensitivityCase){
                    shiftValue = Math.max(shiftValue, 0.0); //only positive flow can be shifted
                    shiftValue = Math.min(shiftValue, lPathFlow);
                }

                if(Double.isNaN(shiftValue) || Double.isInfinite(shiftValue)){
                    System.out.println("The flow requested to be shifted between the paths sPath:"+sPath+" and lPath:"+lPath+" has value of NaN or Infinity");
                    System.out.println("In particular, numerator lPath.getCost() - sPath.getCost()="+(lCost - sCost));
                    System.out.println("and denominator calDenom(sPath, lPath)="+calDenom(sPath, lPath, sensitivityCase));
                    System.exit(1);
                }
                if(sensitivityCase)
                    rate=1.0;
                shiftValue *= rate;

                if(!sensitivityCase){
                    sPath.addToPathFlow(shiftValue);
                    lPath.addToPathFlow(-shiftValue);
                }
                else{
                    sPath.addToSensitivityFlow(shiftValue);
                    lPath.addToSensitivityFlow(-1*shiftValue);
                }
            }
        }
    }
    
    //calculates the denominator for determining flow shift using gradient projection
    //Sums the travel time derivatives for all links that lie on one path but not the other
    //Note that calDenom() results are independent of the sensitivity case (which is interesting!!)
    //(because derivative of t'\alpha wrt \alpha is t'
    public double calDenom(Path sP, Path lP){
        return calDenom(sP, lP,false);
    }
    
    public double calDenom(Path sP, Path lP, boolean sensitivityCase){
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
    
    /**
     * This function is called after updating artificial OD pair's demand due to ALink's flow
     * Assigns the changeInDemand onto path flows in proportion to the current path flows
     * Note that changeInDemand may be negative!!
     * 
     * We only consider cases where more than one path and total demand>0
     * @param changeInDemand 
     */
    public void assignExtraDemandToPathFlows(double changeInDemand){
        if(this.pathSet.size()>0 && this.od.getDemand()>0.0){
            for (Path p : this.pathSet){
                double pathShare = p.getFlow()/this.od.getDemand();
                p.addToPathFlow(pathShare * changeInDemand);
            }
        }
        //calling this function outside of if, because there may be paths for an OD with zero demand
        //(recall updateArtificialLinks() used to add a path if no existing path for finding t_0 parameter)
        //by calling this function outside of the loop we will remove that created path before solving subnet
        dropPathsWFlowltThresh(.0000001);  
    }
    
    public void updateTimeDerivative(){
        double timeder = 0;
        if(this.pathSet.isEmpty()){
            System.out.println("Stem "+this+" pathset is empty and yet we are asked to calculates its time derivative");
            System.exit(1);
        }
        for (Path p : this.pathSet)
            timeder += p.getSensitivityCost();
        timeDerivative = timeder/this.pathSet.size();
    }

    public double getTimeDerivative() {
        return timeDerivative;
    }

    public double getTimeOriginToDest() {
        return timeOriginToDest;
    }

    public void setPathSet(Set<Path> pathSet) {
        this.pathSet = pathSet;
    }
    
}
