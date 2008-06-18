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

    public void testBracket() {
        final Bracket bracket = new Bracket();
        final boolean success = Min.bracket(new Cosine(), 0.0, 0.5, bracket, 10);

        assertTrue(success);

        assertTrue(bracket.lowerX < bracket.minimumX);
        assertTrue(bracket.upperX > bracket.minimumX);

        assertTrue(bracket.lowerF > bracket.minimumF);
        assertTrue(bracket.upperF > bracket.minimumF);
    }

    public void testBrent() {
        final Cosine function = new Cosine();
        final Bracket bracket = new Bracket();

        boolean success;
        success = Min.bracket(function, 0.0, 0.5, bracket, 10);
        assertTrue(success);

        success = Min.brent(function, bracket, 0.001, 0.001, 100);
        assertTrue(success);

        assertEquals(Math.PI, bracket.minimumX, 0.001);
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
