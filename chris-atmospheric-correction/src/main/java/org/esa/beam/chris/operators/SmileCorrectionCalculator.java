/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.chris.operators;

import org.esa.beam.chris.util.math.internal.LocalRegressionSmoother;
import org.esa.beam.chris.util.math.internal.LowessRegressionWeightCalculator;
import org.esa.beam.chris.util.math.internal.Statistics;
import org.esa.beam.framework.datamodel.Band;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Utility class for calculating the smile correction.
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

    public double calculate(Band[] radianceBands, RenderedImage hyperMaskImage, RenderedImage cloudMaskImage,
                            ResamplerFactory resamplerFactory, CalculatorFactory calculatorFactory) {
        final RenderedImage smileImage = SmileOpImage.createImage(radianceBands, hyperMaskImage, cloudMaskImage,
                                                                  resamplerFactory, calculatorFactory);

        final Raster raster = smileImage.getData();
        final int w = raster.getWidth();

        final double[] columnarCorrections = new double[w];
        raster.getPixels(0, 0, w, 1, columnarCorrections);

        // calculation suggested in the ATBD is effectively the same as taking the median (rq-20090630)
        if (false) {
            final double[] smoothedCorrections = new double[w];
            smoother.smooth(columnarCorrections, smoothedCorrections);
            return Statistics.mean(columnarCorrections);
        }

        return Statistics.median(columnarCorrections);
    }
}
