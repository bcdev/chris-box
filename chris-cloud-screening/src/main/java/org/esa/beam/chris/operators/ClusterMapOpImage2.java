/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators;

import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ClusterMapOpImage2 extends PointOpImage {

    private final Band[] sourceBands;
    private final ProbabilityCalculator calculator;
    private final IndexFilter indexFilter;
    private final int clusterCount;

    public static OpImage createImage(Band[] sourceBands, ProbabilityCalculator calculator, IndexFilter indexFilter,
                                      int clusterCount) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        for (final Band sourceBand : sourceBands) {
            RenderedImage sourceImage = sourceBand.getImage();
            if (sourceImage == null) {
                sourceImage = new RasterDataNodeOpImage(sourceBand);
                sourceBand.setImage(sourceImage);
            }
            sourceImageVector.add(sourceImage);
        }

        final int w = sourceImageVector.get(0).getWidth();
        final int h = sourceImageVector.get(0).getHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new ClusterMapOpImage2(imageLayout, sourceImageVector, sourceBands, calculator, indexFilter, clusterCount);
    }

    private ClusterMapOpImage2(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector,
                               Band[] sourceBands, ProbabilityCalculator calculator, IndexFilter indexFilter,
                               int clusterCount) {
        super(sourceImageVector, imageLayout, null, true);

        this.sourceBands = sourceBands;
        this.calculator = calculator;
        this.indexFilter = indexFilter;
        this.clusterCount = clusterCount;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final RasterFormatTag[] formatTags = getFormatTags();
        final RasterAccessor[] sourceAccessors = new RasterAccessor[sources.length];

        for (int i = 0; i < sources.length; ++i) {
            sourceAccessors[i] = new RasterAccessor(sources[i], rectangle, formatTags[i], getSourceImage(i).getColorModel());
        }

        final RasterAccessor targetAccessor =
                new RasterAccessor(target, rectangle, formatTags[sources.length], getColorModel());

        final int targetDataType = targetAccessor.getDataType();
        if (targetDataType == DataBuffer.TYPE_INT) {
            intLoop(sourceAccessors, targetAccessor);
        } else {
            throw new IllegalStateException("Target data type '" + targetDataType + "' not supported.");
        }

        if (targetAccessor.isDataCopy()) {
            targetAccessor.clampDataArrays();
            targetAccessor.copyDataToRaster();
        }
    }

    private void intLoop(RasterAccessor[] sources, RasterAccessor target) {
        final int[] targetDataArray = target.getIntDataArray(0);
        final int targetBandOffset = target.getBandOffset(0);
        final int targetPixelStride = target.getPixelStride();
        final int targetScanlineStride = target.getScanlineStride();

        final int[][] sourceDataArrays = new int[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            sourceDataArrays[i] = sources[i].getIntDataArray(0);
        }

        int sourceBandOffset = sources[0].getBandOffset(0);
        int sourcePixelStride = sources[0].getPixelStride();
        int sourceScanlineStride = sources[0].getScanlineStride();

        int sourceScanlineOffset = sourceBandOffset;
        int targetScanlineOffset = targetBandOffset;

        final double[] sourceSamples = new double[sourceDataArrays.length];
        final double[] posteriors = new double[clusterCount];

        for (int y = 0; y < target.getHeight(); y++) {
            int sourcePixelOffset = sourceScanlineOffset;
            int targetPixelOffset = targetScanlineOffset;

            for (int x = 0; x < target.getWidth(); x++) {
                for (int i = 0; i < sourceDataArrays.length; i++) {
                    sourceSamples[i] = sourceBands[i].scaleInverse(sourceDataArrays[i][sourcePixelOffset]);
                }
                calculator.calculate(sourceSamples, posteriors, indexFilter);
                targetDataArray[targetPixelOffset] = findMaxIndex(posteriors);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private static int findMaxIndex(double[] posteriors) {
        int index = 0;

        for (int i = 1; i < posteriors.length; ++i) {
            if (posteriors[i] > posteriors[index]) {
                index = i;
            }
        }

        return index;
    }
}
