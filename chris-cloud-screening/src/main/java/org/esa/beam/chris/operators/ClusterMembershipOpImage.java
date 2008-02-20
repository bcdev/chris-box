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
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Vector;
import java.util.Arrays;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ClusterMembershipOpImage extends PointOpImage {
    private final int[] ignoreBandIds;

    public static ClusterMembershipOpImage create(Band[] sourceBands, Band targetBand, int[] ignoreBandIds) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();
        for (final Band sourceBand : sourceBands) {
            sourceImageVector.add(sourceBand.getImage());
        }

        return new ClusterMembershipOpImage(sourceImageVector, targetBand, ignoreBandIds);
    }

    public ClusterMembershipOpImage(Vector<RenderedImage> sourceImageVector, Band targetBand, int[] ignoreBandIds) {
        super(sourceImageVector, RasterDataNodeOpImage.createSingleBandedImageLayout(targetBand), null, true);

         this.ignoreBandIds = ignoreBandIds;
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
            case DataBuffer.TYPE_BYTE:
                byteLoop(sourceAccessors, targetAccessor);
                break;
            case DataBuffer.TYPE_INT:
                intLoop(sourceAccessors, targetAccessor);
                break;
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

    private static void byteLoop(RasterAccessor[] sources, RasterAccessor target) {
        final byte[] targetDataArray = target.getByteDataArray(0);
        final int targetBandOffset = target.getBandOffset(0);
        final int targetPixelStride = target.getPixelStride();
        final int targetScanlineStride = target.getScanlineStride();

        final byte[][] sourceDataArrays = new byte[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            sourceDataArrays[i] = sources[i].getByteDataArray(0);
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
                targetDataArray[targetPixelOffset] = (byte) indexMax(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private static void intLoop(RasterAccessor[] sources, RasterAccessor target) {
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

        for (int y = 0; y < target.getHeight(); y++) {
            int sourcePixelOffset = sourceScanlineOffset;
            int targetPixelOffset = targetScanlineOffset;

            for (int x = 0; x < target.getWidth(); x++) {

                final double[] sourceSamples = new double[sourceDataArrays.length];
                for (int i = 0; i < sourceDataArrays.length; i++) {
                    sourceSamples[i] = sourceDataArrays[i][sourcePixelOffset];
                }
                targetDataArray[targetPixelOffset] = indexMax(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private static void floatLoop(RasterAccessor[] sources, RasterAccessor target) {
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

                final double[] sourceSamples = new double[sourceDataArrays.length];
                for (int i = 0; i < sourceDataArrays.length; i++) {
                    sourceSamples[i] = sourceDataArrays[i][sourcePixelOffset];
                }
                targetDataArray[targetPixelOffset] = indexMax(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private static void doubleLoop(RasterAccessor[] sources, RasterAccessor target) {
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
                targetDataArray[targetPixelOffset] = indexMax(sourceSamples);

                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }

    private static int indexMax(double[] samples) {
        int index = 0;
        for (int i = 1; i < samples.length; ++i) {
//            if (ignoreBandIds.contains(i))  {
//                continue;
//            }
            if (samples[i] > samples[index]) {
                index = i;
            }
        }

        return index;
    }
}
