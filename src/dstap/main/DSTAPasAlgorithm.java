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
public class DSTAPasAlgorithm extends DSTAPOptimizer{
    
    public DSTAPasAlgorithm(String printVerbosityLevel, Boolean runSubnetsInParallel){
        super(printVerbosityLevel, runSubnetsInParallel);
    }
    
    @Override
    protected void generateArtificialLinksAndODPairs(){
        for(SubNetwork s: subNets){
            s.generateOriginsAndDestDueToOtherSubnet();
            s.updateNodeList();
        }
        for(SubNetwork s: subNets)
            s.createMasterNetArtificialLinksAndItsODPair();

        for(SubNetwork s: subNets)
            s.createThisSubnetODPairs_otherSubnetALink();
    }
}
