/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.network.*;
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
    public static void main(String[] args) throws InterruptedException{
        //===============================================//
        //================Input variables================//
        //===============================================//
        String folderName = "Networks/"+args[0]+"/";
        
        String printVerbosityLevel = args[1]; //LEAST, LOW, MEDIUM, HIGH
        
        Boolean runSubnetsInParallel = Boolean.getBoolean(args[2]); //or false
        
//        DSTAPasAlgorithm optimizer = new DSTAPasAlgorithm(printVerbosityLevel, 
//                runSubnetsInParallel, demandFactor);
        
        DSTAPHeuristic optimizer = new DSTAPHeuristic(printVerbosityLevel, 
                runSubnetsInParallel);
        
        // We assume inputs are in the format where all subnetwork trips completely
        //within the subnetwork are in separate file than the trips from one
        //subnetwork to the other. This simplifies creation of artificial links.
        optimizer.readInputsAndInitialize(folderName);
        optimizer.runOptimizer();
    }
}
