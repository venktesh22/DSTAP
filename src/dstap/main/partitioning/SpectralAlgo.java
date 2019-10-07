/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dstap.main.partitioning;

//import Jama.EigenvalueDecomposition;
//import Jama.Matrix;
import dstap.links.Link;
import dstap.nodes.Node;
import java.io.File;
import java.util.List;
import org.ejml.data.Complex_F64;
import org.ejml.data.Matrix;
import org.ejml.simple.SimpleBase;
import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.Solve;
import org.jblas.Eigen;

/**
 *
 * @author vp6258
 */
public class SpectralAlgo extends PartitioningAlgo{
    private DoubleMatrix weightedAdjacencyMatrix;
    private DoubleMatrix diagonalDegreeMatrix;
    
    public SpectralAlgo(int noOfClusters, String netName) {
        super(noOfClusters, netName);
    }
    
    public void runAlgorithm(){
        ///=================Implementation using EJML matrices======================//
//        SimpleMatrix adjMatrix = new SimpleMatrix(network.getNodes().size(), network.getNodes().size());
//        SimpleMatrix diaMatrix = new SimpleMatrix(network.getNodes().size(), network.getNodes().size());
//        int minNode= network.getFirstThruNodeID();
//        for(Link l: network.getLinks()){
//           int sourceNodeID = l.getSource().getId();
//           int destNodeID = l.getDest().getId();
//           adjMatrix.set(sourceNodeID-minNode, destNodeID-minNode, this.network.UELinkFlow.get(l));
//        }
//        SimpleMatrix symAdjMat1 = adjMatrix.plus(adjMatrix.transpose()); //make the adjacency matrix symmetric
//        for(int i=0;i< symAdjMat1.numRows();i++){
//           double sumRow=0.0;
//           for(int j=0;j<symAdjMat1.numCols();j++)
//               sumRow+= symAdjMat1.get(i,j);
//           diaMatrix.set(i, i,sumRow);
//       }
//       //generate Laplacian matrix and normalize it
//       SimpleMatrix laplacianMatrix = diaMatrix.minus(symAdjMat1);
//       SimpleMatrix degMatPowerMinusOneHalf= diaMatrix.copy();
//       for(int i=0;i< diaMatrix.numRows(); i++){
//           degMatPowerMinusOneHalf.set(i, i, Math.pow(degMatPowerMinusOneHalf.get(i,i),-0.5));
//       }
//       SimpleMatrix p = laplacianMatrix.mult(degMatPowerMinusOneHalf);
//       SimpleMatrix normalizedLaplacian = degMatPowerMinusOneHalf.mult(p);
//       
////       normalizedLaplacian.print();
//       SimpleEVD mat = normalizedLaplacian.eig(); //doesn't always work. Known issue in current code. read here: https://github.com/lessthanoptimal/ejml/issues/35
//       List<Complex_F64> eigenValues = mat.getEigenvalues();
////       SimpleBase eigenVectors = mat.getEigenVector(1);
//        System.out.println("EigenValues="+eigenValues);
//        System.out.println("HAHA");
        
        ///=====================Implementation using JAMA matrices===============//
//        Matrix adjMatrix = new Matrix(network.getNodes().size(), network.getNodes().size());
//        Matrix diaMatrix = new Matrix(network.getNodes().size(), network.getNodes().size());
//        int minNode= network.getFirstThruNodeID();
//        for(Link l: network.getLinks()){
//           int sourceNodeID = l.getSource().getId();
//           int destNodeID = l.getDest().getId();
//           adjMatrix.set(sourceNodeID-minNode, destNodeID-minNode, this.network.UELinkFlow.get(l));
//       }
//        Matrix symAdjMat1 = adjMatrix.plus(adjMatrix.transpose()); //make the adjacency matrix symmetric
//        for(int i=0;i< symAdjMat1.getRowDimension();i++){
//           double sumRow=0.0;
//           for(int j=0;j<symAdjMat1.getColumnDimension();j++)
//               sumRow+= symAdjMat1.get(i,j);
//           diaMatrix.set(i, i,sumRow);
//       }
//       //generate Laplacian matrix and normalize it
//       Matrix laplacianMatrix = diaMatrix.minus(symAdjMat1);
//       Matrix degMatPowerMinusOneHalf= diaMatrix.copy();
//       for(int i=0;i< diaMatrix.getRowDimension(); i++){
//           degMatPowerMinusOneHalf.set(i, i, Math.pow(degMatPowerMinusOneHalf.get(i,i),-0.5));
//       }
//       Matrix p = laplacianMatrix.times(degMatPowerMinusOneHalf);
//       Matrix normalizedLaplacian = degMatPowerMinusOneHalf.times(p);
//       
//       EigenvalueDecomposition mat = new EigenvalueDecomposition(normalizedLaplacian);
//       Matrix eigenValues = mat.getD();
//       Matrix eigenVectors = mat.getV();
//        System.out.println("EigenValues=");
//        eigenValues.print(0, this.network.getNodes().size());
//        System.out.println("HAHA");
////        prettyPrintMatrix(eigenValueAndVectors[1]);//eigenvalues
////        
////       DoubleMatrix secondSmallestEigenVector =  eigenValueAndVectors[0].getColumn(1); //eigenvalues are automatically sorted
////        System.out.println("Eigenvector corresponding to second largest eigenvalue is");
////        prettyPrintMatrix(secondSmallestEigenVector);
       
        
        //======================Implementation using jblas matrices===============//
        
        //INitialize matrices
       weightedAdjacencyMatrix = DoubleMatrix.zeros(network.getNodes().size(), network.getNodes().size());
       diagonalDegreeMatrix = DoubleMatrix.zeros(network.getNodes().size(), network.getNodes().size());
       
       //Find minimum node ID
       int minNodeID= network.getFirstThruNodeID(); // we assume all node IDs are in increment of 1 for properly defining adjacency matrices
       
       //generate adjacency and degree matrix
       for(Link l: network.getLinks()){
           int sourceNodeID = l.getSource().getId();
           int destNodeID = l.getDest().getId();
           if(this.network.UELinkFlow.get(l)==0){
               //this link has zero flow and can lead to dangling partitions at the edge of the network
               //do not consider it as part of partitioning
               continue;
           }
           weightedAdjacencyMatrix.put(sourceNodeID-minNodeID, destNodeID-minNodeID, this.network.UELinkFlow.get(l)); //LINKFLOW);
//           diagonalDegreeMatrix.put(sourceNodeID-minNodeID, sourceNodeID-minNodeID,
//                   diagonalDegreeMatrix.get(sourceNodeID-minNodeID, sourceNodeID-minNodeID)+1);
//           diagonalDegreeMatrix.put(destNodeID-minNodeID, destNodeID-minNodeID,
//                   diagonalDegreeMatrix.get(destNodeID-minNodeID, destNodeID-minNodeID)+1);
       }
       
        
        DoubleMatrix symAdjMat = weightedAdjacencyMatrix.add(weightedAdjacencyMatrix.transpose()); //make the adjacency matrix symmetric and thus make the graph undirected
        for(int i=0;i< symAdjMat.rows;i++){
           double sumRow=0.0;
           for(int j=0;j<symAdjMat.columns;j++)
               sumRow+= symAdjMat.get(i,j);
           diagonalDegreeMatrix.put(i, i,sumRow);
       }
       //generate Laplacian matrix and normalize it
       DoubleMatrix laplacianMatrix = diagonalDegreeMatrix.sub(symAdjMat);
       DoubleMatrix degMatPowerMinusOneHalf= diagonalDegreeMatrix.dup();
       for(int i=0;i< diagonalDegreeMatrix.rows; i++){
           degMatPowerMinusOneHalf.put(i, i, Math.pow(degMatPowerMinusOneHalf.get(i,i),-0.5));
       }
//       DoubleMatrix Vinverse = Solve.solve(diagonalDegreeMatrix, DoubleMatrix.eye(diagonalDegreeMatrix.rows));
//       DoubleMatrix degMatPowerMinusOneHalf= MatrixFunctions.pow(diagonalDegreeMatrix, -0.5);
//        prettyPrintMatrix(degMatPowerMinusOneHalf);

////       double[][] mul1 = multiply(laplacianMatrix.toArray2(), degMatPowerMinusOneHalf.toArray2());
////       double[][] mull2 = multiply(degMatPowerMinusOneHalf.toArray2(), mul1);
//       
       DoubleMatrix p = laplacianMatrix.mmul(degMatPowerMinusOneHalf);
       DoubleMatrix normalizedLaplacian = degMatPowerMinusOneHalf.mmul(p);
       
       
        prettyPrintMatrix(normalizedLaplacian);
        System.out.println("\n\n");

       //find its eigenvalues and eigenvectors
       DoubleMatrix[] eigenValueAndVectors = Eigen.symmetricEigenvectors(normalizedLaplacian);
        prettyPrintMatrix(eigenValueAndVectors[1]);//eigenvalues
        
       DoubleMatrix secondSmallestEigenVector =  eigenValueAndVectors[0].getColumn(1); //eigenvalues are automatically sorted
        System.out.println("Eigenvector corresponding to second largest eigenvalue is");
        prettyPrintMatrix(secondSmallestEigenVector);
       //populate the associatedClusterLabel
       
       //put all negative values in one partition and positive value in other
       for(int i=0; i< secondSmallestEigenVector.rows; i++){
           int nodeId= network.firstThruNodeID+ i;
           Node n= network.getNodesByID().get(nodeId);
           if(secondSmallestEigenVector.get(i, 0)<0){
               this.associatedClusterLabel.put(n, 0);
//               this.
//               clusterAssociation.put(0, boundaryNodes)
           }
           else{
               this.associatedClusterLabel.put(n, 1);
           }
       }
        System.out.println("Cluster labels"+associatedClusterLabel);
//       
//       //--------------------Identify boundary nodes--------------------
        findBoundaryNodes();
        
        addClusterLabelToCentroids();
    }
    
    @Override
    protected void setOutputFolderName(){
        String epoch= Integer.toString((int)(System.currentTimeMillis()/1000.0));
        epoch = "Spectral_"+epoch;
        File dir = new File("Networks/"+netName+"/Inputs/"+epoch);
    
        // attempt to create the directory here
        boolean successful = dir.mkdir();
        if (!successful){
            System.out.println("failed trying to create the directory");
        }
        this.partitionOutputFolderName= "Networks/"+netName+"/Inputs/"+epoch;
    }
    
    private void prettyPrintMatrix(DoubleMatrix d){
        System.out.println("\n");
        for(int i=0; i<d.rows; i++){
            for(int j=0;j<d.columns;j++){
                System.out.print(Math.round(d.get(i, j)*10000)/10000.0 + "\t");
            }
            System.out.print("\n");
        }
    }
    
    public static double[][] multiply(double[][] A, double[][] B) {
        double[][] result = new double[A.length][B[0].length];
        int x = A.length, y = A[0].length, z = B[0].length;
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                if (A[i][j] != 0) {
                    for (int k = 0; k < z; k++) {
                            result[i][j] += A[i][j] * B[j][k];
                    }
                }
            }
        }
        return result;
    }
    
}
