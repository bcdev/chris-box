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

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;

/**
 * Creates a saturation mask image from CHRIS mask bands.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class SaturationMaskOpImage extends PointOpImage {

    /**
     * Creates a saturation mask image from the CHRIS mask bands supplied.
     *
     * @param maskBands the mask bands.
     *
     * @return the saturation mask.
     */
    public static RenderedImage createImage(Band[] maskBands) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        for (final Band maskBand : maskBands) {
            sourceImageVector.add(maskBand.getImage());
        }

        int w = maskBands[0].getRasterWidth();
        int h = maskBands[1].getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_SHORT, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new SaturationMaskOpImage(imageLayout, sourceImageVector);
    }

    private SaturationMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector) {
        super(sourceImageVector, imageLayout, null, true);
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

        shortLoop(sourceAccessors, targetAccessor);

        if (targetAccessor.isDataCopy()) {
            targetAccessor.clampDataArrays();
            targetAccessor.copyDataToRaster();
        }
    }

    private static void shortLoop(RasterAccessor[] sources, RasterAccessor target) {
        final short[] targetDataArray = target.getShortDataArray(0);
        final int targetBandOffset = target.getBandOffset(0);
        final int targetPixelStride = target.getPixelStride();
        final int targetScanlineStride = target.getScanlineStride();

        final short[][] sourceDataArrays = new short[sources.length][];
        for (int i = 0; i < sources.length; i++) {
            sourceDataArrays[i] = sources[i].getShortDataArray(0);
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
                for (final short[] sourceDataArray : sourceDataArrays) {
                    final short sourceSample = sourceDataArray[sourcePixelOffset];
                    if (sourceSample == 2) {
                        targetDataArray[targetPixelOffset] = 1;
                        break;
                    }
                }
                sourcePixelOffset += sourcePixelStride;
                targetPixelOffset += targetPixelStride;
            }

            sourceScanlineOffset += sourceScanlineStride;
            targetScanlineOffset += targetScanlineStride;
        }
    }
}
