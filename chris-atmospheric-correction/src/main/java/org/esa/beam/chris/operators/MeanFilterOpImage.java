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
import java.util.Map;
import java.util.Vector;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class MeanFilterOpImage extends UntiledOpImage {
    private final int kernelSize;

    public static OpImage createImage(Band band, RenderedImage hyperMaskImage, RenderedImage cloudMaskImage, int
            kernelSize) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        sourceImageVector.add(band.getImage());
        sourceImageVector.add(hyperMaskImage);
        sourceImageVector.add(cloudMaskImage);

        int w = hyperMaskImage.getWidth();
        int h = hyperMaskImage.getHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_DOUBLE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new MeanFilterOpImage(sourceImageVector, null, imageLayout, kernelSize);
    }

    private MeanFilterOpImage(Vector<RenderedImage> sourceImageVector, Map map, ImageLayout imageLayout,
                              int kernelSize) {
        super(sourceImageVector, map, imageLayout);

        this.kernelSize = kernelSize;
    }

    @Override
    protected void computeImage(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor hyperMaskAccessor;
        final PixelAccessor cloudMaskAccessor;

        final UnpackedImageData hyperMaskData;
        final UnpackedImageData cloudMaskData;

        final byte[] hyperMaskPixels;
        final byte[] cloudMaskPixels;

        hyperMaskAccessor = new PixelAccessor(getSourceImage(1));
        cloudMaskAccessor = new PixelAccessor(getSourceImage(2));

        hyperMaskData = hyperMaskAccessor.getPixels(sources[0], rectangle, DataBuffer.TYPE_BYTE, false);
        cloudMaskData = cloudMaskAccessor.getPixels(sources[1], rectangle, DataBuffer.TYPE_BYTE, false);

        hyperMaskPixels = hyperMaskData.getByteData(0);
        cloudMaskPixels = cloudMaskData.getByteData(0);

        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_SHORT, true);
        final short[] targetPixels = targetData.getShortData(0);

        int hyperMaskLineOffset = hyperMaskData.bandOffsets[0];
        int cloudMaskLineOffset = cloudMaskData.bandOffsets[0];

        final int w = rectangle.width;
        final int h = rectangle.height;

        for (int y = 0; y < h; ++y) {
            int hyperMaskPixelOffset = hyperMaskLineOffset;
            int cloudMaskPixelOffset = cloudMaskLineOffset;

            final int minY = Math.max(0, y - kernelSize);
            final int maxY = Math.min(h, y + kernelSize);

            for (int x = 0; x < w; ++x) {
                final int minX = Math.max(0, x - kernelSize);
                final int maxX = Math.min(w, x + kernelSize);

                int hyperMask = hyperMaskPixels[hyperMaskPixelOffset];
                int cloudMask = cloudMaskPixels[cloudMaskPixelOffset];

                if ((hyperMask & 3) == 0 && cloudMask == 0) {
                    double sum = 0.0;
                    int count = 0;

                    int targetLineOffset = targetData.bandOffsets[0] + minY * targetData.lineStride;

                    for (int kernelY = minY; kernelY < maxY; ++kernelY) {
                        int targetPixelOffset = targetLineOffset + minX * targetData.pixelStride;

                        for (int kernelX = minX; kernelX < maxX; ++kernelX) {

                            hyperMask = hyperMaskPixels[hyperMaskPixelOffset];
                            cloudMask = cloudMaskPixels[cloudMaskPixelOffset];

                            if ((hyperMask & 3) == 0 && cloudMask == 0) {
                                sum += targetPixels[targetPixelOffset];
                                count++;
                            }
                            targetPixelOffset += targetData.pixelStride;
                        }
                        targetLineOffset += targetData.lineStride;
                    }
//                    image[x][y] = sum / count;
                }

                hyperMaskPixelOffset += hyperMaskData.pixelStride;
                cloudMaskPixelOffset += cloudMaskData.pixelStride;
            }

            hyperMaskLineOffset += hyperMaskData.lineStride;
            cloudMaskLineOffset += cloudMaskData.lineStride;
//            targetLineOffset += targetData.lineStride;
        }
    }
}
