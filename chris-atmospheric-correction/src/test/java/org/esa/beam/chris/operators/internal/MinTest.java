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

    public void testFindBracket() {
        final Bracket bracket = new Bracket();
        final boolean success = Min.findBracket(new Cosine(), 0.0, 0.5, bracket, 10);

        assertTrue(success);

        assertTrue(bracket.leftX < bracket.centerX);
        assertTrue(bracket.rightX > bracket.centerX);

        assertTrue(bracket.leftF > bracket.centerF);
        assertTrue(bracket.rightF > bracket.centerF);
    }

    public void testBrent() {
        final Cosine function = new Cosine();
        final Bracket bracket = new Bracket();

        boolean success;
        success = Min.findBracket(function, 0.0, 0.5, bracket, 10);
        assertTrue(success);

        success = Min.brent(function, bracket, 100, 0.001, 0.001);
        assertTrue(success);

        assertEquals(Math.PI, bracket.centerX, 0.001);
    }


    private static class Cosine implements UnivariateFunction {

        @Override
        public double value(double x) {
            return Math.cos(x);
        }
    }

    private static class Sine implements UnivariateFunction {

        @Override
        public double value(double x) {
            return Math.sin(x);
        }
    }
}
