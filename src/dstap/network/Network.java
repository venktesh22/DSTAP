/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.network;

import dstap.ODs.*;
import dstap.links.*;
import dstap.nodes.Node;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author vp6258
 */
public abstract class Network {
    protected Set<Link> links;
    protected Set<Link> physicalLinks;
    
    protected Set<Node> nodes;
    protected Map<Integer, Node> nodesByID;
    protected TripTable tripTable;
    
    public String networkName;
    protected int printVerbosityLevel;
    
    //gap calculation variables
    protected double TSTT; //total system travel time
    protected double SPTT; //shortest path travel time (TSTT when all vehicles go on shortest path assuming constant costs)
    protected double initialGap;
    public List<Double> gapValues; //stores the gap values at end of each iteration (after end of solver)
    public List<Double> excessCosts; //stores the maxExcessCost at end of each itr
    public List<Double> avgExcessCosts; //stores the avgExcessCost at end of each itr
    
    public Network(){
        this(1);
    }
    
    public Network(int verbosityLevel){
        printVerbosityLevel = verbosityLevel;
        links = new HashSet<>();
        physicalLinks = new HashSet<>();
        
        nodes = new HashSet<>();
        nodesByID = new HashMap<>();
        tripTable = new TripTable();
        excessCosts = new ArrayList<>();
        avgExcessCosts = new ArrayList<>();
        gapValues = new ArrayList<>();
        initialGap = 1E2;
    }
        
    //@todo: replace Dijkstra with efficient shortest path routines
    /**
     * Solve for label and prev link for every node which is the shortest path distance from the origin
     * Also updates the SPTT value accordingly
     * @param origin 
     */
    public void dijkstras(Node origin){
        for(Node n : nodes){
            n.label = Double.MAX_VALUE;
            n.prev = null;
        }
        origin.label = 0.0;
        Set<Node> Q = new HashSet<>();
        Q.add(origin);

        int count=0;
        while(!Q.isEmpty()){
            Node u = null;
            double min = Double.MAX_VALUE;
            for(Node n : Q){
                if(n.label < min){
                    u = n;
                    min = n.label;
                }
            }
            count++;
            if(count> 10*Math.pow(nodes.size(),2)){
                System.out.println("Stuck in Dijkstra somehow");
            }
            Q.remove(u);
            for (Link l : u.getOutgoing()) {
                Node v = l.getDest();
                double alt = u.label + l.getTravelTime(l.getPreviousItrFlow());
                if(alt < v.label){
                    v.label = alt;
                    Q.add(v);
                    v.prev = l;
                }
            }
        }
        //update the SPTT variable
        if(this.tripTable.getOrigins().contains(origin)){
            for(ODPair od : tripTable.byOrigin(origin)){
                double demand = od.getDemand();
                if(od.getStem()!=null){
                    if (od.getStem().getPathSet().size() > 0  && demand>0.0){
                        double value = demand*od.getDest().label;
                        if(value< 1E-10){
                            System.out.println("Negative value. Exiting.");
                            System.exit(1);
                        }
//                        System.out.println("OD "+od+" has demand="+ demand+" and dest.label()="+od.getDest().label + " and product of the two="+value);
//                        System.out.println("OD's shortest path is "+trace(od));
//                        double cumulativeShortestPathCost = 0.0;
//                        for(Link l: trace(od).getPathLinks()){
//                            System.out.println("==This Path's Link "+l+" has TT of "+l.getTravelTime(l.getPreviousItrFlow()) +" and prev flow="+l.getPreviousItrFlow());
//                            cumulativeShortestPathCost += l.getTravelTime(l.getPreviousItrFlow());
//                        }
//                        System.out.println(" and the path's cumulative cost=" + cumulativeShortestPathCost+" and product with cost*demand="+(cumulativeShortestPathCost*demand));
                        SPTT += value;
                    }
                }
            }
        }
//        else{
//            System.out.println("Origin "+origin+" not found");
//        }
//        System.out.println(" Cumulated SPTT value = "+SPTT);
    }
    
    public Path trace(ODPair od){
        return trace(od.getOrigin(), od.getDest());
    }
    
    /**
     * Extract the link on shortest path from origin node to destination
     * @param origin
     * @param dest
     * @return the path variable
     */
    public Path trace(Node origin, Node dest){
        Path output = new Path(dest.label);
        Node curr = dest;
        while(curr != origin){
            if(curr.prev == null){
                break; //no previous link
            }
            output.addLinkToFront(curr.prev);
            curr = curr.prev.getSource();    // prev is a link object
        }
        if(curr!=origin){
            System.out.println("Found shortest path does not terminate at origin. Fix the issue");
            System.out.println("Origin:"+origin+", destination="+dest+", network"+this+", and path found from dest="+output);
            System.exit(1);
        }
        return output;
    }
    
    protected void traceShortestPathAndAddToODStem(ODPair od){
        Path temp = trace(od);
        if (!temp.getPathLinks().isEmpty()){
            od.getStem().addPath(temp);// adds shortest path if already not in the list
        }
    }
    
    public void updateNodeList(){
        for(Integer id: nodesByID.keySet()){
            if(!nodes.contains(nodesByID.get(id)))
                nodes.add(nodesByID.get(id));
        }
    }
    
    public void printNetworkStatistics(){
        System.out.println("\n=== Network "+networkName+" has following statistics=====");
        System.out.println(" No of nodes = "+nodes.size());
        System.out.println(" No of links = "+links.size());
        if(this.printVerbosityLevel>=4){
            System.out.println("  and the Links are: \n"+links);
        }
        System.out.println(" No of physical links = "+physicalLinks.size());
        System.out.println(" No. of origins = "+ tripTable.getOrigins().size());
        
        double demand=0.0;
        int odPairsNumber =0;
        for(Node origin: tripTable.getOrigins()){
            for(ODPair od: tripTable.byOrigin(origin)){
                demand+= od.getDemand();
                odPairsNumber++;
                if(this.printVerbosityLevel>=4)
                    System.out.println("---OD pair "+od+" has demand="+od.getDemand()+" and "+ ((od instanceof ArtificialODPair)?"Artificial":"Regular"));
            }
        }
        System.out.println(" No of OD pairs = "+ odPairsNumber);
        System.out.println(" Total demand = "+ demand);
    }
    
    public void createODStems(){
        for(Node origin: this.tripTable.getOrigins()){
            for(ODPair od: this.tripTable.byOrigin(origin)){
                od.setStem(new Stem(od)); //this step auotmatically associates the stem with the OD and vice versa
            }
        }
    }
    
    /**
     * The solver implements gradient projection to solve the assignment problem on the given network
     * Considers all network links (including artificial links and artificial/extraNode OD pairs)
     * A couple of details:
     * 1) We try not to repeat doing dijkstra, once for gap calculations and once
     * for shortest path for flow shifting calculations. This means, the getGap() method
     * is a bit complex. getGap() relies on the correct value of SPTT
     * @param gap: relative gap to which the solver should be solved (unit less)
     * @param odGap to which each OD pair is solved (units cost difference between longest and shortest paths)
     * @param itrNo useless for now...we might ignore it! Possibly replace with no of iterations
     * 
     * @todo: also include other variables like whether or not to store gaps, excess cost values, Beckmann function value etc.
     */
    public void solver(double gap, double odGap, int itrNo){
        if(this.printVerbosityLevel >=1){
            System.out.println("Solving network "+this.networkName +" to a gap of "+gap+" or till 500 iterations of gradient projection");
        }
        boolean converged = false;
        //@todo: change the following statement about storing gap values to false if not needed
        int subItrNo =0;
        if(true){
            this.setSPTT(0.0);
            resetLinkPrevItrFlows();
            for(Node origin : this.tripTable.getOrigins())
                dijkstras(origin); //updates SPTT
            this.initialGap = getGap();
//            this.gapValues.add(initialGap);
            System.out.println(this.networkName+" initial gap =\t"+initialGap);
//            if(this.initialGap<gap){ //commented: read point below
//                converged = true;
//            }
        }
        if(this.printVerbosityLevel >=2){
            System.out.println("Subiteration\tGap for "+this.networkName+"\tTime taken this subitr(sec)");
        }
        //regardless of if the first subitr gap< desired gap we run one iteration at least
        //this is because suppose an OD pair which had zero demand earlier now has a demand
        //gap of previous network was solved with OD with zero demand, and initial gap which
        //is same as gap of previous mainItr will still satisfy that, but we still need to load
        //the demand onto the new paths
        
        while(!converged){
//            System.out.println("!!!!!!!!!!!!!!Starting subIteration "+subItrNo);
            double time = System.currentTimeMillis();
            int count = 0;
            this.setSPTT(0.0); //everytime before running dijkstra for all origins we reset SPTT
            this.updateTSTT();
            double beforeTSTT = this.TSTT;
            resetLinkPrevItrFlows();
            
            for(Node origin : this.tripTable.getOrigins()){
                dijkstras(origin); //updates SPTT
                for(ODPair od : this.tripTable.byOrigin(origin)){
                    count++;
                    double demand = od.getDemand(); //includes both subnetwork+regional demand
                    if (demand > 0){
                        traceShortestPathAndAddToODStem(od);
                        //first iteration where only one path and no flow on that path
                        if ((od.getStem().getPathSet().size() == 1) && (od.getStem().getShortestPath().getFlow()< 1E-10)){
                            od.getStem().getShortestPath().addToPathFlow(demand);
                            od.getStem().updateCost();
                        } 
                        else if (od.getStem().getPathSet().size() > 1){
                            int odCounter = 0;
                            double costDiff = od.getStem().getCostDiffBetLongestAndShortestPaths();
                            boolean isODConverged = (costDiff<odGap);
//                            System.out.println("===Cost difference for this OD is "+costDiff);

                            while (!isODConverged){// && odCounter<5){
                                odCounter++;
                                /*
                                shift flow between longest and shortest paths and update their associated link flow
                                 */
//                                od.limitedShiftFlow(rate);
                                od.getStem().shiftLimitedFlowFromAllPathstoSP(0.2,false);

                                od.getStem().updateCost();
                                od.getStem().dropPathsWFlowltThresh(1E-9); /* removes unused paths */
                                if(itrNo>-1){
                                    costDiff = od.getStem().getCostDiffBetLongestAndShortestPaths();
                                    isODConverged = (costDiff<odGap);
//                                    System.out.println("Cost difference for OD pair "+od+" is "+costDiff);
                                }
                                else
                                    isODConverged = true;
//                                isODConverged = true; //Forcing one iteration of Netwon's method for flow shifting.
                            } 
                        }
                    }//end of demand>0 if
                    //@todo: remove consistency checks all the time
                    if(!od.checkFlowConsistency()){
                        System.out.println("OD pair "+od+" is flow inconsistent. Terminating");
                        System.exit(1);
                    }
                }//end of ODpair for loop
            }//end of looping through all origins
            double gapAtBeginningOfThisSubItr = getGap(beforeTSTT); //we have to get gap at end only because SPTT is updated only after solving all origin dijkstra
            if(gapAtBeginningOfThisSubItr<gap || subItrNo>500)
                converged = true;
            if(this.printVerbosityLevel >=2){
                System.out.println(subItrNo+"\t"+gapAtBeginningOfThisSubItr+"\t"+((System.currentTimeMillis()-time)/1000.0));
            }
            subItrNo++;
            
            //print flows on each path
//            printODPathFlows();
//            for(Link l: links){
//                System.out.println("**Link "+l+" has flow="+l.getFlow()+" and TT="+l.getTravelTime());
//            }
        }
        for(Node origin : this.tripTable.getOrigins()){
            for(ODPair od : this.tripTable.byOrigin(origin)){
                if(!od.checkFlowConsistency()){
                    System.out.println("OD pair "+od+" is flow inconsistent. Terminating");
                    System.exit(1);
                }
            }
        }
        
        
        //evaluate final gap
        this.setSPTT(0.0);
        resetLinkPrevItrFlows();
        for(Node origin : this.tripTable.getOrigins())
            dijkstras(origin); //updates SPTT
        double finalGap = getGap();
        this.gapValues.add(finalGap);
        this.updateExcessCosts();
        if(printVerbosityLevel>=1){
            System.out.println("Solver ended in "+subItrNo+" subiterations.");
            System.out.println("Final gap= "+finalGap+", Max Excess Cost= "+this.excessCosts.get(excessCosts.size()-1)
            +", and AEC= "+this.avgExcessCosts.get(avgExcessCosts.size()-1));
        }
    }
    
    /**
     * Updates max and avg access costs. Relies on the fact that dijkstra already
     * found a shortest path for the network. We only find excess costs at end of the
     * solver so dijkstra has been run once before. For fullNetwork where we
     * do not run a dijkstra to update the gap, make sure to run Dijkstra explicitly
     */
    public void updateExcessCosts(){
        double maxExCost = -1000.0;
        double avgExCost = 0.0;
        int count = 0;

        for (Node origin: this.tripTable.getOrigins()){
            for(ODPair od : this.tripTable.byOrigin(origin)){
                if (od.getStem().getPathSet().size() > 0){
                    double minCost = od.getStem().getShortestPathCost();
                    for (Path p : od.getStem().getPathSet()){
                        double ec = p.getCost() - minCost;
                        avgExCost += ec;
                        count++;
                        if (ec > maxExCost){
                            maxExCost = ec;
                        }
                    }
                }
            }
        }
        avgExCost = avgExCost/count;
        excessCosts.add(maxExCost);
        avgExcessCosts.add(avgExCost);
    }
    
    protected double getGap(){
        updateTSTT();
        return getGap(this.TSTT);
    }
    
    /**
     * Return the relative gap
     * It relies on the fact that SPTT was updated while solving dijkstra earlier
     * So before calling this function make sure to
     * (a) reset previous link flows to be equal to current link flows 
     * (coz dijkstra relies on previous link flows to prevent asynchronous updates...like dijkstra for one origin should
     * not use the modified flow from an earlier origin's flow shifting)
     * (b) solve dijkstra
     * @return 
     */
    protected double getGap(double prevTSTT){
        if(prevTSTT<1E-10 && this.SPTT<1E-10){
            return 1.0; //0/0 is not defined
        }
        else if(prevTSTT<1E-8 && this.SPTT>0.0){
            //demand not loaded yet
            return 1.0;
        }
        if(getSPTT()- prevTSTT >1e-8){
            System.out.println("SPTT="+getSPTT()+" while TSTT="+prevTSTT+" causing negative gap. Exiting!");
            System.exit(1);
        }
        double g=(1 - (getSPTT()/prevTSTT));
        return g;
    }
    
    protected void updateTSTT(){
        double tstt = 0.0;
        for (Node origin: this.tripTable.getOrigins()){
            for(ODPair od : this.tripTable.byOrigin(origin)){
                double allODPairPathsCumulativeCost = 0.0;
                if(od.getStem()!=null){
                    for (Path p : od.getStem().getPathSet()){
                        if(Double.isNaN(p.getCost()) || Double.isNaN(p.getFlow())){
                            System.out.println("Path flow or cost for path "+ p+" are NaN--- path flow:"+p.getFlow()+"---path cost:"+p.getCost());
                            System.exit(1);
                        }
                        double pCost = p.getCost();
                        double pFlow = p.getFlow();
//                        System.out.println("--TSTT calc OD pair="+od+" for path="+p+" p.cost()="+pCost+" p.flow()="+pFlow+" and product="+ (pCost*pFlow));
                        tstt += pCost*pFlow;
                        allODPairPathsCumulativeCost+= (pCost*pFlow);
                    }
                }
//                System.out.println("TSTT Calc OD pair="+od+" with cumulative cost="+allODPairPathsCumulativeCost);
            }
        }
        if(Double.isNaN(tstt) && tstt<0.0)
            System.exit(1);
        this.TSTT = tstt;
//        System.out.println("\n Cumulative TSTT value="+tstt);
    }
    
    /**
     * This function relies on dijkstra to update the SPTT.
     * we assume that getSPTT is only called after dijkstra has atleast been run for every origin in the network
     * (because running dijkstra can be very time consuming)
     * if not, we encourage running dijkstra first
     * @return 
     */
    protected double getSPTT(){
        if(this.SPTT<=0.0){
            System.out.println("SPTT is zero or negative which is not possible. Exiting!");
            System.exit(1);
        }
        return this.SPTT;
    }
    
    //we only allow setting of SPTT to zero before starting to call all Dijkstra
    //@todo: make this more secure. One shouldn't be able to edit SPTT from outside very easily
    protected void setSPTT(double sptt){
        this.SPTT = sptt;
    }
    
    protected void printODPathFlows(){
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        for(Node origin : this.tripTable.getOrigins()){
            for(ODPair od : this.tripTable.byOrigin(origin)){
                for(Path p: od.getStem().getPathSet()){
                    System.out.println("OD "+od+" has path="+p+" with flow="+p.getFlow()+" and cost="+p.getCost());
                }
            }
        }
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
    
    public void printAllLinkFlows(){
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        for(Link l: links){
            System.out.println("Link "+l+" has flow of "+l.getFlow()+" and travel time is "+l.getTravelTime()+" with coef="+l.getCoef()+" and fftt="+l.getFFTime());
        }
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    }
    
    protected void resetLinkPrevItrFlows(){
        for(Link l: links)
            l.resetPreviousItrFlow();
    }
    
}
