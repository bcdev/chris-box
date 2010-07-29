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
import org.esa.beam.chris.util.math.internal.Min.Bracket;

/**
 * Tests for class {@link Min}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class MinTest extends TestCase {
    private static final double ACCURACY_GOAL = 1.0E-6;

    public void testBrackCos() {
        final Bracket bracket = Min.brack(new Functions.Cos(), 0.0, 0.5, new Bracket());

        assertTrue(bracket.lowerX < bracket.minimumX);
        assertTrue(bracket.upperX > bracket.minimumX);

        assertTrue(bracket.lowerF > bracket.minimumF);
        assertTrue(bracket.upperF > bracket.minimumF);
    }

    public void testBrackSin() {
        final Bracket bracket = Min.brack(new Functions.Sin(), 0.0, 0.5, new Bracket());

        assertTrue(bracket.lowerX < bracket.minimumX);
        assertTrue(bracket.upperX > bracket.minimumX);

        assertTrue(bracket.lowerF > bracket.minimumF);
        assertTrue(bracket.upperF > bracket.minimumF);
    }

    public void testBrentCos() {
        final UnivariateFunction function = new Functions.Cos();
        final Bracket bracket = new Bracket(2.0, 5.0, function);

        final Boolean success = Min.brent(function, bracket, ACCURACY_GOAL);
        assertTrue(success);

        assertEquals(Math.PI, bracket.minimumX, ACCURACY_GOAL);
        assertEquals(-1.0, bracket.minimumF, ACCURACY_GOAL);
    }

    public void testBrentSin() {
        final UnivariateFunction function = new Functions.Sin();
        final Bracket bracket = new Bracket(3.0, 6.0, function);

        final Boolean success = Min.brent(function, bracket, ACCURACY_GOAL);
        assertTrue(success);

        assertEquals(1.5 * Math.PI, bracket.minimumX, ACCURACY_GOAL);
        assertEquals(-1.0, bracket.minimumF, ACCURACY_GOAL);
    }
}
