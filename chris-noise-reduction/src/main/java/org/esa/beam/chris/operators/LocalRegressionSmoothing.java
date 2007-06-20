/* $Id: $
 *
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
import com.bc.ceres.core.Assert;

import java.util.Arrays;

/**
 * Class for performing local regression smoothing.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
class LocalRegressionSmoothing {

    private int degree; // the polynomial degree
    private int span;   // the span
    private int iter;   // the number of robust regression iterations

    private double[][] p; // the values of the basis functions for all points in the span
    private double[][] w; // the regression weights for all points in the span

    /**
     * Constructs an instance of this class which does not perform robust smoothing.
     *
     * @param degree the polynomial degree
     * @param span   the span, must be greater than {@code degree + 2}.
     * @throws IllegalArgumentException if the polynomial degree is a negative nnumber.
     * @throws IllegalArgumentException if the span is not a positive number.
     * @throws IllegalArgumentException if the span is not greater than {@code degree + 2}.
     */
    public LocalRegressionSmoothing(int degree, int span) {
        this(degree, span, 0);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param degree the polynomial degree
     * @param span   the span, must be greater than {@code degree + 2}.
     * @param iter   the number of robust regression iterations performed.
     * @throws IllegalArgumentException if the polynomial degree is a negative number.
     * @throws IllegalArgumentException if the span is not a positive number.
     * @throws IllegalArgumentException if the span is not greater than {@code degree + 2}.
     * @throws IllegalArgumentException if the number of robust regression is negative.
     */
    public LocalRegressionSmoothing(int degree, int span, int iter) {
        Assert.argument(degree >= 0, "!(degree >= 0)");
        Assert.argument(span > 0, "!(span > 0)");
        Assert.argument(span > degree + 2, "!(span > degree + 2)");
        Assert.argument(iter >= 0, "!(iter >= 0)");

        this.degree = degree;
        this.span = span;
        this.iter = iter;
    }

    /**
     * Returns the polynomial degree.
     *
     * @return the polynomial drgree.
     */
    public int getPolynomialDegree() {
        return degree;
    }

    /**
     * Returns the span.
     *
     * @return the span.
     */
    public int getSpan() {
        return span;
    }

    /**
     * Returns the number of robust regression iterations.
     *
     * @return the number of robust regression iterations.
     */
    public int getRobustRegressionCount() {
        return iter;
    }

    /**
     * Performs a local regression smoothing of the given response values.
     *
     * @param y the response values.
     * @param z the smoothed response values.
     * @throws IllegalArgumentException if the lengths of {@code y} and {@code z} are
     *                                  different.
     * @throws IllegalArgumentException if the lengths of {@code y} and {@code z} are
     *                                  less than the span.
     * @throws IllegalArgumentException if {@code y} and {@code z} are references to
     *                                  the same instance.
     */
    public void smooth(double[] y, double[] z) {
        Assert.argument(y.length == z.length, "!(y.length == z.length)");
        Assert.argument(y.length >= span, "!(y.length >= span)");
        Assert.argument(!y.equals(z), "y.equals(z)");

        final int m = y.length;
        final int n = degree + 1;

        final double[] r = new double[m];    // absolute residuals, robust weights
        final double[] c = new double[n];    // linear coefficients
        final double[] g = new double[span]; // robust regression weights

        if (p == null) {
            initialize();
        }

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
                r[i] = Math.abs(z[i] - y[i]);
            }
            calculateRobustWeights(r);
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

    /**
     * Function for calculating the regression weights for the ith point in the span.
     * <p/>
     * The weights are equal to the multiplying coefficients of the design matrix of
     * the corresponding linear regression problem.
     *
     * @param i the point index.
     * @param w the regression weights for the ith point in the span.
     */
    protected void calculateRegressionWeights(int i, double[] w) {
        final int b = Math.max(i, w.length - 1 - i);

        for (int j = 0; j < w.length; ++j) {
            w[j] = Math.pow(1.0 - Math.pow(Math.abs((double) (i - j) / b), 3.0), 1.5);
        }
    }

    /**
     * Function for calculating the robust weights from the absolute residuals.
     * <p/>
     * The weights are equal to the multiplying coefficients of the design matrix of
     * the corresponding linear regression problem.
     * For solving the linear regression problem, the rows of the design matrix are
     * multiplied by these weights.
     *
     * @param r the absolute residuals. On return holds the robust weights.
     */
    protected void calculateRobustWeights(double[] r) {
        final double b = 6.0 * median(r);

        for (int i = 0; i < r.length; ++i) {
            if (r[i] > 0.0) {
                if (r[i] < b) {
                    r[i] = (1.0 - (r[i] / b) * (r[i] / b));
                } else {
                    r[i] = 0.0;
                }
            } else { // vanishing residual (rare case)
                r[i] = 1.0;
            }
        }
    }

    private void initialize() {
        final Functions polynomials = new LegendrePolynomials();

        p = new double[span][degree + 1];
        w = new double[span][span];

        for (int i = 0; i < span; ++i) {
            final double x = 2.0 * ((double) i / (span - 1)) - 1.0; // mapping onto [-1, 1]

            polynomials.calculate(x, p[i]);
            calculateRegressionWeights(i, w[i]);
        }
    }

    private static double median(double[] values) {
        final double[] a = Arrays.copyOf(values, values.length);
        Arrays.sort(a);

        return a[values.length / 2];
    }

    private static void fit(double[] y, int from, double[] w, double[] c, double[][]p) {
        assert(w.length == p.length);
        assert(w.length >= c.length);

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

}
