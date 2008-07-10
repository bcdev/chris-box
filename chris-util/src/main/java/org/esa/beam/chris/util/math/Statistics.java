package org.esa.beam.chris.util.math;

import java.util.Arrays;

/**
 * The class {@code Statistics} is a utility class providing some statistical
 * functions.
 *
 * @author Ralf Quast
 * @version $Revision: 2530 $ $Date: 2008-07-09 13:10:39 +0200 (Wed, 09 Jul 2008) $
 * @since BEAM 4.2
 */
public class Statistics {

    /**
     * Returns the number of values in an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the number of values.
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static int count(final float[] values) throws NullPointerException {
        if (values == null) {
            throw new NullPointerException("values");
        }

        int count = 0;
        for (final float value : values) {
            if (isValid(value)) {
                ++count;
            }
        }

        return count;
    }

    /**
     * Returns the number of values in an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the number of values.
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static int count(final double[] values) throws NullPointerException {
        if (values == null) {
            throw new NullPointerException("values");
        }

        int count = 0;
        for (final double value : values) {
            if (isValid(value)) {
                ++count;
            }
        }

        return count;
    }

    /**
     * Returns the mean of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the mean value (or {@link Float#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float mean(float[] values) throws NullPointerException {
        if (values == null) {
            throw new NullPointerException("values");
        }

        float sum = 0.0f;
        int count = 0;

        for (final float value : values) {
            if (isValid(value)) {
                sum += value;
                ++count;
            }
        }

        return sum / count;
    }

    /**
     * Returns the mean of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the mean value (or {@link Double#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double mean(final double[] values) throws NullPointerException {
        if (values == null) {
            throw new NullPointerException("values");
        }

        double sum = 0.0;
        int count = 0;

        for (final double value : values) {
            if (isValid(value)) {
                sum += value;
                ++count;
            }
        }

        return sum / count;
    }

    /**
     * Returns the standard deviation of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the standard deviation (or {@link Float#NaN} if {@code values} is empty or
     *         contains less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float sdev(float[] values) throws NullPointerException {
        return (float) Math.sqrt(variance(values));
    }

    /**
     * Returns the standard deviation of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the standard deviation (or {@link Double#NaN} if {@code values} is empty or
     *         includes less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double sdev(double[] values) throws NullPointerException {
        return Math.sqrt(variance(values));
    }

    /**
     * Returns the variance of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     * <p/>
     * To minimize the roundoff error, the implementation uses the <em>corrected two-pass
     * algorithm</em> described by e.g. Press et al. (2002, Numerical Recipes).
     *
     * @param values the values.
     *
     * @return the variance (or {@link Float#NaN} if {@code values} is empty or
     *         includes less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float variance(final float[] values) throws NullPointerException {
        return variance(values, mean(values));
    }

    private static float variance(final float[] values, final float mean) {
        float var = 0.0f;
        float sum = 0.0f;
        int count = 0;

        for (final float value : values) {
            if (isValid(value)) {
                final float d = value - mean;
                sum += d;
                var += d * d;

                ++count;
            }
        }

        return (var - sum * sum / count) / (count - 1);
    }

    /**
     * Returns the variance of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     * <p/>
     * To minimize the roundoff error, the implementation uses the <em>corrected two-pass
     * algorithm</em> described by e.g. Press et al. (2002, Numerical Recipes).
     *
     * @param values the values.
     *
     * @return the variance (or {@link Double#NaN} if {@code values} is empty or
     *         includes less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double variance(final double[] values) throws NullPointerException {
        return variance(values, mean(values));
    }

    private static double variance(final double[] values, final double mean) {
        double var = 0.0;
        double sum = 0.0;
        int count = 0;

        for (final double value : values) {
            if (isValid(value)) {
                final double d = value - mean;
                sum += d;
                var += d * d;

                ++count;
            }
        }

        return (var - sum * sum / count) / (count - 1);
    }

    /**
     * Returns the minimum of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the minimum value (or {@link Float#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float min(final float[] values) {
        if (values == null) {
            throw new NullPointerException("values");
        }

        float min = Float.POSITIVE_INFINITY;

        for (final float value : values) {
            if (isValid(value)) {
                if (value < min) {
                    min = value;
                }
            }

        }
        if (Float.isInfinite(min)) {
            min = Float.NaN;
        }

        return min;
    }

    /**
     * Returns the minimum of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the minimum value (or {@link Double#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double min(final double[] values) {
        if (values == null) {
            throw new NullPointerException("values");
        }

        double min = Double.POSITIVE_INFINITY;

        for (final double value : values) {
            if (isValid(value)) {
                if (value < min) {
                    min = value;
                }
            }

        }
        if (Double.isInfinite(min)) {
            min = Double.NaN;
        }

        return min;
    }

    /**
     * Returns the maximum of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the maximum value (or {@link Float#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float max(final float[] values) {
        if (values == null) {
            throw new NullPointerException("values");
        }

        float max = Float.NEGATIVE_INFINITY;

        for (final float value : values) {
            if (isValid(value)) {
                if (value > max) {
                    max = value;
                }
            }

        }
        if (Float.isInfinite(max)) {
            max = Float.NaN;
        }

        return max;
    }

    /**
     * Returns the maximum of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the maximum value (or {@link Double#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double max(final double[] values) {
        if (values == null) {
            throw new NullPointerException("values");
        }

        double max = Double.NEGATIVE_INFINITY;

        for (final double value : values) {
            if (isValid(value)) {
                if (value > max) {
                    max = value;
                }
            }

        }
        if (Double.isInfinite(max)) {
            max = Double.NaN;
        }

        return max;
    }

    /**
     * Returns the coefficient of variation for an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the coefficient of variation (or {@link Float#NaN} if {@code values} is empty or
     *         includes less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float cv(final float[] values) throws NullPointerException {
        final float mean = mean(values);
        final float sdev = (float) Math.sqrt(variance(values, mean));

        return sdev / mean;
    }

    /**
     * Returns the coefficient of variation for an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the coefficient of variation (or {@link Double#NaN} if {@code values} is empty or
     *         includes less than two valid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double cv(final double[] values) throws NullPointerException {
        final double mean = mean(values);
        final double sdev = Math.sqrt(variance(values, mean));

        return sdev / mean;
    }

    /**
     * Returns the median of an array of {@code float} values. The internal
     * {@link Float#NaN}, {@link Float#POSITIVE_INFINITY} and {@link Float#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the median value (or {@link Float#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static float median(final float[] values) {
        final int count = count(values);
        final float[] floats = new float[count];

        for (int i = 0, j = 0; j < count; ++i) {
            if (isValid(values[i])) {
                floats[j++] = values[i];
            }
        }

        float median = Float.NaN;

        if (count > 0) {
            final int half = count >> 1;
            Arrays.sort(floats);

            if (half << 1 == count) {
                // even
                median = (float) (0.5 * (floats[half - 1] + floats[half]));
            } else {
                // odd
                median = floats[half];
            }
        }

        return median;
    }


    /**
     * Returns the median of an array of {@code double} values. The internal
     * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY} and {@link Double#NEGATIVE_INFINITY}
     * are interpreted as missing values.
     *
     * @param values the values.
     *
     * @return the median value (or {@link Double#NaN} if {@code values} is empty or
     *         includes only invalid values).
     *
     * @throws NullPointerException if {@code values} is {@code null}.
     */
    public static double median(final double[] values) {
        final int count = count(values);
        final double[] doubles = new double[count];

        for (int i = 0, j = 0; j < count; ++i) {
            if (isValid(values[i])) {
                doubles[j++] = values[i];
            }
        }

        double median = Double.NaN;

        if (count > 0) {
            final int half = count >> 1;
            Arrays.sort(doubles);

            if (half << 1 == count) {
                // even
                median = 0.5 * (doubles[half - 1] + doubles[half]);
            } else {
                // odd
                median = doubles[half];
            }
        }

        return median;
    }

    private static boolean isValid(float value) {
        return !(Float.isNaN(value) || Float.isInfinite(value));
    }

    private static boolean isValid(final double value) {
        return !(Double.isNaN(value) || Double.isInfinite(value));
    }

}
