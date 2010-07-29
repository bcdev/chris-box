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
 * Tests for class {@link org.esa.beam.chris.util.math.internal.Multimin}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class MultiminTest extends TestCase {
    private static final double ACCURACY_GOAL = 1.0E-8;

    public void testPowellCigar() {
        final double[] x = new double[]{1.0, 1.0, 1.0};
        final double[][] e = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}};

        final boolean success = Multimin.powell(new Functions.Cigar(), x, e, ACCURACY_GOAL, 200);
        assertTrue(success);

        assertEquals(0.0, x[0], ACCURACY_GOAL);
        assertEquals(0.0, x[1], ACCURACY_GOAL);
        assertEquals(0.0, x[2], ACCURACY_GOAL);
    }

    public void testPowellRosenbrock() {
        final double[] x = new double[]{0.0, 0.0, 0.0};
        final double[][] e = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}};

        final boolean success = Multimin.powell(new Functions.Rosenbrock(), x, e, ACCURACY_GOAL, 200);
        assertTrue(success);

        assertEquals(1.0, x[0], ACCURACY_GOAL);
        assertEquals(1.0, x[1], ACCURACY_GOAL);
        assertEquals(1.0, x[2], ACCURACY_GOAL);
    }
}
