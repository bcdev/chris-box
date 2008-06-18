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

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class PowellMinimizerTest extends TestCase {
    private PowellMinimizer minimizer;

    @Override
    protected void setUp() throws Exception {
        minimizer = new PowellMinimizer(200);
    }

    public void testFindMinimum() {
        final double[] x = new double[]{2.5, 1.7};
        final double minimum = minimizer.findMinimum(new F(), x);

//        assertEquals(-7.0, minimum, 0.0);
//        assertEquals(3.0, x[0], 0.0);
//        assertEquals(2.0, x[1], 0.0);
    }

    private static class F implements MultivariateFunction {

        public double value(double... x) {
            return x[0] * (x[0] - x[1] - 4.0) + x[1] * (x[1]  - 1.0);
        }
    }
}
