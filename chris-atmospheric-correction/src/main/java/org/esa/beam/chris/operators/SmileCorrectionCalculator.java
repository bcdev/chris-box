package org.esa.beam.chris.operators;

import org.esa.beam.chris.math.LocalRegressionSmoother;
import org.esa.beam.chris.math.LowessRegressionWeightCalculator;
import org.esa.beam.chris.math.Statistics;
import org.esa.beam.framework.datamodel.Band;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class SmileCorrectionCalculator {
    private LocalRegressionSmoother smoother;

    public SmileCorrectionCalculator() {
        smoother = new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 27);
    }

    public double calculateCorrection(Band[] toaRadianceBands, RenderedImage hyperMaskImage,
                                      RenderedImage cloudMaskImage, CalculatorFactory calculatorFactory) {
        final RenderedImage smileImage = SmileOpImage.createImage(toaRadianceBands, hyperMaskImage, cloudMaskImage,
                                                                  calculatorFactory);

        final Raster raster = smileImage.getData();
        final int w = raster.getWidth();

        final double[] originalShifts = new double[w];
        raster.getPixels(0, 0, w, 1, originalShifts);

        final double[] smoothedShifts = new double[w];
        smoother.smooth(originalShifts, smoothedShifts);

        return Statistics.mean(smoothedShifts);
    }
}
