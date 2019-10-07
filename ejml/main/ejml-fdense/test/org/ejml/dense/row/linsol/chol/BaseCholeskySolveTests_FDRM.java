/*
 * Copyright (c) 2009-2018, Peter Abeles. All Rights Reserved.
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

package org.ejml.dense.row.linsol.chol;

import org.ejml.EjmlUnitTests;
import org.ejml.LinearSolverSafe;
import org.ejml.UtilEjml;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;
import org.ejml.dense.row.RandomMatrices_FDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public abstract class BaseCholeskySolveTests_FDRM {

    Random rand = new Random(0x45);

    public void standardTests() {

        solve_dimensionCheck();
        testSolve();
        testInvert();
        testQuality();
        testQuality_scale();
    }

    public abstract LinearSolverDense<FMatrixRMaj> createSolver();

    public LinearSolverDense<FMatrixRMaj> createSafeSolver() {
        LinearSolverDense<FMatrixRMaj> solver = createSolver();
        return new LinearSolverSafe<FMatrixRMaj>(solver);
    }

    @Test
    public void setA_dimensionCheck() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        try {
            FMatrixRMaj A = RandomMatrices_FDRM.rectangle(4,5,rand);
            assertTrue(solver.setA(A));
            fail("Should have thrown an exception");
        } catch( RuntimeException ignore ) {}
    }

    @Test
    public void solve_dimensionCheck() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        FMatrixRMaj A = RandomMatrices_FDRM.symmetricPosDef(4, rand);
        assertTrue(solver.setA(A));

        {
            FMatrixRMaj x = RandomMatrices_FDRM.rectangle(4,3,rand);
            FMatrixRMaj b = RandomMatrices_FDRM.rectangle(4,2,rand);
            solver.solve(b,x);
            assertEquals(x.numCols,b.numCols);
        }

        {
            FMatrixRMaj x = RandomMatrices_FDRM.rectangle(5,2,rand);
            FMatrixRMaj b = RandomMatrices_FDRM.rectangle(4,2,rand);
            solver.solve(b,x);
            assertEquals(x.numRows,b.numRows);
        }

        try {
            FMatrixRMaj x = RandomMatrices_FDRM.rectangle(5,2,rand);
            FMatrixRMaj b = RandomMatrices_FDRM.rectangle(5,2,rand);
            solver.solve(b,x);
            fail("Should have thrown an exception");
        } catch( RuntimeException ignore ) {}
    }

    @Test
    public void testSolve() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        FMatrixRMaj A = new FMatrixRMaj(3,3, true, 1, 2, 4, 2, 13, 23, 4, 23, 90);
        FMatrixRMaj b = new FMatrixRMaj(3,1, true, 17, 97, 320);
        FMatrixRMaj x = RandomMatrices_FDRM.rectangle(3,1,rand);
        FMatrixRMaj A_orig = A.copy();
        FMatrixRMaj B_orig = b.copy();

        assertTrue(solver.setA(A));
        solver.solve(b,x);

        // see if the input got modified
        EjmlUnitTests.assertEquals(A,A_orig,UtilEjml.TEST_F32_SQ);
        EjmlUnitTests.assertEquals(b,B_orig,UtilEjml.TEST_F32_SQ);

        FMatrixRMaj x_expected = new FMatrixRMaj(3,1, true, 1, 2, 3);

        EjmlUnitTests.assertEquals(x_expected,x,UtilEjml.TEST_F32_SQ);
    }

    @Test
    public void testInvert() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        FMatrixRMaj A = new FMatrixRMaj(3,3, true, 1, 2, 4, 2, 13, 23, 4, 23, 90);
        FMatrixRMaj found = new FMatrixRMaj(A.numRows,A.numCols);

        assertTrue(solver.setA(A));
        solver.invert(found);

        FMatrixRMaj A_inv = new FMatrixRMaj(3,3, true, 1.453515f, -0.199546f, -0.013605f, -0.199546f, 0.167800f, -0.034014f, -0.013605f, -0.034014f, 0.020408f);

        EjmlUnitTests.assertEquals(A_inv,found,UtilEjml.TEST_F32_SQ);
    }

    @Test
    public void testQuality() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        FMatrixRMaj A = CommonOps_FDRM.diag(3,2,1);
        FMatrixRMaj B = CommonOps_FDRM.diag(3,2,0.001f);

        assertTrue(solver.setA(A));
        float qualityA = (float)solver.quality();

        assertTrue(solver.setA(B));
        float qualityB = (float)solver.quality();

        assertTrue(qualityB < qualityA);
    }

    @Test
    public void testQuality_scale() {

        LinearSolverDense<FMatrixRMaj> solver = createSafeSolver();

        FMatrixRMaj A = CommonOps_FDRM.diag(3,2,1);
        FMatrixRMaj B = A.copy();
        CommonOps_FDRM.scale(0.001f,B);

        assertTrue(solver.setA(A));
        float qualityA = (float)solver.quality();

        assertTrue(solver.setA(B));
        float qualityB = (float)solver.quality();

        assertEquals(qualityB,qualityA, UtilEjml.TEST_F32);
    }
}
