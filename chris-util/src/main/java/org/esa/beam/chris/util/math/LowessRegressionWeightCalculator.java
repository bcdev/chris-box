package org.esa.beam.chris.util.math;

import java.util.Arrays;

/**
 * Regression weight calculator for locally weighted scatter plot smoothing.
 *
 * @author Ralf Quast
 * @version $Revision: 2530 $ $Date: 2008-07-09 13:10:39 +0200 (Wed, 09 Jul 2008) $
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
    public void calculateRobustRegressionWeights(double[] a, double[] w) {
        // copy the residuals since they are sorted in the next line
        System.arraycopy(a, 0, w, 0, a.length);
        Arrays.sort(a);
        // six-fold median absolute deviation
        final double mad6 = 6.0 * a[a.length / 2];

        for (int i = 0; i < w.length; ++i) {
            if (mad6 > 0.0) {
                if (w[i] < mad6) {
                    w[i] = (1.0 - (w[i] / mad6) * (w[i] / mad6));
                } else {
                    w[i] = 0.0;
                }
            } else { // smoothing is not affected by outliers  
                w[i] = 1.0;
            }
        }
    }
}
