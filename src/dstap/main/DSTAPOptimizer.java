/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.links.Link;
import dstap.network.*;
import dstap.nodes.Node;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This modified version of Ehsan's code provides a different organization of files, variables, and inputs
 * It is a master class which can be inherited in two ways to perform following tasks:
 * a) Run DSTAP as an algorithm
 * b) Run DSTAP as a heuristic without subnetwork artificial links
 * @author Venktesh (venktesh at utexas dot edu)
 */
public abstract class DSTAPOptimizer {
    protected final List<String> subnetworkNames;
    protected MasterNetwork masterNet;
    protected FullNetwork fullNet;
    protected List<SubNetwork> subNets;
    
    protected String printVerbosityLevel; //LEAST, LOW, MEDIUM, HIGH
    protected Boolean runSubnetsInParallel; //or false
    protected double demandFactor;

    public DSTAPOptimizer() {
        this("LEAST",false,1.0);        
    }

    public DSTAPOptimizer(String printVerbosityLevel, Boolean runSubnetsInParallel, double demandFactor) {
        this.printVerbosityLevel = printVerbosityLevel;
        this.runSubnetsInParallel = runSubnetsInParallel;
        this.demandFactor = demandFactor;
        subnetworkNames = new ArrayList<>();
        subNets = new ArrayList<>();
    }
    //=============================================//
    //======Functions for reading files============//
    //=============================================//
    
    public void readInputsAndInitialize(String folderName){
        readSubnetNames(folderName+"/Inputs/");
        initAndRelateAllNetworks();
        readAllNetworkInputFiles(folderName+"/Inputs/");
        copyToFullNetwork();
        updateNodeList();
        generateArtificialLinksAndODPairs();
        updateNodeList();
        fullNet.createODPairMappings();
        printNetworkReadingStatistics();
        createODstems();
    }
    
    private void readSubnetNames(String folderName){
        try{
            Scanner fileIn= new Scanner(new File(folderName+"subnetNames.txt"));
            while(fileIn.hasNext()){
                String netName= fileIn.next();
                subnetworkNames.add(netName);
            }
            fileIn.close();
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
    }
    
    /**
     * Initializes all network variables in an order
     * Relate each network with the other. Like each subnetwork should know what 
     * object is masternetwork and likewise
     */
    private void initAndRelateAllNetworks(){
        masterNet = new MasterNetwork(printVerbosityLevel, "masterNet");
        for(String netName: subnetworkNames){
            SubNetwork temp= new SubNetwork(masterNet, printVerbosityLevel, netName);
            subNets.add(temp);
            masterNet.addSubNetwork(temp);
        }
        fullNet = new FullNetwork(masterNet, subNets, "fullNet", printVerbosityLevel);
        
        for(SubNetwork s1: subNets){
            List<SubNetwork> tempList = new ArrayList<>();
            for(SubNetwork s2:subNets){
                if(!s1.equals(s2))
                    tempList.add(s2);
            }
            s1.setOtherSubNet(tempList);
        }
    }
    
    protected void readAllNetworkInputFiles(String folderName){
        masterNet.readNetwork(folderName+"regionalLinks.txt");
        for(SubNetwork s: subNets){
            s.readInTrips(folderName+s.networkName+"In_Trips.txt", demandFactor);
            s.readNetwork(folderName+s.networkName+"_net.txt");
        }
        //we need to read network file for all subnetworks before we read OutTrips for all
        for(SubNetwork s:subNets){
            s.readOutTrips(folderName+s.networkName+"Out_Trips.txt", demandFactor);
        }
    }
    
    protected void copyToFullNetwork(){
        for(SubNetwork s: subNets)
            fullNet.copySubNetwork(s);
        fullNet.copyMasterNet(masterNet);
        
    }
    
    abstract void generateArtificialLinksAndODPairs();
    
    private void updateNodeList(){
        masterNet.updateNodeList();
        for(SubNetwork subnet: subNets){
            subnet.updateNodeList();
        }
    }
    
    private void printNetworkReadingStatistics(){
        masterNet.printMasterNetworkStatistics();
        for(SubNetwork subnet: subNets){
            subnet.printSubNetworkStatistics();
        }
        fullNet.printFullNetworkStatistics();
    }
    
    protected void createODstems(){
        masterNet.createODStems();
        for(SubNetwork s: subNets)
            s.createODStems();
        fullNet.createODStems();
    }
    
    //=============================================//
    //======Functions for solving DSTAP============//
    //=============================================//
    
    public void runOptimizer() throws InterruptedException{
        System.out.println("================\n====Solving DSTAP=====\n===============");
        boolean artificialLinksConstantTT = false; //if true, then sensivity analysis is not performed
        //(below) tracks if the optimizer has converged. Convergence criteria can be multi-faceted
        boolean hasConverged = false;
        
        double masterGap = 0.0001, masterODGap = 0.1, masterGapRate = 0.9;
        double subNetGap = 0.001, subNetODGap = 0.1, subNetGapRate = 0.2;
        //@todo for now ignoring the changes in rate based on new and old fullNetworkGap
        
        int itrNo = 0;
        
        while(!hasConverged){
            
            masterGap = masterGapRate * masterGap;
            masterODGap = Math.max(masterGap, 0.001);

            System.out.println("\n--Solving Master network in iteration: "+itrNo +" to a gap of "+masterGap);
            masterNet.solver(masterGap, masterODGap, itrNo);
            
            //function for updating subnetwork demand using masterNet artificial link flows
            masterNet.updateSubnetODsDemand();
            
            //function for solving each subnetwork in parallel
            subNetGap = subNetGapRate * subNetGap;
            subNetODGap = Math.max(subNetGap, 0.001);
            if(this.runSubnetsInParallel){
                double startTimeSubnetEval = System.currentTimeMillis();
                //got the information for parallelization from http://www.vogella.com/tutorials/JavaConcurrency/article.html
                ExecutorService executor = Executors.newFixedThreadPool(subnetworkNames.size());
                for(SubNetwork subNet: subNets){
                    Runnable worker = new SubnetSolverRunnable(subNet, subNetGap, subNetODGap, itrNo); //, getGap, costFunc);
                    executor.execute(worker);
                }
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.MINUTES); //wait for an UPPERLIMIT of 5 min when one subnet finishes and other is still running. 
                System.out.println("All threads finished");
                System.out.println("Time to solve the subnetworks: "+ (System.currentTimeMillis()-startTimeSubnetEval) + " milliseconds");

            }
            else{
                for (SubNetwork subNet : subNets)
                {
                    double startTimeSubnetEval = System.currentTimeMillis();
                    subNet.solver(subNetGap, subNetODGap, itrNo);
                    subNet.updateArtificialLinks(false, 1E-5);
                    System.out.println("Time to solve the subnetwork "+ subNet.networkName+": "+ (System.currentTimeMillis()-startTimeSubnetEval) + " milliseconds");
                }
            }
            System.out.println("\n\nMapping flow to full net");
            double startTForMappingFlows= System.currentTimeMillis();
            fullNet.mapDSTAPnetFlowToFullNet();
            System.out.println("Time to map DSTAP flows to master and subnets:"+(System.currentTimeMillis()-startTForMappingFlows) + " milliseconds");
            double fullNetGap = fullNet.getFullNetGap();
            
            
//            startTForMappingFlows= System.currentTimeMillis();
//            #fullNet.getExcessCosts();
//            fullNetGapValues.add(newFullGap);
//            System.out.println("Time to get excess costs:"+(System.currentTimeMillis()-startTForMappingFlows) + " milliseconds");
            System.out.println("\n\n=======Actual gap on full net is " + fullNetGap+"======\n");
            
            //
            itrNo++;
            if(itrNo>30)
                hasConverged = true; //for debug phase. Remove after code is done
        }
    }
}
