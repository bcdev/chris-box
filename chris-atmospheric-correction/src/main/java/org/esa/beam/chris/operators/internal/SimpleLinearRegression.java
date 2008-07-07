package org.esa.beam.chris.operators.internal;

/**
 * Class performing the function of calculating simple linear regressions. See
 * Mendenhall & Sincich (1995, Statistics for Engineering and the Sciences).
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class SimpleLinearRegression {
    /**
     * The number of (x, y) pairs.
     */
    private final int count;
    /**
     * The mask indicating valid (x, y) pairs.
     */
    private final boolean[] valid;

    /**
     * The y-intercept of the regression line.
     */
    private double b0;
    /**
     * The slope of the regression line.
     */
    private double b1;
    /**
     * The sum of all x values.
     */
    private double sx;
    /**
     * The sum of all y values.
     */
    private double sy;
    /**
     * A helper variable storing an intermediate result.
     */
    private double ssyy;
    /**
     * A helper variable storing an intermediate result.
     */
    private double ssxy;
    /**
     * A helper variable storing an intermediate result.
     */
    private double ssxx;
    /**
     * The sum of squared errors.
     */
    private double sse;

    /**
     * Computes a simple linear regression for the ({@code x[i]}, {@code y[i]})
     * pairs supplied as arguments. Pairs with {@code x[i]} or {@code y[i]} equal
     * to {@link Double#NaN}, {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}
     * are ignored.
     * <p/>
     * Note that a minimum of two valid pairs is required to calculate a regression.
     *
     * @param x the values of the independent variable.
     * @param y the values of the dependent variable.
     *
     * @throws IllegalArgumentException if the length of the argument arrays is different.
     * @throws NullPointerException     if any argument is {@code null}.
     */
    public SimpleLinearRegression(final double[] x, final double[] y) {
        this(x, y, 0, x.length);
    }

    /**
     * Computes a simple linear regression for the ({@code x[i]}, {@code y[i]})
     * pairs supplied as arguments. Pairs with {@code x[i]} or {@code y[i]} equal
     * to {@link Double#NaN}, {@link Double#NEGATIVE_INFINITY} or {@link Double#POSITIVE_INFINITY}
     * are ignored.
     * <p/>
     * Note that a minimum of two valid pairs is required to calculate a regression.
     *
     * @param x    the values of the independent variable.
     * @param y    the values of the dependent variable.
     * @param from the index of the first ({@code x[i]}, {@code y[i]}) pair being considered.
     * @param to   the index of the final ({@code x[i]}, {@code y[i]}) pair being considered (exclusive).
     *
     * @throws IllegalArgumentException if the length of the argument arrays is different.
     * @throws NullPointerException     if any argument is {@code null}.
     */
    public SimpleLinearRegression(final double[] x, final double[] y, int from, int to) {
        ensureNotNullAndEqualLength(x, y);

        if (from < 0) {
            throw new IllegalArgumentException("from < 0");
        }
        if (to < 0) {
            throw new IllegalArgumentException("to < 0");
        }
        if (to > x.length) {
            throw new IllegalArgumentException("to > x.length");
        }

        valid = new boolean[x.length];
        count = validate(x, y, from, to);

        computeRegression(x, y, from, to);
    }

    /**
     * Returns the number of valid ({@code x[i]}, {@code y[i]}) pairs.
     *
     * @return the number of valid pairs.
     */
    public final int getCount() {
        return count;
    }

    /**
     * Returns the variance of the random error, which is estimated by dividing the sum of
     * squared errors (i.e. residuals) by the degrees of freedom.
     *
     * @return the estimated variance of the random error.
     */
    public final double getEstimatedVariance() {
        return sse / (count - 2);
    }

    /**
     * Returns the coefficient of determination.
     *
     * @return the coefficient of determination.
     */
    public final double getRSquared() {
        return 1.0 - sse / ssyy;
    }

    /**
     * Returns the <em>y</em>-intercept of the regression line.
     *
     * @return the <em>y</em>-intercept.
     */
    public final double getIntercept() {
        return b0;
    }

    /**
     * Returns the standard error of the <em>y</em>-intercept of the regression line.
     *
     * @return the standard error of the y-intercept.
     */
    public final double getInterceptSE() {
        return Math.sqrt(getEstimatedVariance() / count * (1.0 + (sx * sx) / ssxx / count));
    }

    /**
     * Returns the slope of the regression line.
     *
     * @return the slope.
     */
    public final double getSlope() {
        return b1;
    }

    /**
     * Returns the standard error of the slope of the regression line.
     *
     * @return the standard error of the slope
     */
    public final double getSlopeSE() {
        return Math.sqrt(getEstimatedVariance() / ssxx);
    }

    private void computeRegression(final double[] x, final double[] y, int from, int to) {
        for (int i = from; i < to; ++i) {
            if (valid[i]) {
                sx += x[i];
                sy += y[i];
            }
        }

        final double xm = sx / count;
        final double ym = sy / count;

        for (int i = from; i < to; ++i) {
            if (valid[i]) {
                final double dx = x[i] - xm;
                final double dy = y[i] - ym;

                ssxx += dx * dx;
                ssxy += dx * dy;
                ssyy += dy * dy;
            }
        }

        b1 = ssxy / ssxx;
        b0 = (sy - b1 * sx) / count;

        for (int i = from; i < to; ++i) {
            if (valid[i]) {
                final double dy = y[i] - (b0 + b1 * x[i]);

                sse += dy * dy;
            }
        }
    }

    private static void ensureNotNullAndEqualLength(final double[] x, final double[] y) {
        if (x == null) {
            throw new NullPointerException("x == null");
        }
        if (y == null) {
            throw new NullPointerException("y == null");
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
    }

    /**
     * Validates the given ({@code x[i]}, {@code y[i]}) pairs and returns
     * the number of valid pairs.
     *
     * @param x    the values of the independent variable.
     * @param y    the corresponding values of the dependent variable.
     * @param from the index of the first ({@code x[i]}, {@code y[i]}) pair being considered.
     * @param to   the index of the final ({@code x[i]}, {@code y[i]}) pair being considered (exclusive).
     *
     * @return the number of valid pairs.
     */
    private int validate(final double[] x, final double[] y, int from, int to) {
        int count = 0;

        for (int i = from; i < to; ++i) {
            if (isValid(x[i], y[i])) {
                valid[i] = true;
                ++count;
            }
        }

        return count;
    }

    private static boolean isValid(final double x, final double y) {
        return !(Double.isNaN(x) || Double.isInfinite(x) || Double.isNaN(y) || Double.isInfinite(y));
    }
}
