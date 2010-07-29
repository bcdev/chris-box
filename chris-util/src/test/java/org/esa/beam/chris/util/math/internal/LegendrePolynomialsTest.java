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
 * Tests for class {@link LegendrePolynomials}.
 *
 * @author Ralf Quast
 * @version $Revision: 2402 $ $Date: 2008-07-02 21:14:58 +0200 (Wed, 02 Jul 2008) $
 */
public class LegendrePolynomialsTest extends TestCase {

    public void testCalculationForNullArray() {
        try {
            new LegendrePolynomials().calculate(0.5, null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    public void testCalculateForEmptyArray() {
        new LegendrePolynomials().calculate(0.5, new double[0]);
    }

    public void testCalculate() {
        double[] y = new double[5];
        new LegendrePolynomials().calculate(0.5, y);

        assertEquals(1.0, y[0], 0.0);
        assertEquals(0.5, y[1], 0.0);
        assertEquals(-0.125, y[2], 0.0);
        assertEquals(-0.4375, y[3], 0.0);
        assertEquals(-0.2890625, y[4], 0.0);
    }

}
