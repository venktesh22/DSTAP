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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
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
    
    protected int printVerbosityLevel; //LEAST=1, LOW=2, MEDIUM=3, HIGH=4
    protected Boolean runSubnetsInParallel; //or false
    
    protected String outputFolderName;
    
    protected Boolean writeOutputFiles;
    
    protected  double INITIALMASTERGAP;
    protected  double INITIAL_MASTER_OD_GAP;
    protected  double INITIAL_MASTER_GAP_RATE;
    protected  double INITIALSUBNETGAP;
    protected  double INITIAL_SUBNET_OD_GAP;
    protected  double INITIAL_SUBNET_GAP_RATE;
    protected double DESIRED_FULLNET_GAP;
    protected int MAX_OUTER_ITERATIONS;
    protected  double DEMAND_FACTOR;
    

    public DSTAPOptimizer() {
        this("LEAST",false);        
    }

    public DSTAPOptimizer(String printVerbosityLevel, Boolean runSubnetsInParallel) {
        switch(printVerbosityLevel){
            case "LEAST":
                this.printVerbosityLevel = 1;
                break;
            case "LESS":
                this.printVerbosityLevel = 2;
                break;
            case "MEDIUM":
                this.printVerbosityLevel = 3;
                break;
            case "HIGH":
                this.printVerbosityLevel = 4;
                break;
            default:
                this.printVerbosityLevel = 2;
                break;
        }
        this.runSubnetsInParallel = runSubnetsInParallel;
        //this.D = demandFactor;
        subnetworkNames = new ArrayList<>();
        subNets = new ArrayList<>();
        //debug mode
        writeOutputFiles = true;
    }
    
    //=============================================//
    //======Functions for reading files============//
    //=============================================//
    
    public void readInputsAndInitialize(String folderName, String partitionSubFolder){
        readParametersFile(folderName+"/Inputs/"+partitionSubFolder+"/");
        readSubnetNames(folderName+"/Inputs/"+partitionSubFolder+"/");
        initAndRelateAllNetworks();
        readAllNetworkInputFiles(folderName+"/Inputs/"+partitionSubFolder+"/");
        
        copyToFullNetwork();
        updateNodeList();
        generateArtificialLinksAndODPairs();
        updateNodeList();
        fullNet.createODPairMappings();
        printNetworkReadingStatistics();
        createODstems();
        
        if(writeOutputFiles)
            createOutputDirectory(folderName, partitionSubFolder);
    }
    
    private void readSubnetNames(String folderName){
        try{
            Scanner fileIn= new Scanner(new File(folderName+"subnetNames.txt"));
            if(this.printVerbosityLevel>=4)
                System.out.print("Reading subnetwork names:");
            while(fileIn.hasNext()){
                String netName= fileIn.next();
                subnetworkNames.add(netName);
            }
            fileIn.close();
            if(this.printVerbosityLevel>=4)
                System.out.print("Done.\n");
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
    }
    
    /**
     * reads the parameters like OD gap etc 
     * @param folderName 
     */
    private void readParametersFile(String folderName){
        try{
            Scanner fileIn= new Scanner(new File(folderName+"Parameters.txt"));
            if(this.printVerbosityLevel>=3)
                System.out.print("Reading simulation parameters:");
            
            while(fileIn.hasNext()){
                String parameterName= fileIn.next();
                switch(parameterName){
                    case "initialMasterGap":
                        this.INITIALMASTERGAP = Double.parseDouble(fileIn.next());
                        break;
                    case "intialMasterODGap":
                        this.INITIAL_MASTER_OD_GAP = Double.parseDouble(fileIn.next());
                        break;
                    case "initialMasterGapRate":
                        this.INITIAL_MASTER_GAP_RATE = Double.parseDouble(fileIn.next());
                        break;
                    case "initialSubNetGap":
                        this.INITIALSUBNETGAP = Double.parseDouble(fileIn.next());
                        break;
                    case "intialSubNetODGap":
                        this.INITIAL_SUBNET_OD_GAP = Double.parseDouble(fileIn.next());
                        break;
                    case "intialSubNetGapRate":
                        this.INITIAL_SUBNET_GAP_RATE = Double.parseDouble(fileIn.next());
                        break;    
                    case "desiredFullNetGap":
                        this.DESIRED_FULLNET_GAP = Double.parseDouble(fileIn.next());
                        break;
                    case "maxIterations":
                        this.MAX_OUTER_ITERATIONS = Integer.parseInt(fileIn.next());
                        break;
                    case "demandFactor":
                        this.DEMAND_FACTOR = Double.parseDouble(fileIn.next());
                        break;
                    default:
                        break;
                }
            }
            fileIn.close();
            if(this.printVerbosityLevel>=3){
                System.out.print("Done. \n");
            }
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
            s.readInTrips(folderName+s.networkName+"In_Trips.txt", this.DEMAND_FACTOR);
            s.readNetwork(folderName+s.networkName+"_net.txt");
        }
        //we need to read network file for all subnetworks before we read OutTrips for all
        for(SubNetwork s:subNets){
            s.readOutTrips(folderName+s.networkName+"Out_Trips.txt", this.DEMAND_FACTOR);
        }
        if(this.printVerbosityLevel>=1){
            System.out.println("All network input files read");
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
    /**
     * Creates a subdirectory with a new name obtained from local computer's 
     * @param folderName 
     * @param partitionSubFolder 
     */
    protected void createOutputDirectory(String folderName, String partitionSubFolder){
        String epoch= Integer.toString((int)(System.currentTimeMillis()/1000.0));
        epoch = partitionSubFolder+"_"+epoch;
        File dir = new File(folderName+"/Outputs/"+epoch);
    
        // attempt to create the directory here
        boolean successful = dir.mkdir();
        if (!successful){
            System.out.println("failed trying to create the directory");
        }
        this.outputFolderName= folderName+"/Outputs/"+epoch+"/";
        
        File source = new File(folderName+"/Inputs/"+partitionSubFolder+"/Parameters.txt");
        File dest = new File(this.outputFolderName+"Parameters.txt");
        try {
            Files.copy(source.toPath(), dest.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //=============================================//
    //======Functions for solving DSTAP============//
    //=============================================//
    
    public void runOptimizer() throws InterruptedException{
        System.out.println("================\n====Solving DSTAP=====\n===============");
        boolean artificialLinksConstantTT = false; //if true, then sensivity analysis is not performed
        //(below) tracks if the optimizer has converged. Convergence criteria can be multi-faceted
        boolean hasConverged = false;
        
        double masterGap = this.INITIALMASTERGAP, masterODGap = this.INITIAL_MASTER_OD_GAP, masterGapRate = this.INITIAL_MASTER_GAP_RATE;
        double subNetGap = this.INITIALSUBNETGAP, subNetODGap = this.INITIAL_SUBNET_OD_GAP, subNetGapRate = this.INITIAL_SUBNET_GAP_RATE;
        //@todo for now ignoring the changes in rate based on new and old fullNetworkGap
        
        int itrNo = 0;
        
        while(!hasConverged){
            System.out.println("\n------------Starting Iteration No. "+itrNo+"---------------\n");
            masterGap = masterGapRate * masterGap;
            masterODGap = Math.max(masterODGap*masterGap, 0.001);

//            System.out.println("\n--Solving Master network in iteration: "+itrNo +" to a gap of "+masterGap);
            masterNet.solver(masterGap, masterODGap, itrNo);
            
            //function for updating subnetwork demand using masterNet artificial link flows
            masterNet.updateSubnetODsDemand();
            
            //function for solving each subnetwork in parallel
            subNetGap = subNetGapRate * subNetGap;
            subNetODGap = Math.max(subNetODGap*subNetGapRate, 0.001);
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
                for (SubNetwork subNet : subNets){
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
            double fullNetGap = fullNet.getFullNetGapAndUpdateExcessCosts();
            
            
            System.out.println("\n\n=======Actual gap on full net is " + fullNetGap+"======\n");
            
            //
            itrNo++;
            if(fullNetGap< this.DESIRED_FULLNET_GAP || itrNo > this.MAX_OUTER_ITERATIONS)
                hasConverged=true;
//            if(itrNo>30)
//                hasConverged = true; //for debug phase. Remove after code is done
        }
        if(writeOutputFiles){
            this.printExcessCostsAndGaps();
            try{
            fullNet.printFull_LinkFlows(outputFolderName);
            fullNet.printFull_TTs(outputFolderName);
            }catch(Exception e){
                e.printStackTrace();
                return;
            }
        }
    }
    
    private void printExcessCostsAndGaps(){
//        System.out.println("\n\nItrNo\tMasterGap\tFullNetGap\tMasterMEC\tMasterAEC\tFullNetMEC\tFullNetAEC");
//        for(int i=0;i< itrNo; i++){
//            System.out.println((i+1)+"\t"+masterNet.gapValues.get(i)+"\t"+
//                    fullNet.gapValues.get(i)+"\t"+
//                    masterNet.excessCosts.get(i)+"\t"+
//                    masterNet.avgExcessCosts.get(i)+"\t"+
//                    fullNet.excessCosts.get(i)+"\t"+
//                    fullNet.avgExcessCosts.get(i));
//        }
        
        try{
            PrintWriter fileOut = new PrintWriter(new File(this.outputFolderName+"excessCosts.txt"));
            fileOut.print("iteration \t master_max \t master_avg \t");
            for(SubNetwork s: subNets)
                fileOut.print(s.networkName+"_max \t "+s.networkName+"_avg \t");
            fileOut.print("full_max \t full_avg\n");

            int count = masterNet.excessCosts.size(); //all of those have identical counts
            for (int i = 0; i < count; i++){
                fileOut.print(i+"\t"+masterNet.excessCosts.get(i)+"\t"+masterNet.avgExcessCosts.get(i)+"\t");
                for(SubNetwork s: this.subNets)
                    fileOut.print(s.excessCosts.get(i)+"\t"+s.avgExcessCosts.get(i)+"\t");
                fileOut.print(fullNet.excessCosts.get(i)+"\t"+fullNet.avgExcessCosts.get(i)+"\n");
    //                    "\t"++nNet.excessCosts.get(i)+"\t"+nNet.avgExcessCosts.get(i)+
    //                    "\t"+fullNet.excessCosts.get(i)+"\t"+fullNet.avgExcessCosts.get(i));

            }
            fileOut.flush();
            fileOut.close();
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
        
        try{
            PrintWriter fileOut = new PrintWriter(new File(this.outputFolderName+"gapValues.txt"));
            fileOut.print("iteration \t master_gap \t");
            for(SubNetwork s: subNets)
                fileOut.print(s.networkName+"_gap \t");
            fileOut.print("full_gap\n");

            int count = masterNet.gapValues.size(); //all of those have identical counts
            for (int i = 0; i < count; i++){
                fileOut.print(i+"\t"+masterNet.gapValues.get(i)+"\t");
                for(SubNetwork s: this.subNets)
                    fileOut.print(s.gapValues.get(i)+"\t");
                fileOut.print(fullNet.gapValues.get(i)+"\n");
    //                    "\t"++nNet.excessCosts.get(i)+"\t"+nNet.avgExcessCosts.get(i)+
    //                    "\t"+fullNet.excessCosts.get(i)+"\t"+fullNet.avgExcessCosts.get(i));

            }
            fileOut.flush();
            fileOut.close();
        }catch(IOException e){
            e.printStackTrace();
            return;
        }
    }
}
