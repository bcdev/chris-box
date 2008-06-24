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

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;

/**
 * Creates a spectrum mask image from all CHRIS mask bands.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class SpectrumMaskOpImage extends PointOpImage {

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
            RenderedImage image = maskBand.getImage();
            if (image == null) {
                image = new RasterDataNodeOpImage(maskBand);
            }
            sourceImageVector.add(image);
        }

        int w = maskBands[0].getRasterWidth();
        int h = maskBands[1].getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_SHORT, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new SpectrumMaskOpImage(imageLayout, sourceImageVector);
    }

    private SpectrumMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector) {
        super(sourceImageVector, imageLayout, null, true);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_SHORT, true);
        final short[] targetPixels = targetData.getShortData(0);

        for (int i = 0; i < sources.length; ++i) {
            final PixelAccessor sourceAccessor = new PixelAccessor(getSourceImage(i));
            final UnpackedImageData sourceData = sourceAccessor.getPixels(sources[i], rectangle, DataBuffer.TYPE_SHORT,
                                                                          false);
            final short[] sourcePixels = sourceData.getShortData(0);

            int sourceLineOffset = sourceData.bandOffsets[0];
            int targetLineOffset = targetData.bandOffsets[0];

            for (int y = 0; y < target.getHeight(); y++) {
                int sourcePixelOffset = sourceLineOffset;
                int targetPixelOffset = targetLineOffset;

                for (int x = 0; x < target.getWidth(); x++) {
                    targetPixels[targetPixelOffset] |= sourcePixels[sourcePixelOffset];

                    sourcePixelOffset += sourceData.pixelStride;
                    targetPixelOffset += targetData.pixelStride;
                }

                sourceLineOffset += sourceData.lineStride;
                targetLineOffset += targetData.lineStride;
            }
        }

        targetAccessor.setPixels(targetData);
    }
}
