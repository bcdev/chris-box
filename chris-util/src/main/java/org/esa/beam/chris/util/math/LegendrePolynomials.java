/*
 * Copyright (C) 2002-2007 by Brockmann Consult
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

/**
 * Class for calculating Legendre polynomials.
 *
 * @author Ralf Quast
 * @version $Revision: 2402 $ $Date: 2008-07-02 21:14:58 +0200 (Wed, 02 Jul 2008) $
 */
final public class LegendrePolynomials implements UnivariateFunctionSequence {

    public void calculate(double x, double[] y) {
        if (y.length > 0) {
            double l1 = 1.0;
            double l2 = 0.0;
            double l3;

            y[0] = 1.0;

            for (int i = 1; i < y.length; ++i) {
                l3 = l2;
                l2 = l1;
                l1 = ((2 * i - 1) * x * l2 - (i - 1) * l3) / i;

                y[i] = l1;
            }
        }
    }
}
