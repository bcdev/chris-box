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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.BandOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Cloud mask image.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CloudMaskOpImage extends PointOpImage {

    private final double cloudProductThreshold;

    /**
     * Creates a cloud mask image.
     *
     * @param cloudProductBand      the cloud product band.
     * @param cloudProductThreshold the cloud product threshold.
     *
     * @return the cloud mask image.
     */
    public static OpImage createImage(Band cloudProductBand, double cloudProductThreshold) {
        RenderedImage cloudProductImage = cloudProductBand.getSourceImage();
        if (cloudProductImage == null) {
            cloudProductImage = new BandOpImage(cloudProductBand);
            cloudProductBand.setSourceImage(cloudProductImage);
        }

        int w = cloudProductImage.getWidth();
        int h = cloudProductImage.getHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new CloudMaskOpImage(cloudProductImage, imageLayout, cloudProductThreshold);
    }

    private CloudMaskOpImage(RenderedImage cloudProductImage, ImageLayout imageLayout, double cloudProductThreshold) {
        super(cloudProductImage, imageLayout, null, true);

        this.cloudProductThreshold = cloudProductThreshold;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor;
        final UnpackedImageData targetData;
        final byte[] targetPixels;

        targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_BYTE, true);
        targetPixels = targetData.getByteData(0);

        final PixelAccessor sourceAccessor;
        final UnpackedImageData sourceData;
        final double[] sourcePixels;

        sourceAccessor = new PixelAccessor(getSourceImage(0));
        sourceData = sourceAccessor.getPixels(sources[0], rectangle, DataBuffer.TYPE_DOUBLE, false);
        sourcePixels = sourceData.getDoubleData(0);

        int sourceLineOffset = sourceData.bandOffsets[0];
        int targetLineOffset = targetData.bandOffsets[0];

        for (int y = 0; y < rectangle.height; ++y) {
            int sourcePixelOffset = sourceLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < rectangle.width; ++x) {
                if (sourcePixels[sourcePixelOffset] > cloudProductThreshold) {
                    targetPixels[targetPixelOffset] = 1;
                }

                sourcePixelOffset += sourceData.pixelStride;
                targetPixelOffset += targetData.pixelStride;
            }

            sourceLineOffset += sourceData.lineStride;
            targetLineOffset += targetData.lineStride;
        }

        targetAccessor.setPixels(targetData);
    }
}
