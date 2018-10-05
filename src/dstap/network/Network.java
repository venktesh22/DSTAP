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
abstract class Network {
    protected Set<Link> links;
    protected Set<Link> physicalLinks;
    protected Set<ArtificialLink> artificialLinks;
    
    protected Set<Node> nodes;
    protected Map<Integer, Node> nodesByID;
    protected TripTable tripTable;
//    protected Set<ODPair> ODPairs;
    
    public String networkName;
    protected String printVerbosityLevel;
    
    //gap calculation variables
    protected double TSTT; //total system travel time
    protected double SPTT; //shortest path travel time
    protected List<Double> gapValues;
    protected List<Double> excessCosts;
    protected List<Double> avgExcessCosts;
    
    public Network(){
        printVerbosityLevel = "LEAST";
        links = new HashSet<>();
        physicalLinks = new HashSet<>();
        artificialLinks = new HashSet<>();
        
        nodes = new HashSet<>();
        nodesByID = new HashMap<>();
        tripTable = new TripTable();
        excessCosts = new ArrayList<>();
        avgExcessCosts = new ArrayList<>();
        gapValues = new ArrayList<>();
    }
    
    public Network(String verbosityLevel){
        printVerbosityLevel = verbosityLevel;
        links = new HashSet<>();
        physicalLinks = new HashSet<>();
        artificialLinks = new HashSet<>();
        
        nodes = new HashSet<>();
        nodesByID = new HashMap<>();
        tripTable = new TripTable();
        excessCosts = new ArrayList<>();
        avgExcessCosts = new ArrayList<>();
        gapValues = new ArrayList<>();
    }
        
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
            if(count> nodes.size()*2){
                System.out.println("Stuck in Dijkstra somehow");
            }
            Q.remove(u);
            for (Link l : u.getOutgoing()){
                Node v = l.getDest();
                double alt = u.label + l.getTravelTime();
                if(alt < v.label){
                    v.label = alt;
                    Q.add(v);
                    v.prev = l;
                }
            }
        }
    }
    
}
