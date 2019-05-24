/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.ODs;

import dstap.nodes.Node;
import java.util.HashMap;
import java.util.HashSet;
import java.util.*;
import java.util.Map;
import java.util.Set;

/**
 * Contains all OD pairs and provides functions to access OD pairs by origin and
 * destination
 * @author vp6258
 */
public class TripTable implements Iterable<ODPair> {
    private final Map<Node, Map<Node, ODPair>> odPairs; //includes artificial as well
    private Map<Node, Map<Node, ArtificialODPair>> artificialODPairs; 


    private final Set<Node> origins;
    private final Set<Node> dests;

    public TripTable(){
        odPairs = new HashMap<>();
        artificialODPairs = new HashMap<>();
        origins = new HashSet<>();
        dests = new HashSet<>();
    }

    public ODPair getODPair(Node origin, Node dest){
        if(!odPairs.containsKey(origin)){
            System.out.println("Triptable: origin was nor found\t"+origin);
            System.exit(1);
        } else if (!odPairs.get(origin).containsKey(dest)){
            System.out.println("Triptable: dest was nor found\t"+origin);
            System.exit(1);
        }
        return odPairs.get(origin).get(dest);
    }
    
    public ArtificialODPair getArtificialODPair(Node origin, Node dest){
        if(!artificialODPairs.containsKey(origin)){
            System.out.println("Triptable: origin was not found\t"+origin);
            System.exit(1);
        } else if (!artificialODPairs.get(origin).containsKey(dest)){
            System.out.println("Triptable: dest was not found\t"+origin);
            System.exit(1);
        }
        return artificialODPairs.get(origin).get(dest);
    }


    public void addODpair(Node o, Node d){
        Map<Node, ODPair> temp;
        if(odPairs.containsKey(o)){
            temp = odPairs.get(o);
        }
        else{
            odPairs.put(o, temp = new HashMap<>());
        }
        if(temp.containsKey(d)){
            temp.get(d).addToDemand(0.0); //we already have this OD pair defined, so simply add that demand to original demand
        }
        else
            temp.put(d, new ODPair(o, d));
    }

    public void addODpair(Node o, Node d, double demand){
        Map<Node, ODPair> temp;
        if(odPairs.containsKey(o)){
            temp = odPairs.get(o);
        }
        else{
            odPairs.put(o, temp = new HashMap<>());
        }
        
        if(temp.containsKey(d)){
            temp.get(d).addToDemand(demand); //we already have this OD pair defined, so simply add that demand to original demand
        }
        else
            temp.put(d, new ODPair(o, d, demand));
        if (!origins.contains(o)){
            origins.add(o);
        }
        if (!dests.contains(d)){
            dests.add(d);
        }
    }


    public void addODpair(Node o, Node d, double demand, String netName){
        Map<Node, ODPair> temp;

        if(odPairs.containsKey(o)){
            temp = odPairs.get(o);
        }
        else{
            odPairs.put(o, temp = new HashMap<>());
        }
        if(temp.containsKey(d)){
            temp.get(d).addToDemand(demand); //we already have this OD pair defined, so simply add that demand to original demand
        }
        else
            temp.put(d, new ODPair(o, d, demand, netName));
        if (!origins.contains(o)){
            origins.add(o);
        }
        if (!dests.contains(d)){
            dests.add(d);
        }
    }
    
    //every OD pair is of
    public void addArtificialODPair(Node o, Node d, double demand){
        //check if there already exists an ODpair object between o and d
        if(odPairs.containsKey(o)){
            if(odPairs.get(o).containsKey(d)){
                demand = demand + odPairs.get(o).get(d).getDemand();
                odPairs.get(o).put(d, null); //get rid of regular OD pair created before because we are going to call it artificial OD pair!
            }
        }
        
        
        ArtificialODPair od= null;
        Map<Node, ArtificialODPair> temp;
        if(artificialODPairs.containsKey(o)){
            temp = artificialODPairs.get(o);
        }
        else{
            artificialODPairs.put(o, temp = new HashMap<>());
        }
        temp.put(d, od=new ArtificialODPair(o, d, demand));
        
        Map<Node, ODPair> temp2;
        if(odPairs.containsKey(o)){
            temp2 = odPairs.get(o);
        }
        else{
            odPairs.put(o, temp2 = new HashMap<>());
        }
        temp2.put(d, (ODPair) od);
        if (!origins.contains(o)){
            origins.add(o);
        }
        if (!dests.contains(d)){
            dests.add(d);
        }
        
//        addODpair(o, d, demand); //ODpair contains all artificials as well
    }

    public void addOrigin(Node o){
        origins.add(o);
        if (!odPairs.containsKey(o)){
            odPairs.put(o, new HashMap<>());
        }
    }

    public void addDest(Node d){
        dests.add(d);
    }

    public Set<Node> getOrigins(){
        return origins;
    }

    public Set<Node> getDests(){
        return dests;
    }

    public Map<Node, Map<Node, ArtificialODPair>> getArtificialODPairs() {
        return artificialODPairs;
    }

    public Map<Node, Map<Node, ODPair>> getTrips(){
        return odPairs;
    }

    public void add(ODPair od){
        Map<Node, ODPair> temp;
        Node o = od.getOrigin();
        Node d = od.getDest();

        if(odPairs.containsKey(o)){
            temp = odPairs.get(o);
        }
        else{
            odPairs.put(o, temp = new HashMap<>());
            origins.add(o);
        }

        temp.put(d, od);
        dests.add(d);
    }

    public Iterable<ODPair> byOrigin(Node origin){
        return new MapValueIterable<>(odPairs.get(origin));
    }
    
//    public Iterable<ODPair> byOriginArtificial(Node origin){
//        return new MapValueIterable<>(artificialODPairs.get(origin));
//    }

    @Override
    public Iterator<ODPair> iterator(){
        return new TripTableIterator();
    }
    
    class TripTableIterator implements Iterator<ODPair>{
        private Iterator<ODPair> inner;  // iterates over destinations of each origin and returns the associated ODpair object
        private final Iterator<Node> outer;  // iterates over origins

        public TripTableIterator(){
            outer = odPairs.keySet().iterator();
            inner = new MapValueIterator<>(odPairs.get(outer.next()));
        }
        
        @Override
        public boolean hasNext(){
            return inner.hasNext() || outer.hasNext();
        }

        @Override
        public ODPair next(){
            if(inner.hasNext())
            {
                return inner.next();
            }
            else if(outer.hasNext())
            {
                inner = new MapValueIterator<>(odPairs.get(outer.next()));
                return inner.next();
            }
            else
            {
                return null;
            }
        }
        @Override
        public void remove(){
            throw new RuntimeException("Unsupported");
        }
    }
}

/**
 * Additional class written to easily iterate over a map
 * @todo: understand how it works (low priority)
 * @author vp6258
 * @param <V> 
 */
class MapValueIterable<V> implements Iterable<V>{
    private final Map<? extends Object, V> map;
    public MapValueIterable(Map<? extends Object, V> map){
        this.map = map;
    }
    @Override
    public Iterator<V> iterator(){
        return new MapValueIterator<>(map);
    }
}

// iterates over Objects and returns V associated to the object
class MapValueIterator<V> implements Iterator<V>{
    private final Map<? extends Object, V> map;
    private final Iterator mapIterator;
    public MapValueIterator(Map<? extends Object, V> map){
        this.map = map;
        mapIterator = map.keySet().iterator();
    }
    @Override
    public boolean hasNext(){
        return mapIterator.hasNext();
    }
    @Override
    public V next(){
        return map.get(mapIterator.next());
    }
    @Override
    public void remove(){
        throw new RuntimeException("Unsupported");
    }
}
