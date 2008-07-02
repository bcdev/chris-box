package org.esa.beam.chris.operators;

import java.util.Arrays;

/**
 * Regression weight calculator for locally weighted scatter plot smoothing.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class LowessRegressionWeightCalculator implements LocalRegressionWeightCalculator {

    @Override
    public void calculateRegressionWeights(int i, double[] w) {
        final int b = Math.max(i, w.length - 1 - i);

        for (int j = 0; j < w.length; ++j) {
            w[j] = Math.pow(1.0 - Math.pow(Math.abs((double) (i - j) / b), 3.0), 1.5);
        }
    }

    @Override
    public void calculateRobustRegressionWeights(double[] r, double[] w) {
        System.arraycopy(r, 0, w, 0, r.length);
        Arrays.sort(r);

        final double b = 6.0 * r[r.length / 2];

        for (int i = 0; i < w.length; ++i) {
            if (b > 0.0) {
                if (w[i] < b) {
                    w[i] = (1.0 - (w[i] / b) * (w[i] / b));
                } else {
                    w[i] = 0.0;
                }
            } else { // many vanishing residuals
                w[i] = 1.0;
            }
        }
    }
}
