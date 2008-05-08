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

import org.esa.beam.framework.datamodel.Band;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ClusterProbabilityOpImage extends PointOpImage {

    private final int correspondingBandIndex;
    private final int[] backgroundBandIndexes;

    public static ClusterProbabilityOpImage create(final ImageLayout imageLayout,
                                                   final Band[] sourceBands,
                                                   final int correspondingBandIndex,
                                                   final int[] backgroundBandIndexes) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        for (final Band sourceBand : sourceBands) {
            sourceImageVector.add(sourceBand.getImage());
        }

        return new ClusterProbabilityOpImage(sourceImageVector, imageLayout, correspondingBandIndex, backgroundBandIndexes);
    }

    private ClusterProbabilityOpImage(Vector<RenderedImage> sourceImageVector,
                                      ImageLayout layout,
                                      int correspondingBandIndex,
                                      int[] backgroundBandIndexes) {
        super(sourceImageVector, layout, null, true);

        this.correspondingBandIndex = correspondingBandIndex;
        this.backgroundBandIndexes = backgroundBandIndexes;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final RasterFormatTag[] formatTags = getFormatTags();
        final RasterAccessor[] sourceAccessors = new RasterAccessor[sources.length];

        for (int i = 0; i < sources.length; ++i) {
            sourceAccessors[i] =
                    new RasterAccessor(sources[i], rectangle, formatTags[i], getSourceImage(i).getColorModel());
        }

        final RasterAccessor targetAccessor =
                new RasterAccessor(target, rectangle, formatTags[sources.length], getColorModel());

        switch (targetAccessor.getDataType()) {
            case DataBuffer.TYPE_FLOAT:
                floatLoop(sourceAccessors, targetAccessor);
                break;
            case DataBuffer.TYPE_DOUBLE:
                doubleLoop(sourceAccessors, targetAccessor);
                break;
        }

        if (targetAccessor.isDataCopy()) {
            targetAccessor.clampDataArrays();
            targetAccessor.copyDataToRaster();
        }
    }

    private void floatLoop(RasterAccessor[] sources, RasterAccessor target) {
        final float[] targetDataArray = target.getFloatDataArray(0);
        final int targetBandOffset = target.getBandOffset(0);
        final int targetPixelStride = target.getPixelStride();
        final int targetScanlineStride = target.getScanlineStride();

        final float[][] sourceDataArrays = new float[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            sourceDataArrays[i] = sources[i].getFloatDataArray(0);
        }

        int sourceBandOffset = sources[0].getBandOffset(0);
        int sourcePixelStride = sources[0].getPixelStride();
        int sourceScanlineStride = sources[0].getScanlineStride();

        int sourceScanlineOffset = sourceBandOffset;
        int targetScanlineOffset = targetBandOffset;

        for (int y = 0; y < target.getHeight(); y++) {
            int sourcePixelOffset = sourceScanlineOffset;
            int targetPixelOffset = targetScanlineOffset;

            for (int x = 0; x < target.getWidth(); x++) {

                final float[] sourceSamples = new float[sourceDataArrays.length];
                for (int i = 0; i < sourceDataArrays.length; i++) {
                    sourceSamples[i] = sourceDataArrays[i][sourcePixelOffset];
                }
                targetDataArray[targetPixelOffset] = renormalize(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private void doubleLoop(RasterAccessor[] sources, RasterAccessor target) {
        final double[] targetDataArray = target.getDoubleDataArray(0);
        final int targetBandOffset = target.getBandOffset(0);
        final int targetPixelStride = target.getPixelStride();
        final int targetScanlineStride = target.getScanlineStride();

        final double[][] sourceDataArrays = new double[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            sourceDataArrays[i] = sources[i].getDoubleDataArray(0);
        }

        int sourceBandOffset = sources[0].getBandOffset(0);
        int sourcePixelStride = sources[0].getPixelStride();
        int sourceScanlineStride = sources[0].getScanlineStride();

        int sourceScanlineOffset = sourceBandOffset;
        int targetScanlineOffset = targetBandOffset;

        for (int y = 0; y < target.getHeight(); y++) {
            int sourcePixelOffset = sourceScanlineOffset;
            int targetPixelOffset = targetScanlineOffset;

            for (int x = 0; x < target.getWidth(); x++) {

                final double[] sourceSamples = new double[sourceDataArrays.length];
                for (int i = 0; i < sourceDataArrays.length; i++) {
                    sourceSamples[i] = sourceDataArrays[i][sourcePixelOffset];
                }
                targetDataArray[targetPixelOffset] = renormalize(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private float renormalize(float[] samples) {
        if (isContained(correspondingBandIndex, backgroundBandIndexes)) {
            return 0.0f;
        }

        float sum = 0.0f;
        for (int i = 0; i < samples.length; ++i) {
            if (!isContained(i, backgroundBandIndexes)) {
                sum += samples[i];
            }
        }

        return samples[correspondingBandIndex] / sum;
    }

    private double renormalize(double[] samples) {
        if (isContained(correspondingBandIndex, backgroundBandIndexes)) {
            return 0.0;
        }

        double sum = 0.0;
        for (int i = 0; i < samples.length; ++i) {
            if (!isContained(i, backgroundBandIndexes)) {
                sum += samples[i];
            }
        }

        if (sum > 0.0) {
            return samples[correspondingBandIndex] / sum;
        } else {
            return 0.0;
        }
    }

    private static boolean isContained(int index, int[] indexes) {
        for (final int i : indexes) {
            if (i == index) {
                return true;
            }
        }

        return false;
    }
}
