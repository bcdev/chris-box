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
package org.esa.beam.chris.util.math;

import junit.framework.TestCase;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class RegressionTest extends TestCase {

    public void testRegression() {
        final UnivariateFunctionSequence functionSequence = new LegendrePolynomials();

        final double[][] x = new double[5][3];
        for (int i = 0; i < 5; ++i) {
            functionSequence.calculate(i, x[i]);
        }
        final double[][] xx = new double[3][5];
        for (int i = 0; i < 3; ++i) {
            final double[] xxi = xx[i];
            for (int j = 0; j < 5; ++j) {
                xxi[j] = x[j][i];
            }
        }

        final double[] y = {0.0, 3.0, 12.0, 27.0, 48.0};
        double[] z = new Regression(xx).fit(y, new double[5], new double[3], new double[3]);

        assertEquals(y[0], z[0], 1.0E-10);
        assertEquals(y[1], z[1], 1.0E-10);
        assertEquals(y[2], z[2], 1.0E-10);
        assertEquals(y[3], z[3], 1.0E-10);
        assertEquals(y[4], z[4], 1.0E-10);
    }
}
