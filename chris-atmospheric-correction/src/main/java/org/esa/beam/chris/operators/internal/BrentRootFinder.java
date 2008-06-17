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

import java.text.MessageFormat;

/**
 * Brent root finding algorithm for real-valued functions of a single variable.
 * <p/>
 * The implementation is based on the Brent root finding algorithm provided by
 * the GNU Scientific Libary.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class BrentRootFinder {
    /**
     * Epsilon.
     */
    private static final double DBL_EPSILON = 1.0 / (0x00000001L << 52);
    /**
     * The maximum number of iterations.
     */
    private final int maxIter;

    /**
     * Constructs a new instance of this class.
     */
    public BrentRootFinder() {
        this(100);
    }

    /**
     * Constructs a new instance if this class.
     *
     * @param maxIter the maximum number of iterations being performed.
     */
    public BrentRootFinder(int maxIter) {
        this.maxIter = maxIter;
    }

    /**
     * Finds the root of a function supplied. The root must be bracketed by the
     * closed interval [a, b].
     *
     * @param f the function.
     * @param a the lower bound of the bracketing interval.
     * @param b the upper bound of the bracketing interval.
     *
     * @return any root of f being bracketed by [a, b].
     *
     * @throws IllegalArgumentException when the endpoints {@code a} and {@code b}
     *                                  do not straddle zero.
     */
    public final double findRoot(UnivariateFunction f, double a, double b) throws IllegalArgumentException {
        double fa = f.value(a);
        double fb = f.value(b);

        if ((fa < 0.0 && fb < 0.0) || (fa > 0.0 && fb > 0.0)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "endpoints a = {0}, b = {1} do not straddle y = 0.", a, b));
        }

        double c = b;
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

            tol = 0.5 * DBL_EPSILON * Math.abs(b);
            m = 0.5 * (c - b);

            if (fb == 0) {
                return b;
            }

            if (Math.abs(m) <= tol) {
                return b;
            }

            if (Math.abs(e) < tol || Math.abs(fa) <= Math.abs(fb)) {
                // use bisection
                d = m;
                e = m;
            } else {
                // use inverse cubic interpolation
                double p, q, r;
                final double s = fb / fa;

                if (acEqual) {
                    p = 2 * m * s;
                    q = 1 - s;
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

            if ((fb < 0 && fc < 0) || (fb > 0 && fc > 0)) {
                c = a;
            }
        }

        return b;
    }
}
