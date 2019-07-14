/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.main.partitioning.SDDAlgorithm;
import dstap.network.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vp6258
 */
public class Main {
    /**
     * @param args folder location that contains all input files for the code
     */
    public static void main(String[] args) throws InterruptedException, FileNotFoundException{
        /**
         * Outline of ideas for integrating SDDA with DSTAP code:
         * a) input while running the code that specifies whether to run partition and whether to not
         * b) If run partition, then 
         * c) If not, then specify the folder from which the inputs will be read and likewise
         */
        boolean runPartitioning = Boolean.parseBoolean(args[3]);
        String netName = args[0];
        String subFolderName="SDDA_1562904751"; //subfolder inside INPUTS that contains the partition files...
        if(runPartitioning){
            int noOfClusters=2;
            SDDAlgorithm algo = new SDDAlgorithm(2, netName);
            algo.readAndPrepareNetworkFiles();
            algo.runSDDAalgorithm();
            subFolderName = algo.generateSubNetNames(netName, noOfClusters);
            algo.generateNetworkFileOutput(netName, noOfClusters);
            algo.generateODTripsOutput(netName, noOfClusters);
        }
//        System.exit(0);
        //===============================================//
        //================Input variables================//
        //===============================================//
        String folderName = "Networks/"+args[0]+"/";
//        String subFolderName = ""; 
        
        String printVerbosityLevel = args[1]; //LEAST, LOW, MEDIUM, HIGH
        
        Boolean runSubnetsInParallel = Boolean.getBoolean(args[2]); //or false
        
//        DSTAPasAlgorithm optimizer = new DSTAPasAlgorithm(printVerbosityLevel, 
//                runSubnetsInParallel, demandFactor);
        
        DSTAPHeuristic optimizer = new DSTAPHeuristic(printVerbosityLevel, 
                runSubnetsInParallel);
        
        // We assume inputs are in the format where all subnetwork trips completely
        //within the subnetwork are in separate file than the trips from one
        //subnetwork to the other. This simplifies creation of artificial links.
        optimizer.readInputsAndInitialize(folderName, subFolderName);
        optimizer.runOptimizer();
    }
}
