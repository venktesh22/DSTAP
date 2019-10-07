/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.dense.row.decomposition;

import org.ejml.UtilEjml;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;
import org.ejml.dense.row.MatrixFeatures_FDRM;
import org.ejml.dense.row.RandomMatrices_FDRM;
import org.ejml.dense.row.misc.UnrolledInverseFromMinor_FDRM;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestTriangularSolver_FDRM {

    Random rand = new Random(0xff);


    @Test
    public void invert_inplace() {
        FMatrixRMaj L = createRandomLowerTriangular();

        FMatrixRMaj L_inv = L.copy();

        TriangularSolver_FDRM.invertLower(L_inv.data,L.numRows);

        FMatrixRMaj I = new FMatrixRMaj(L.numRows,L.numCols);

        CommonOps_FDRM.mult(L,L_inv,I);

        assertTrue(MatrixFeatures_FDRM.isIdentity(I,UtilEjml.TEST_F32));
    }

    @Test
    public void invert() {
        FMatrixRMaj L = createRandomLowerTriangular();

        FMatrixRMaj L_inv = L.copy();

        TriangularSolver_FDRM.invertLower(L.data,L_inv.data,L.numRows);

        FMatrixRMaj I = new FMatrixRMaj(L.numRows,L.numCols);

        CommonOps_FDRM.mult(L,L_inv,I);

        assertTrue(MatrixFeatures_FDRM.isIdentity(I, UtilEjml.TEST_F32));
    }

    @Test
    public void solveL_vector() {
        FMatrixRMaj L = createRandomLowerTriangular();

        FMatrixRMaj L_inv = L.copy();
        UnrolledInverseFromMinor_FDRM.inv(L_inv,L_inv);

        FMatrixRMaj B = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj expected = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj found = B.copy();

        TriangularSolver_FDRM.solveL(L.data,found.data,3);
        CommonOps_FDRM.mult(L_inv,B,expected);


        assertTrue(MatrixFeatures_FDRM.isIdentical(expected,found,UtilEjml.TEST_F32));
    }

    private FMatrixRMaj createRandomLowerTriangular() {
        FMatrixRMaj L = RandomMatrices_FDRM.rectangle(3,3,rand);
        for( int i = 0; i < L.numRows; i++ ) {
            for( int j = i+1; j < L.numCols; j++ ) {
                L.set(i,j,0);
            }
        }
        return L;
    }

    @Test
    public void solveL_matrix() {
        FMatrixRMaj L = createRandomLowerTriangular();

        FMatrixRMaj L_inv = L.copy();
        UnrolledInverseFromMinor_FDRM.inv(L_inv,L_inv);

        FMatrixRMaj B = RandomMatrices_FDRM.rectangle(3,4,rand);
        FMatrixRMaj expected = RandomMatrices_FDRM.rectangle(3,4,rand);
        FMatrixRMaj found = B.copy();

        TriangularSolver_FDRM.solveL(L.data,found.data,3,4);
        CommonOps_FDRM.mult(L_inv,B,expected);

        assertTrue(MatrixFeatures_FDRM.isIdentical(expected,found,UtilEjml.TEST_F32));
    }

    @Test
    public void solveTranL() {
        FMatrixRMaj L = createRandomLowerTriangular();

        FMatrixRMaj B = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj expected = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj found = B.copy();

        TriangularSolver_FDRM.solveTranL(L.data,found.data,3);

        CommonOps_FDRM.transpose(L);
        FMatrixRMaj L_inv = L.copy();
        UnrolledInverseFromMinor_FDRM.inv(L_inv,L_inv);
        CommonOps_FDRM.mult(L_inv,B,expected);

        assertTrue(MatrixFeatures_FDRM.isIdentical(expected,found,UtilEjml.TEST_F32));
    }

    @Test
    public void solveU() {
        FMatrixRMaj U = RandomMatrices_FDRM.rectangle(3,3,rand);
        for( int i = 0; i < U.numRows; i++ ) {
            for( int j = 0; j < i; j++ ) {
                U.set(i,j,0);
            }
        }

        FMatrixRMaj U_inv = U.copy();
        UnrolledInverseFromMinor_FDRM.inv(U_inv,U_inv);

        FMatrixRMaj B = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj expected = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj found = B.copy();

        TriangularSolver_FDRM.solveU(U.data,found.data,3);
        CommonOps_FDRM.mult(U_inv,B,expected);

        assertTrue(MatrixFeatures_FDRM.isIdentical(expected,found,UtilEjml.TEST_F32));
    }

    @Test
    public void solveU_submatrix() {

        // create U and B.  Insert into a larger matrix
        FMatrixRMaj U_orig = RandomMatrices_FDRM.rectangle(3,3,rand);
        for( int i = 0; i < U_orig.numRows; i++ ) {
            for( int j = 0; j < i; j++ ) {
                U_orig.set(i,j,0);
            }
        }
        FMatrixRMaj U = new FMatrixRMaj(6,7);
        CommonOps_FDRM.insert(U_orig,U,2,3);
        
        
        FMatrixRMaj B_orig = RandomMatrices_FDRM.rectangle(3,2,rand);

        FMatrixRMaj B = new FMatrixRMaj(4,5);
        CommonOps_FDRM.insert(B_orig,B,1,2);
        
        // compute expected solution
        FMatrixRMaj U_inv = U_orig.copy();
        UnrolledInverseFromMinor_FDRM.inv(U_inv,U_inv);

        FMatrixRMaj expected = RandomMatrices_FDRM.rectangle(3,2,rand);

        int startU = 2*U.numCols+3;
        int strideU = U.numCols;
        int widthU = U_orig.numCols;
        int startB = 1*B.numCols+2;
        int strideB = B.numCols;
        int widthB = B_orig.numCols;
        TriangularSolver_FDRM.solveU(U.data,startU,strideU,widthU,B.data,startB,strideB,widthB);

        FMatrixRMaj found = CommonOps_FDRM.extract(B,1,4,2,4);
        CommonOps_FDRM.mult(U_inv,B_orig,expected);

        assertTrue(MatrixFeatures_FDRM.isIdentical(expected,found,UtilEjml.TEST_F32));
    }
}
