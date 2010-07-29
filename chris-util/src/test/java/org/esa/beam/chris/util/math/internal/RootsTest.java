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
 * Tests for class {@link Roots}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class RootsTest extends TestCase {

    public void testBrent() throws Exception {
        final UnivariateFunction f = new Functions.Cos();
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 2.0);
        final boolean success = Roots.brent(f, bracket, 100);

        assertTrue(success);
        assertEquals(Math.PI / 2.0, bracket.root, 0.0);
    }

    public void testBrentWithRootAtBracketingIntervalLowerLimit() throws Exception {
        final UnivariateFunction f = new Functions.Sin();
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 1.0);
        final boolean success = Roots.brent(f, bracket, 100);

        assertTrue(success);
        assertEquals(0.0, bracket.root, 0.0);
    }

    public void testBrentWithRootAtBracketingIntervalUpperLimit() throws Exception {
        final UnivariateFunction f = new Functions.Sin();
        final Roots.Bracket bracket = new Roots.Bracket(-1.0, 0.0);
        final boolean success = Roots.brent(f, bracket, 100);

        assertTrue(success);
        assertEquals(0.0, bracket.root, 0.0);
    }

    public void testBrentWithRootNotInBracketingInterval() throws Exception {
        final UnivariateFunction f = new Functions.Cos();

        Roots.Bracket bracket;
        bracket = new Roots.Bracket(0.0, 1.0);

        // the bracketing interval does not bracket a root
        assertFalse(bracket.isBracket(f));

        boolean success;
        success = Roots.brent(f, bracket, 100);
        // the bracketing interval does not bracket a root, but Brent's
        // algorithm returns the value which is closest to the root
        assertTrue(success);
        assertEquals(1.0, bracket.root, 0.0);


        bracket = new Roots.Bracket(Math.PI - 1.0, Math.PI);

        // the bracketing interval does not bracket a root
        assertFalse(bracket.isBracket(f));

        success = Roots.brent(f, bracket, 100);
        // the bracketing interval does not bracket a root, but Brent's
        // algorithm returns the value which is closest to the root
        assertTrue(success);
        assertEquals(Math.PI - 1.0, bracket.root, 0.0);
    }
}
