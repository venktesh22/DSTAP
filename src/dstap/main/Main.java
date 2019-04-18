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
    public static void main(String[] args){
        //===============================================//
        //================Input variables================//
        //===============================================//
        //@todo: move following elements as part of the args to the main function
        String folderName = "Networks/Grid_2/";
        String printVerbosityLevel = "MEDIUM"; //LEAST, LOW, MEDIUM, HIGH
        Boolean runSubnetsInParallel = true; //or false
        double demandFactor = 1.0; // 0.5 means 50% demand and likewise
        
//        DSTAPasAlgorithm optimizer = new DSTAPasAlgorithm(printVerbosityLevel, 
//                runSubnetsInParallel, demandFactor);
        
        DSTAPHeuristic optimizer = new DSTAPHeuristic(printVerbosityLevel, 
                runSubnetsInParallel, demandFactor);
        
        // We assume inputs are in the format where all subnetwork trips completely
        //within the subnetwork are in separate file than the trips from one
        //subnetwork to the other. This simplifies creation of artificial links.
        optimizer.readInputsAndInitialize(folderName);
    }
}
