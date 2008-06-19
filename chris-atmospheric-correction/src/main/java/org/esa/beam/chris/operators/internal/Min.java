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

import static java.lang.Math.abs;
import static java.lang.Math.min;
import java.text.MessageFormat;

/**
 * Methods for finding the minimum of an univariate function.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Min {

    private static final double GOLDEN = 0.3819660;

    /**
     * Container used to bracket a minimum of an univariate function.
     * <p/>
     * A minimum of a function f is bracketed by a triple of points, if and only if
     * the value of f at the inner point is less than the values of f at both outer
     * points.
     */
    public static class Bracket {
        /**
         * The lower limit of the bracketing interval.
         */
        public double lowerX;
        /**
         * The upper limit of the bracketing interval.
         */
        public double upperX;
        /**
         * The function value at the lower limit of the bracketing interval.
         */
        public double lowerF;
        /**
         * The function value at the upper limit of the bracketing interval.
         */
        public double upperF;
        /**
         * An abscissa value within the bracketing interval.
         */
        public double innerX;
        /**
         * The function value at {@code innerX}. This value must be less than
         * both {@code lowerF} and {@code upperF}.
         */
        public double innerF;

        /**
         * Creates a new instance of this class.
         */
        public Bracket() {
        }

        /**
         * Creates a new instance of this class.
         *
         * @param lowerX the lower limit of the bracketing interval.
         * @param upperX the upper limit of the bracketing interval.
         * @param f      the univariate function.
         */
        Bracket(double lowerX, double upperX, UnivariateFunction f) {
            if (lowerX > upperX) {
                this.lowerX = upperX;
                this.upperX = lowerX;
            } else {
                this.lowerX = lowerX;
                this.upperX = upperX;
            }

            lowerF = f.value(this.lowerX);
            upperF = f.value(this.upperX);

            innerX = this.lowerX + GOLDEN * (this.upperX - this.lowerX);
            innerF = f.value(this.innerX);
        }
    }

    /**
     * Brackets a minimum of an univariate function given two initial abscissa
     * values.
     *
     * @param f       the univariate function.
     * @param a       the lower initial abscissa value.
     * @param b       the upper initial abscissa value.
     * @param bracket the bracket found.
     *
     * @return the bracket found.
     */
    public static Bracket brack(UnivariateFunction f, double a, double b, Bracket bracket) {
        double lowerX = a;
        double lowerF = f.value(a);

        double innerX = b;
        double innerF = f.value(b);

        if (innerF > lowerF) {
            final double lx = lowerX;
            final double lf = lowerF;

            lowerX = innerX;
            lowerF = innerF;

            innerX = lx;
            innerF = lf;
        }

        double upperX = innerX + (innerX - lowerX) * (1.0 / GOLDEN - 1.0);
        double upperF = f.value(upperX);

        while (innerF > upperF) {
            upperX = upperX + (upperX - innerX) * (1.0 / GOLDEN - 1.0);
            upperF = f.value(upperX);
        }

        bracket.innerX = innerX;
        bracket.innerF = innerF;

        if (lowerX > upperX) {
            bracket.lowerX = upperX;
            bracket.lowerF = upperF;
            bracket.upperX = lowerX;
            bracket.upperF = lowerF;
        } else {
            bracket.lowerX = lowerX;
            bracket.lowerF = lowerF;
            bracket.upperX = upperX;
            bracket.upperF = upperF;
        }

        return bracket;
    }

    /**
     * Finds the minimum of an univariate function using Brent's algorithm.
     * <p/>
     * Based on code provided by the GNU Scientific Library (GSL).
     *
     * @param f                    the univariate function.
     * @param bracket              the bracket for the minimum being searched.
     * @param relativeAccuracyGoal the relative accuracy goal for the minimum being searched.
     *
     * @return {@code true} on success.
     *
     * @throws IllegalArgumentException if the {@code bracket} is invalid.
     */
    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal) {
        return brent(f, bracket, relativeAccuracyGoal, 1.0E-10);
    }

    /**
     * Finds the minimum of an univariate function using Brent's algorithm.
     * <p/>
     * Based on code provided by the GNU Scientific Library (GSL).
     *
     * @param f                    the univariate function.
     * @param bracket              the bracket for the minimum being searched.
     * @param relativeAccuracyGoal the relative accuracy goal for the minimum being searched.
     * @param absoluteAccuracyGoal the relative absolute goal for the minimum being searched.
     *
     * @return {@code true} on success.
     *
     * @throws IllegalArgumentException if the {@code bracket} is invalid.
     */
    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal,
                                double absoluteAccuracyGoal) {
        return brent(f, bracket, relativeAccuracyGoal, absoluteAccuracyGoal, 100);
    }

    /**
     * Finds the minimum of an univariate function using Brent's algorithm.
     * <p/>
     * Based on code provided by the GNU Scientific Library (GSL).
     *
     * @param f                    the univariate function.
     * @param bracket              the bracket for the minimum being searched.
     * @param relativeAccuracyGoal the relative accuracy goal for the minimum being searched.
     * @param absoluteAccuracyGoal the relative absolute goal for the minimum being searched.
     * @param maxIter              the maximum number of iterations being performed.
     *
     * @return {@code true} on success.
     *
     * @throws IllegalArgumentException if the {@code bracket} is invalid.
     */
    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal,
                                double absoluteAccuracyGoal, int maxIter) {
        if (bracket.innerF >= bracket.lowerF ||
                bracket.innerF >= bracket.upperF ||
                bracket.innerX <= bracket.lowerX && bracket.innerX <= bracket.upperX ||
                bracket.innerX >= bracket.lowerX && bracket.innerX >= bracket.upperX) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The points a = {0}, b = {1}, c = {2} do not bracket a minimum.",
                    bracket.lowerX, bracket.innerX, bracket.upperX));
        }

        double u;
        double v = bracket.lowerX + GOLDEN * (bracket.upperX - bracket.lowerX);
        double w = v;

        double d = 0.0;
        double e = 0.0;

        double fu;
        double fv = f.value(v);
        double fw = fv;

        for (int i = 0; i < maxIter; ++i) {
            final double a = bracket.lowerX;
            final double b = bracket.upperX;
            final double z = bracket.innerX;

            final double fz = bracket.innerF;

            final double lowerW = (z - a);
            final double upperW = (b - z);
            final double tolerance = Constants.SQRT_DBL_EPSILON * abs(z);

            final double midpoint = 0.5 * (a + b);

            double p = 0.0;
            double q = 0.0;
            double r = 0.0;

            if (abs(e) > tolerance) {
                r = (z - w) * (fz - fv);
                q = (z - v) * (fz - fw);
                p = (z - v) * q - (z - w) * r;
                q = 2.0 * (q - r);

                if (q > 0.0) {
                    p = -p;
                } else {
                    q = -q;
                }

                r = e;
                e = d;
            }
            if (abs(p) < abs(0.5 * q * r) && p < q * lowerW && p < q * upperW) {
                final double t2 = 2.0 * tolerance;

                d = p / q;
                u = z + d;

                if ((u - a) < t2 || (b - u) < t2) {
                    d = (z < midpoint) ? tolerance : -tolerance;
                }
            } else {
                e = (z < midpoint) ? b - z : -(z - a);
                d = GOLDEN * e;
            }
            if (abs(d) >= tolerance) {
                u = z + d;
            } else {
                u = z + ((d > 0.0) ? tolerance : -tolerance);
            }

            fu = f.value(u);

            if (fu <= fz) {
                if (u < z) {
                    bracket.upperX = z;
                    bracket.upperF = fz;
                } else {
                    bracket.lowerX = z;
                    bracket.lowerF = fz;
                }

                v = w;
                w = z;
                fv = fw;
                fw = fz;

                bracket.innerX = u;
                bracket.innerF = fu;
            } else {
                if (u < z) {
                    bracket.lowerX = u;
                    bracket.lowerF = fu;
                } else {
                    bracket.upperX = u;
                    bracket.upperF = fu;
                }
            }
            if (testInterval(bracket.lowerX, bracket.upperX, absoluteAccuracyGoal, relativeAccuracyGoal)) {
                return true;
            }
        }

        return false;
    }

    private static boolean testInterval(double lowerX, double upperX, double absoluteAccuracyGoal,
                                        double relativeAccuracyGoal) {
        final double minAbs;
        if (lowerX > 0.0 && upperX > 0.0 || lowerX < 0.0 && upperX < 0.0) {
            minAbs = min(abs(lowerX), abs(upperX));
        } else {
            minAbs = 0.0;
        }

        return abs(upperX - lowerX) < absoluteAccuracyGoal + relativeAccuracyGoal * minAbs;
    }
}
