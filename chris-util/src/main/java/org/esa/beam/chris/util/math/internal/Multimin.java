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
package org.esa.beam.chris.util.math.internal;

/**
 * Methods for finding the minimum of an multivariate function.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Multimin {

    /**
     * Finds the minimum of a multivariate function using Powell's method.
     * <p/>
     * Based on online material provided by John H. Mathews (Department of Mathematics,
     * California State Univ. Fullerton).
     *
     * @param f            the multivariate function.
     * @param pn           the initial guess of the minimum. On return contains the
     *                     minimum found. The length of this array must be equal to
     *                     the number of variables in {@code f}.
     * @param u            the initial direction set. On return contains the actual
     *                     direction set. The number of directions must be equal to
     *                     the number of variables in {@code f}.
     * @param accuracyGoal the prescribed accuracy goal.
     * @param maxIter      the maximum number of iterations.
     *
     * @return {@code true} on success.
     *
     * @throws IndexOutOfBoundsException if the dimensions of {@code pn} and {@code u}
     *                                   are not consistent.
     */
    public static boolean powell(MultivariateFunction f, double[] pn, double[][] u, double accuracyGoal,
                                 int maxIter) {
        final int n = pn.length;

        final LineMinimizer[] minimizers = new LineMinimizer[n];
        for (int k = 0; k < n; ++k) {
            minimizers[k] = new LineMinimizer(f, pn, u[k]);
        }

        final double[] p0 = new double[n];
        final double[] pe = new double[n];

        double zn = f.value(pn);

        for (int i = 0; i < maxIter; ++i) {
            // 1. Initialize
            System.arraycopy(pn, 0, p0, 0, n);
            double z0 = zn;
            // 2. Successively minimize along all directions
            // 3. Remember magnitude and direction of maximum decrease
            double maxDecrease = 0.0;
            int maxIndex = 0;
            for (int k = 0; k < n; ++k) {
                final double z = minimizers[k].findMinimum(pn);
                final double d = Math.abs(z - zn);
                if (d > maxDecrease) {
                    maxIndex = k;
                    maxDecrease = d;
                }
                zn = z;
            }
            // 4. Stop if ||pn - p0|| < accuracyGoal
            double sum = 0.0;
            for (int k = 0; k < n; ++k) {
                sum += Pow.pow2(pn[k] - p0[k]);
            }
            if (sum < Pow.pow2(accuracyGoal)) {
                return true;
            }
            // 5. Extrapolate
            for (int k = 0; k < n; ++k) {
                pe[k] = 2.0 * pn[k] - p0[k];
            }
            final double ze = f.value(pe);
            // 6. When necessary, discard the direction of maximum decrease
            if (ze < z0) {
                if (2.0 * (z0 - 2.0 * zn + ze) * Pow.pow2(z0 - zn - maxDecrease) < maxDecrease * Pow.pow2(z0 - ze)) {
                    for (int k = 0; k < n; ++k) {
                        u[maxIndex][k] = pn[k] - p0[k];
                    }
                    zn = minimizers[maxIndex].findMinimum(pn);
                }
            }
        }

        return false;
    }

    /**
     * Finds the minimum of a multivariate function along a straight
     * line.
     */
    private static class LineMinimizer {
        private final F1 f;
        private final double[] p;
        private final double[] u;

        private final Min.Bracket bracket;

        /**
         * Constructs a new instance of this class.
         *
         * @param f the multivariate function.
         * @param p the starting point of the line.
         * @param u the direction of the line.
         */
        public LineMinimizer(MultivariateFunction f, double[] p, double[] u) {
            this.f = new F1(f, p, u);
            this.p = p;
            this.u = u;

            bracket = new Min.Bracket();
        }

        /**
         * Finds a minimum of {@code f} along the line defined by {@code p}
         * and {@code u}.
         *
         * @param x the minimum found.
         *
         * @return the value of {@code f} at the minimum found.
         */
        public double findMinimum(double[] x) {
            Min.brack(f, 0.0, 1.0, bracket);
            Min.brent(f, bracket, 1.0E-6);

            for (int i = 0; i < p.length; ++i) {
                x[i] = p[i] + u[i] * bracket.minimumX;
            }

            return bracket.minimumF;
        }
    }

    /**
     * Builds an univariate function from the values of a multivariate
     * function along a straight line.
     */
    private static class F1 implements UnivariateFunction {
        private final MultivariateFunction f;
        private final double[] p;
        private final double[] u;

        private final double[] point;

        public F1(MultivariateFunction f, double[] p, double[] u) {
            this.f = f;
            this.p = p;
            this.u = u;

            point = new double[p.length];
        }

        @Override
        public final double value(double x) {
            for (int i = 0; i < p.length; i++) {
                point[i] = p[i] + x * u[i];
            }

            return f.value(point);
        }
    }
}
