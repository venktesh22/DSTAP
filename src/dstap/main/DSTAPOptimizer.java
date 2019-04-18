/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.network.*;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
        printNetworkReadingStatistics();
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
    }
    
}
