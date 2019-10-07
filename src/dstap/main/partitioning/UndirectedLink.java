/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import java.util.Set;

/**
 *
 * @author vp6258
 */
public class UndirectedLink{
    Set<UndirectedNode> allNodes;

    public UndirectedLink(Set<UndirectedNode> nodeSet) {
        allNodes = nodeSet;
        for(UndirectedNode n : nodeSet){
            n.addLink(this);
            
            for(UndirectedNode n2: nodeSet){
                if(!n.equals(n2))
                    n.addNode(n2);
            }
        }
    }
    
}