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

import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import javax.media.jai.PixelAccessor;
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

    private final Band[] featureBands;
    private final ProbabilityCalculator calculator;
    private final IndexFilter validClusterFilter;
    private final IndexFilter cloudClusterFilter;
    private final int clusterCount;
    private final boolean binaryMask;

    public static OpImage createProbabilisticImage(Band[] featureBands, EMCluster[] clusters,
                                                   IndexFilter validClusterFilter, IndexFilter cloudClusterFilter) {
        final ProbabilityCalculator calculator = Clusterer.createProbabilityCalculator(clusters);

        return createImage(featureBands, calculator, validClusterFilter, cloudClusterFilter, clusters.length, false);
    }

    public static OpImage createBinaryImage(Band[] featureBands, EMCluster[] clusters, IndexFilter validClusterFilter,
                                            IndexFilter cloudClusterFilter) {
        final ProbabilityCalculator calculator = Clusterer.createProbabilityCalculator(clusters);

        return createImage(featureBands, calculator, validClusterFilter, cloudClusterFilter, clusters.length, true);
    }

    /*
     * For unit-level testing, which is easier with a calculator 
     * instead of a clusters array.
     */
    static OpImage createImage(Band[] featureBands, ProbabilityCalculator calculator, IndexFilter clusterFilter,
                               IndexFilter cloudClusterFilter, int clusterCount, boolean binaryMask) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        for (final Band band : featureBands) {
            RenderedImage sourceImage = band.getImage();
            if (sourceImage == null) {
                sourceImage = new RasterDataNodeOpImage(band);
                band.setImage(sourceImage);
            }
            sourceImageVector.add(sourceImage);
        }

        final int w = sourceImageVector.get(0).getWidth();
        final int h = sourceImageVector.get(0).getHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_DOUBLE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new CloudMaskOpImage(imageLayout, sourceImageVector, featureBands, calculator, clusterFilter,
                                    cloudClusterFilter, clusterCount, binaryMask);
    }

    private CloudMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector,
                             Band[] featureBands, ProbabilityCalculator calculator,
                             IndexFilter validClusterFilter, IndexFilter cloudClusterFilter,
                             int clusterCount, boolean binaryMask) {
        super(sourceImageVector, imageLayout, new RenderingHints(JAI.KEY_TILE_CACHE, null), true);

        this.featureBands = featureBands;
        this.calculator = calculator;
        this.validClusterFilter = validClusterFilter;
        this.cloudClusterFilter = cloudClusterFilter;
        this.clusterCount = clusterCount;
        this.binaryMask = binaryMask;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor;
        final UnpackedImageData targetData;
        final double[] targetPixels;

        targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_DOUBLE, true);
        targetPixels = targetData.getDoubleData(0);

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
                    sourceSamples[i] = featureBands[i].scale(sourcePixels[i][sourcePixelOffset]);
                }
                calculator.calculate(sourceSamples, posteriors, validClusterFilter);

                final double cloudProbability = accumulateCloudProbabilities(posteriors);
                if (binaryMask) {
                    if (cloudProbability > 0.5) {
                        targetPixels[targetPixelOffset] = 1.0;
                    }
                } else {
                    targetPixels[targetPixelOffset] = cloudProbability;
                }

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceLineOffset += sourceLineStride;
            targetLineOffset += targetLineStride;
        }

        targetAccessor.setPixels(targetData);
    }

    private double accumulateCloudProbabilities(double[] posteriors) {
        double sum = 0.0;

        for (int i = 0; i < posteriors.length; ++i) {
            if (cloudClusterFilter.accept(i)) {
                sum += posteriors[i];
            }
        }

        return sum;
    }
}
