/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.util.math.internal;

import junit.framework.TestCase;

/**
 * Tests for class {@link LocalRegressionSmoother}.
 *
 * @author Ralf Quast
 * @version $Revision: 2572 $ $Date: 2008-07-10 11:01:58 +0200 (Thu, 10 Jul 2008) $
 */
public class LocalRegressionSmootherTest extends TestCase {

    public void testConstructors() {
        try {
            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), -1, 2);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("!(degree >= 0)", e.getMessage());
        }

        try {
            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 2, 4);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("!(span > degree + 2)", e.getMessage());
        }

        try {
            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 2, 7, -1);
            fail();
        } catch (Exception e) {
            assertEquals("!(iter >= 0)", e.getMessage());
        }
    }

    public void testFitHorizontalLine() {
        final UnivariateFunctionSequence functionSequence = new LegendrePolynomials();

        final double[][] p = new double[5][3];
        for (int i = 0; i < 5; ++i) {
            functionSequence.calculate(i, p[i]);
        }

        final double[] w = new double[]{0.5, 1.0, 1.0, 1.0, 0.5};
        final double[] c = new double[]{0.0, 0.0, 0.0};

        LocalRegressionSmoother.fastFit(new double[]{1.0, 1.0, 1.0, 1.0, 1.0}, 0, w, c, p);
        assertEquals(1.0, c[0], 1.0E-10);
        assertEquals(0.0, c[1], 1.0E-10);
        assertEquals(0.0, c[2], 1.0E-10);
    }

    public void testFitInclinedLines() {
        final UnivariateFunctionSequence functionSequence = new LegendrePolynomials();

        final double[][] p = new double[5][3];
        for (int i = 0; i < 5; ++i) {
            functionSequence.calculate(i, p[i]);
        }

        final double[] w = new double[]{0.5, 1.0, 1.0, 1.0, 0.5};
        final double[] c = new double[]{0.0, 0.0, 0.0};

        LocalRegressionSmoother.fastFit(new double[]{0.0, 1.0, 2.0, 3.0, 4.0}, 0, w, c, p);
        assertEquals(0.0, c[0], 1.0E-10);
        assertEquals(1.0, c[1], 1.0E-10);
        assertEquals(0.0, c[2], 1.0E-10);

        LocalRegressionSmoother.safeFit(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, 0, w, c, p);
        assertEquals(1.0, c[0], 1.0E-10);
        assertEquals(1.0, c[1], 1.0E-10);
        assertEquals(0.0, c[2], 1.0E-10);
    }

    public void testFitParabolas() {
        final UnivariateFunctionSequence functionSequence = new LegendrePolynomials();

        final double[][] p = new double[5][3];
        for (int i = 0; i < 5; ++i) {
            functionSequence.calculate(i, p[i]);
        }

        final double[] w = new double[]{0.5, 1.0, 1.0, 1.0, 0.5};
        final double[] c = new double[]{0.0, 0.0, 0.0};

        LocalRegressionSmoother.fastFit(new double[]{0.0, 3.0, 12.0, 27.0, 48.0}, 0, w, c, p);
        assertEquals(1.0, c[0], 1.0E-10);
        assertEquals(0.0, c[1], 1.0E-10);
        assertEquals(2.0, c[2], 1.0E-10);

        LocalRegressionSmoother.safeFit(new double[]{0.0, 4.0, 14.0, 30.0, 52.0}, 0, w, c, p);
        assertEquals(1.0, c[0], 1.0E-10);
        assertEquals(1.0, c[1], 1.0E-10);
        assertEquals(2.0, c[2], 1.0E-10);
    }

    public void testSmoothArgs() {
        try {
            final double[] z = new double[7];

            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7).smooth(null, z);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            final double[] y = new double[7];

            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7).smooth(y, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            final double[] y = new double[7];
            final double[] z = new double[8];

            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7).smooth(y, z);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("!(y.length == z.length)", e.getMessage());
        }

        try {
            final double[] y = new double[6];
            final double[] z = new double[6];

            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7).smooth(y, z);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("!(y.length >= span)", e.getMessage());
        }

        try {
            final double[] y = new double[7];

            new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7).smooth(y, y);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("y == z", e.getMessage());
        }
    }

    public void testSmoothHorizontalLine() {
        final double[] y = new double[10];
        final double[] z = new double[10];

        y[0] = 1.0;
        y[1] = 1.0;
        y[2] = 1.0;
        y[3] = 1.0;
        y[4] = 7.0; // outlier
        y[5] = 1.0;
        y[6] = 1.0;
        y[7] = 1.0;
        y[8] = 1.0;
        y[9] = 1.0;

        new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 7, 1).smooth(y, z);

        assertEquals(1.0, z[0], 1.0E-10);
        assertEquals(1.0, z[4], 1.0E-10);
        assertEquals(1.0, z[5], 1.0E-10);
        assertEquals(1.0, z[9], 1.0E-10);
    }

    public void testSmoothInclinedLine() {
        final double[] y = new double[10];
        final double[] z = new double[10];

        y[0] = 0.0;
        y[1] = 1.0;
        y[2] = 2.0;
        y[3] = 3.0;
        y[4] = 0.0; // outlier
        y[5] = 5.0;
        y[6] = 6.0;
        y[7] = 7.0;
        y[8] = 8.0;
        y[9] = 9.0;

        new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 1, 7, 1).smooth(y, z);

        assertEquals(0.0, z[0], 1.0E-10);
        assertEquals(4.0, z[4], 1.0E-10);
        assertEquals(5.0, z[5], 1.0E-10);
        assertEquals(9.0, z[9], 1.0E-10);
    }

    public void testSmoothParabola() {
        final double[] y = new double[10];
        final double[] z = new double[10];

        y[0] = 0.0;
        y[1] = 1.0;
        y[2] = 4.0;
        y[3] = 9.0;
        y[4] = 77.0; // outlier
        y[5] = 25.0;
        y[6] = 36.0;
        y[7] = 49.0;
        y[8] = 64.0;
        y[9] = 81.0;

        new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 2, 7, 1).smooth(y, z);

        assertEquals(0.0, z[0], 1.0E-10);
        assertEquals(16.0, z[4], 1.0E-10);
        assertEquals(25.0, z[5], 1.0E-10);
        assertEquals(81.0, z[9], 1.0E-10);
    }
}
