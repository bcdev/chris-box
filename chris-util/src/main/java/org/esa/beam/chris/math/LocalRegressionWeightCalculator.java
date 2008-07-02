package org.esa.beam.chris.math;

/**
 * Local regression weight calculator interface.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public interface LocalRegressionWeightCalculator {
    /**
     * Calculates the local regression weights for the ith point in the span.
     *
     * @param i the point index.
     * @param w the regression weights.
     */
    void calculateRegressionWeights(int i, double[] w);

    /**
     * Calculates the robust regression weights from the absolute residuals
     * of the local regression smoothing procedure.
     *
     * @param a the absolute residuals.
     * @param w the robust regression weights.
     */
    void calculateRobustRegressionWeights(double[] a, double[] w);
}
