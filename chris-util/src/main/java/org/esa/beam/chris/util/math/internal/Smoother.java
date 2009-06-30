package org.esa.beam.chris.util.math.internal;

public interface Smoother {

    /**
     * Smoothes a given set of values.
     *
     * @param values         the values.
     * @param smoothedValues the smoothed values.
     *
     * @throws IllegalArgumentException if the lengths of {@code values} and {@code smoothedValues} are
     *                                  different.
     */
    void smooth(double[] values, double[] smoothedValues);
}
