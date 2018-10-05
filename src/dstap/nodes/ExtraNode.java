/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.nodes;

/**
 * Nodes in subnetworks which were not a part of it originally
 * but are added to model the impact of other subnetworks' artificial
 * link
 * @author vp6258
 */
public class ExtraNode extends Node{
    public ExtraNode(int id, String t){
        super(id, t);
    }
}
