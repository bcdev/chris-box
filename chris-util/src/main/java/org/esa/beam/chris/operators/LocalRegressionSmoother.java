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
package org.esa.beam.chris.operators;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Class for performing local regression smoothing.
 * <p/>
 * Note that the algorithm is not strictly correct for response values
 * not corresponding to equidistant predictor values.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class LocalRegressionSmoother {

    private final LocalRegressionWeightCalculator regressionWeightCalculator;

    private final int degree; // the polynomial degree
    private final int span;   // the span

    private final int iter;   // the number of robust regression iterations
    private final double[][] p; // the values of the basis functions for all points in the span
    private final double[][] w; // the regression weights for all points in the span

    /**
     * Constructs a new instance of this class.
     * <p/>
     * Note that no robust regression iterations are carried out.
     *
     * @param regressionWeightCalculator the regression weight calculator.
     * @param degree                     the polynomial degree.
     * @param span                       the span, must be greater than {@code degree + 2}.
     *
     * @throws IllegalArgumentException if the polynomial degree is a negative number,
     *                                  or if the span is less than {@code degree + 3}.
     */
    public LocalRegressionSmoother(LocalRegressionWeightCalculator regressionWeightCalculator, int degree, int span) {
        this(regressionWeightCalculator, degree, span, 0);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param regressionWeightCalculator the regression weight calculator.
     * @param degree                     the polynomial degree.
     * @param span                       the span, must be greater than {@code degree + 2}.
     * @param iter                       the number of robust regression iterations being performed.
     *
     * @throws IllegalArgumentException if the polynomial degree is a negative number,
     *                                  if the span is less than {@code degree + 3}, or
     *                                  if the number of robust regression is negative.
     */
    public LocalRegressionSmoother(LocalRegressionWeightCalculator regressionWeightCalculator, int degree, int span,
                                   int iter) {
        Assert.argument(degree >= 0, "!(degree >= 0)");
        Assert.argument(span > degree + 2, "!(span > degree + 2)");
        Assert.argument(iter >= 0, "!(iter >= 0)");

        this.regressionWeightCalculator = regressionWeightCalculator;
        this.degree = degree;
        this.span = span;
        this.iter = iter;

        p = new double[span][degree + 1];
        w = new double[span][span];

        final UnivariateFunctionSequence polynomials = new LegendrePolynomials();
        for (int i = 0; i < span; ++i) {
            polynomials.calculate(2.0 * ((double) i / (span - 1)) - 1.0, p[i]);
            regressionWeightCalculator.calculateRegressionWeights(i, w[i]);
        }
    }

    /**
     * Returns the polynomial degree.
     *
     * @return the polynomial drgree.
     */
    public final int getPolynomialDegree() {
        return degree;
    }

    /**
     * Returns the span.
     *
     * @return the span.
     */
    public final int getSpan() {
        return span;
    }

    /**
     * Returns the number of robust regression iterations.
     *
     * @return the number of robust regression iterations.
     */
    public final int getRobustRegressionIterationCount() {
        return iter;
    }

    /**
     * Performs a local regression smoothing of the given response values.
     *
     * @param y the response values.
     * @param z the smoothed response values.
     *
     * @throws IllegalArgumentException if the lengths of {@code y} and {@code z} are
     *                                  different,
     *                                  if the lengths of {@code y} and {@code z} are
     *                                  less than the span, or
     *                                  if {@code y} and {@code z} are references to
     *                                  the same instance.
     */
    public final void smooth(double[] y, double[] z) {
        Assert.argument(y.length == z.length, "!(y.length == z.length)");
        Assert.argument(y.length >= span, "!(y.length >= span)");
        Assert.argument(y != z, "y == z");

        final int m = y.length;
        final int n = degree + 1;

        final double[] a = new double[m];    // absolute residuals
        final double[] r = new double[m];    // robust weights
        final double[] c = new double[n];    // linear coefficients
        final double[] g = new double[span]; // robust regression weights

        // local regression smoothing
        for (int i = 0, from = 0; i < m; ++i) {
            if (i > span / 2) {
                if (from < m - span) {
                    ++from;
                }
            }
            fit(y, from, w[i - from], c, p);
            z[i] = 0.0;
            for (int j = 0; j < n; ++j) {
                z[i] += c[j] * p[i - from][j];
            }
        }
        // robust smoothing
        for (int k = 0; k < iter; ++k) {
            for (int i = 0; i < m; ++i) {
                a[i] = Math.abs(z[i] - y[i]);
            }
            regressionWeightCalculator.calculateRobustRegressionWeights(a, r);
            for (int i = 0, from = 0; i < m; ++i) {
                if (i > span / 2) {
                    if (from < m - span) {
                        ++from;
                    }
                }
                for (int j = 0; j < span; ++j) {
                    g[j] = w[i - from][j] * r[from + j];
                }
                fit(y, from, g, c, p);
                z[i] = 0.0;
                for (int j = 0; j < n; ++j) {
                    z[i] += c[j] * p[i - from][j];
                }
            }
        }
    }

    private static void fit(double[] y, int from, double[] w, double[] c, double[][] p) {
        assert (w.length == p.length);
        assert (w.length >= c.length);

        try {
            fastFit(y, from, w, c, p);
        } catch (ArithmeticException e) {
            safeFit(y, from, w, c, p);
        }
    }

    /**
     * Performs a linear least squares fit. Solves the normal equations by means of
     * Cholesky decomposition,  which is about a factor of six faster than singular
     * value decomposition.
     *
     * @param y    the response values.
     * @param from the index of the first response value to be considered.
     * @param w    the weights associated with the response values. These are equal
     *             to the multiplying coefficients of the design matrix.
     * @param c    the linear coefficients.
     * @param p    the values of the basis functions for all points considered.
     *
     * @throws ArithmeticException if the design matrix multiplied by its transpose
     *                             is not positive definite.
     */
    static void fastFit(double[] y, int from, double[] w, double[] c, double[][] p) throws ArithmeticException {
        final int m = w.length;
        final int n = c.length;

        final double[][] a = new double[n][n];
        final double[] b = new double[n];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                for (int k = j; k < n; ++k) {
                    a[j][k] += (w[i] * w[i]) * (p[i][j] * p[i][k]);
                }
                b[j] += (w[i] * w[i]) * y[from + i] * p[i][j];
            }
        }
        for (int i = 0; i < n; ++i) {
            for (int j = i; j < n; ++j) {
                double r = a[i][j];
                for (int k = 0; k < i; ++k) {
                    r -= a[i][k] * a[j][k];
                }
                if (i < j) {
                    a[j][i] = r / a[i][i];
                } else {
                    if (r > 0.0) {
                        a[i][i] = Math.sqrt(r);
                    } else {
                        throw new ArithmeticException();
                    }
                }
            }
        }
        for (int i = 0; i < n; ++i) {
            c[i] = b[i];
            for (int k = 0; k < i; ++k) {
                c[i] -= a[i][k] * c[k];
            }
            c[i] /= a[i][i];
        }
        for (int i = n; i-- > 0;) {
            for (int k = i + 1; k < n; ++k) {
                c[i] -= a[k][i] * c[k];
            }
            c[i] /= a[i][i];
        }
    }

    /**
     * Performs a linear least squares fit.  Uses the singular value decomposition
     * of the design matrix for calculating the linear coefficients, which is most
     * robust.
     *
     * @param y    the response values.
     * @param from the index of the first response value to be considered.
     * @param w    the weights associated with the response values. These are equal
     *             to the multiplying coefficients of the design matrix.
     * @param c    the linear coefficients.
     * @param p    the values of the basis functions for all points considered.
     */
    static void safeFit(double[] y, int from, double[] w, double[] c, double[][] p) {
        final int m = w.length;
        final int n = c.length;

        final double[][] a = new double[m][n]; // the design matrix
        final double[] b = new double[m];

        for (int i = 0; i < m; ++i) {
            for (int j = 0; j < n; ++j) {
                a[i][j] = w[i] * p[i][j];
            }
            b[i] = w[i] * y[from + i];
        }

        final SingularValueDecomposition svd = new Matrix(a, m, n).svd();
        final Matrix u = svd.getU();
        final Matrix v = svd.getV();

        final double[] s = svd.getSingularValues();
        final int rank = svd.rank();

        for (int j = 0; j < rank; ++j) {
            c[j] = 0.0;
            for (int i = 0; i < m; ++i) {
                c[j] += u.get(i, j) * b[i];
            }
            s[j] = c[j] / s[j];
        }
        for (int j = 0; j < n; ++j) {
            c[j] = 0.0;
            for (int i = 0; i < rank; ++i) {
                c[j] += v.get(j, i) * s[i];
            }
        }
    }

    private static class Assert {

        public static boolean argument(boolean expression, String message) {
            if (!expression) {
                throw new IllegalArgumentException(message);
            }
            return expression;
        }
    }
}
