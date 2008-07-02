package org.esa.beam.chris.operators;

import java.util.Arrays;

/**
 * Special regression weight calculator for boxcar (or moving average) smoothing.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class BoxcarWeightCalculator extends LowessRegressionWeightCalculator {
    
    @Override
    public void calculateRegressionWeights(int i, double[] w) {
        Arrays.fill(w, 1.0);
    }
}
