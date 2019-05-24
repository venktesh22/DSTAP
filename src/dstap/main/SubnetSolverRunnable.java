/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main;

import dstap.network.SubNetwork;

/**
 * The class which runs the subnetwork equilibrium in parallel
 * @author vp6258
 */
public class SubnetSolverRunnable implements Runnable{

    private final SubNetwork subNet;
    private final double subGap;
    private final double subOdGap;
    private final int itr;
//    private final boolean getGap;
//    private final boolean costFunc;
    
    SubnetSolverRunnable(SubNetwork s, double sG, double sOG, int i){ //, boolean gG, boolean cF){
        this.subNet=s;
        this.subGap = sG;
        this.subOdGap = sOG;
        this.itr=i;
    }
//        this.getGap=gG;
//        costFunc=cF;
    
    @Override
    public void run() {
        subNet.solver(subGap, subOdGap, itr);
        subNet.updateArtificialLinks(false, 1E-5);
    }
    
}
