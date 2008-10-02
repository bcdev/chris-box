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
package org.esa.beam.chris.operators.internal;

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
public class CloudMaskOpImage extends PointOpImage {

    private static final double CLOUD_PROBABILITY_THRESHOLD = 0.5;
    private static final short CLOUD_PROBABILITY_SCALING = 10000;

    private final Band[] sourceBands;
    private final ProbabilityCalculator calculator;
    private final IndexFilter clusterFilter;
    private final IndexFilter cloudClusterFilter;
    private final int clusterCount;

    public static OpImage createImage(Band[] sourceBands, ProbabilityCalculator calculator, IndexFilter clusterFilter,
                                      IndexFilter cloudClusterFilter, int clusterCount) {
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

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_SHORT, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new CloudMaskOpImage(imageLayout, sourceImageVector, sourceBands, calculator, clusterFilter,
                cloudClusterFilter, clusterCount);
    }

    private CloudMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector,
                             Band[] sourceBands, ProbabilityCalculator calculator, IndexFilter clusterFilter,
                             IndexFilter cloudClusterFilter, int clusterCount) {
        super(sourceImageVector, imageLayout, null, true);

        this.sourceBands = sourceBands;
        this.calculator = calculator;
        this.clusterFilter = clusterFilter;
        this.cloudClusterFilter = cloudClusterFilter;
        this.clusterCount = clusterCount;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor;
        final UnpackedImageData targetData;
        final short[] targetPixels;

        targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_SHORT, true);
        targetPixels = targetData.getShortData(0);

        final PixelAccessor[] sourceAccessors = new PixelAccessor[sources.length];
        final UnpackedImageData[] sourceData = new UnpackedImageData[sources.length];
        final short[][] sourcePixels = new short[sources.length][];

        for (int i = 0; i < sources.length; ++i) {
            sourceAccessors[i] = new PixelAccessor(getSourceImage(i));
            sourceData[i] = sourceAccessors[i].getPixels(sources[i], rectangle, DataBuffer.TYPE_SHORT, false);
            sourcePixels[i] = sourceData[i].getShortData(0);
        }

        final int sourceBandOffset = sourceData[0].bandOffsets[0];
        final int targetBandOffset = targetData.bandOffsets[0];

        final int sourcePixelStride = sourceData[0].pixelStride;
        final int targetPixelStride = targetData.pixelStride;

        final int sourceLineStride = sourceData[0].lineStride;
        final int targetLineStride = targetData.lineStride;

        int sourceLineOffset = sourceBandOffset;
        int targetLineOffset = targetBandOffset;

        final double[] sourceSamples = new double[sources.length];
        final double[] posteriors = new double[clusterCount];

        for (int y = 0; y < target.getHeight(); y++) {
            int sourcePixelOffset = sourceLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < target.getWidth(); x++) {
                for (int i = 0; i < sources.length; i++) {
                    sourceSamples[i] = sourceBands[i].scale(sourcePixels[i][sourcePixelOffset]);
                }
                calculator.calculate(sourceSamples, posteriors, clusterFilter);
                targetPixels[targetPixelOffset] = accumulateCloudProbabilities(posteriors);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceLineOffset += sourceLineStride;
            targetLineOffset += targetLineStride;
        }

        targetAccessor.setPixels(targetData);
    }

    private short accumulateCloudProbabilities(double[] posteriors) {
        double sum = 0.0;

        for (int i = 0; i < posteriors.length; ++i) {
            if (cloudClusterFilter.accept(i)) {
                sum += posteriors[i];
            }
        }
        if (sum < CLOUD_PROBABILITY_THRESHOLD) {
            return 0;
        } else {
            return CLOUD_PROBABILITY_SCALING;
        }
    }
}
