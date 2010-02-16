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
 * Methods for finding the roots of an univariate function.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Roots {

    /**
     * Container for bracketing a root of an univariate function.
     * <p/>
     * Note that a root of an univariate function is bracketed if and only if the
     * endpoints of the bracketing interval do straddle zero.
     */
    public static class Bracket {
        /**
         * The lower abcissa value of the bracketing interval.
         */
        public double lowerX;
        /**
         * The upper abcissa value of the bracketing interval.
         */
        public double upperX;
        /**
         * The abcissa value within the bracketing interval which
         * is used for estimating the root.
         */
        public double root;

        /**
         * Constructs a new instance of this class.
         *
         * @param lowerX the lower abcissa value of the bracketing interval.
         * @param upperX the upper abcissa value of the bracketing interval.
         */
        public Bracket(double lowerX, double upperX) {
            if (lowerX > upperX) {
                this.upperX = lowerX;
                this.lowerX = upperX;
            } else {
                this.lowerX = lowerX;
                this.upperX = upperX;
            }

            root = (lowerX + upperX) / 2;
        }

        /**
         * Tests if a root of an univariate function is bracketed.
         *
         * @param f the univariate function.
         *
         * @return {@code true}, if the end points of the bracket do straddle
         *         zero, {code false} otherwise.
         */
        public boolean isBracket(UnivariateFunction f) {
            final double lowerF = f.value(lowerX);
            final double upperF = f.value(upperX);

            return !(lowerF < 0.0 && upperF < 0.0 || lowerF > 0.0 && upperF > 0.0);
        }
    }

    /**
     * Finds the root of an univariate function using Brent's algorithm.
     * <p/>
     * Based on code provided by the GNU Scientific Library (GSL).
     *
     * @param f       the function.
     * @param bracket the interval bracketing the root.
     * @param maxIter the maximum number of iterations.
     *
     * @return {@code true} on success.
     */
    public static boolean brent(UnivariateFunction f, Bracket bracket, int maxIter) {
        double a = bracket.lowerX;
        double b = bracket.upperX;
        double c = b;

        double fa = f.value(a);
        double fb = f.value(b);
        double fc = fb;

        double d = b - a;
        double e = b - a;

        for (int i = 0; i < maxIter; ++i) {
            final double tol, m;

            boolean acEqual = false;

            if ((fb < 0 && fc < 0) || (fb > 0 && fc > 0)) {
                acEqual = true;
                c = a;
                fc = fa;
                d = b - a;
                e = b - a;
            }

            if (Math.abs(fc) < Math.abs(fb)) {
                acEqual = true;
                a = b;
                b = c;
                c = a;
                fa = fb;
                fb = fc;
                fc = fa;
            }

            tol = 0.5 * Constants.DBL_EPSILON * Math.abs(b);
            m = 0.5 * (c - b);

            if (fb == 0) {
                bracket.root = b;
                bracket.lowerX = b;
                bracket.upperX = b;

                return true;
            }

            if (Math.abs(m) <= tol) {
                bracket.root = b;

                if (b < c) {
                    bracket.lowerX = b;
                    bracket.upperX = c;
                } else {
                    bracket.lowerX = c;
                    bracket.upperX = b;
                }

                return true;
            }

            if (Math.abs(e) < tol || Math.abs(fa) <= Math.abs(fb)) {
                // bisection
                d = m;
                e = m;
            } else {
                // inverse cubic interpolation
                final double s = fb / fa;
                double p, q, r;

                if (acEqual) {
                    p = 2.0 * m * s;
                    q = 1.0 - s;
                } else {
                    q = fa / fc;
                    r = fb / fc;
                    p = s * (2.0 * m * q * (q - r) - (b - a) * (r - 1.0));
                    q = (q - 1.0) * (r - 1.0) * (s - 1.0);
                }

                if (p > 0) {
                    q = -q;
                } else {
                    p = -p;
                }

                if (2.0 * p < Math.min(3.0 * m * q - Math.abs(tol * q), Math.abs(e * q))) {
                    e = d;
                    d = p / q;
                } else {
                    // interpolation failed, fall back to bisection
                    d = m;
                    e = m;
                }
            }

            a = b;
            fa = fb;

            if (Math.abs(d) > tol) {
                b += d;
            } else {
                if (m > 0.0) {
                    b += tol;
                } else {
                    b -= tol;
                }
            }

            fb = f.value(b);
            if ((fb < 0.0 && fc < 0.0) || (fb > 0.0 && fc > 0.0)) {
                c = a;
            }

            bracket.root = b;
            if (b < c) {
                bracket.lowerX = b;
                bracket.upperX = c;
            } else {
                bracket.lowerX = c;
                bracket.upperX = b;
            }
        }

        return false;
    }
}
