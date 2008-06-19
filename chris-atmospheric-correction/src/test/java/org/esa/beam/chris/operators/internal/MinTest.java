/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators.internal;

import junit.framework.TestCase;
import org.esa.beam.chris.operators.internal.Min.Bracket;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class MinTest extends TestCase {
    private static final double ACCURACY_GOAL = 1.0E-6;

    public void testBrackCos() {
        final Bracket bracket = Min.brack(new TestFunctions.Cos(), 0.0, 0.5, new Bracket());

        assertTrue(bracket.lowerX < bracket.innerX);
        assertTrue(bracket.upperX > bracket.innerX);

        assertTrue(bracket.lowerF > bracket.innerF);
        assertTrue(bracket.upperF > bracket.innerF);
    }

    public void testBrackSin() {
        final Bracket bracket = Min.brack(new TestFunctions.Sin(), 0.0, 0.5, new Bracket());

        assertTrue(bracket.lowerX < bracket.innerX);
        assertTrue(bracket.upperX > bracket.innerX);

        assertTrue(bracket.lowerF > bracket.innerF);
        assertTrue(bracket.upperF > bracket.innerF);
    }

    public void testBrentCos() {
        final UnivariateFunction function = new TestFunctions.Cos();
        final Bracket bracket = new Bracket(2.0, 5.0, function);

        final Boolean success = Min.brent(function, bracket, ACCURACY_GOAL);
        assertTrue(success);

        assertEquals(Math.PI, bracket.innerX, ACCURACY_GOAL);
        assertEquals(-1.0, bracket.innerF, ACCURACY_GOAL);
    }

    public void testBrentSin() {
        final UnivariateFunction function = new TestFunctions.Sin();
        final Bracket bracket = new Bracket(3.0, 6.0, function);

        final Boolean success = Min.brent(function, bracket, ACCURACY_GOAL);
        assertTrue(success);

        assertEquals(1.5 * Math.PI, bracket.innerX, ACCURACY_GOAL);
        assertEquals(-1.0, bracket.innerF, ACCURACY_GOAL);
    }
}
