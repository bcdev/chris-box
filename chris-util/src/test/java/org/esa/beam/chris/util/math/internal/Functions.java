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

/**
 * Test functions.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Functions {

    static class Cos implements UnivariateFunction {

        @Override
        public double value(double x) {
            return Math.cos(x);
        }
    }

    static class Sin implements UnivariateFunction {

        @Override
        public double value(double x) {
            return Math.sin(x);
        }
    }

    static class Cigar implements MultivariateFunction {

        @Override
        public double value(double... x) {
            double sum = 0.0;

            for (int i = 1; i < x.length; ++i) {
                sum += Pow.pow2(1000.0 * x[i]);
            }

            return x[0] * x[0] + sum;
        }
    }

    static class Rosenbrock implements MultivariateFunction {

        @Override
        public double value(double... x) {
            double sum = 0.0;

            for (int i = 0; i < x.length - 1; ++i) {
                sum += 100.0 * Pow.pow2(x[i] * x[i] - x[i + 1]) + Pow.pow2(x[i] - 1.0);
            }

            return sum;
        }
    }
}
