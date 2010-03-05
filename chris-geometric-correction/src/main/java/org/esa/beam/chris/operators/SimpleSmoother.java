package org.esa.beam.chris.operators;

import org.esa.beam.chris.util.math.internal.LocalRegressionSmoother;
import org.esa.beam.chris.util.math.internal.LowessRegressionWeightCalculator;
import org.esa.beam.chris.util.math.internal.Smoother;

/**
 * A simple smoother (effectively doing boxcar averaging).
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class SimpleSmoother implements Smoother {

    private LocalRegressionSmoother smoother;

    /**
     * Constructs a new instance of this class.
     *
     * @param span the span, must be greater than 2.
     *
     * @throws IllegalArgumentException if the span is less than 3.
     */
    SimpleSmoother(int span) {
        smoother = new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, span);
    }

    /**
     * Smoothes a given set of values.
     *
     * @param values         the values.
     * @param smoothedValues the smoothed values.
     *
     * @throws IllegalArgumentException if the lengths of {@code values} and {@code smoothedValues} are
     *                                  different,
     *                                  if the lengths of {@code values} and {@code smoothedValues} are
     *                                  less than the span, or
     *                                  if {@code values} and {@code smoothedValues} are references to
     *                                  the same instance.
     */
    @Override
    public void smooth(double[] values, double[] smoothedValues) {
        smoother.smooth(values, smoothedValues);
    }
}
