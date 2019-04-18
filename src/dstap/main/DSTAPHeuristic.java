/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.network.SubNetwork;

/**
 *
 * @author vp6258
 */
public class DSTAPHeuristic extends DSTAPOptimizer{
    
    public DSTAPHeuristic(){
        super();
    }
    
    public DSTAPHeuristic(String printVerbosityLevel, Boolean runSubnetsInParallel, double demandFactor){
        super(printVerbosityLevel, runSubnetsInParallel, demandFactor);
    }

    @Override
    public void generateArtificialLinksAndODPairs(){
        for(SubNetwork s: subNets){
            s.generateOriginsAndDestDueToOtherSubnet();
            s.updateNodeList();
        }
        for(SubNetwork s: subNets)
            s.createMasterNetArtificialLinksAndItsODPair();
    }
    
    
}
