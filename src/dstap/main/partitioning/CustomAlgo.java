/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

import dstap.nodes.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * This class simply reads the custom partition file and generates DSTAP input
 * @author vp6258
 */
public class CustomAlgo extends PartitioningAlgo{

    private String partitionFileName;
    public CustomAlgo(int noOfClusters, String netName) {
        super(noOfClusters, netName);
    }
    
    public CustomAlgo(int noOfClusters, String netName, String fileName) throws FileNotFoundException {
        super(noOfClusters, netName);
        if(fileName.contains(".txt"))
            fileName= fileName.substring(0, fileName.length()-4);
        partitionFileName = fileName;
//        if(fileName.contains(".txt"))
//            partitionFileName = "Networks/"+netName+"/"+fileName;
//        else
//            partitionFileName = "Networks/"+netName+"/"+fileName+".txt";
        
    }
    
    public void readPartitionFile() throws FileNotFoundException{
        System.out.println("Reading link file....");
        Scanner filein = new Scanner(new File("Networks/"+netName+"/"+partitionFileName+".txt"));
        
        filein.nextLine(); //ignore first line
        while(filein.hasNext()){
            int nodeId = filein.nextInt();
            int clusterId= filein.nextInt();
            Node n= this.network.getNodesByID().get(nodeId);
            associatedClusterLabel.put(n, clusterId);
        }
        
        findBoundaryNodes();
        filein.close();
    }
    
    @Override
    protected void setOutputFolderName() {
        String epoch= Integer.toString((int)(System.currentTimeMillis()/1000.0));
        epoch = this.partitionFileName+"_"+epoch;
        File dir = new File("Networks/"+netName+"/Inputs/"+epoch);
    
        // attempt to create the directory here
        boolean successful = dir.mkdir();
        if (!successful){
            System.out.println("failed trying to create the directory");
        }
        this.partitionOutputFolderName= "Networks/"+netName+"/Inputs/"+epoch;
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runAlgorithm() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
