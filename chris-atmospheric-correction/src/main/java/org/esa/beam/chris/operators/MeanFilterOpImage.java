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

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.UntiledOpImage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
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

        final PixelAccessor sourceAccessor = new PixelAccessor(getSourceImage(0));
        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final PixelAccessor hyperMaskAccessor = new PixelAccessor(getSourceImage(1));
        final PixelAccessor cloudMaskAccessor = new PixelAccessor(getSourceImage(2));

        final UnpackedImageData sourceData = sourceAccessor.getPixels(sources[0], rectangle,
                                                                      DataBuffer.TYPE_SHORT, false);
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle,
                                                                      DataBuffer.TYPE_SHORT, true);
        final UnpackedImageData hyperMaskData = hyperMaskAccessor.getPixels(sources[0], rectangle,
                                                                            DataBuffer.TYPE_BYTE, false);
        final UnpackedImageData cloudMaskData = cloudMaskAccessor.getPixels(sources[1], rectangle,
                                                                            DataBuffer.TYPE_BYTE, false);

        final short[] sourcePixels = sourceData.getShortData(0);
        final short[] targetPixels = targetData.getShortData(0);
        final byte[] hyperMaskPixels = hyperMaskData.getByteData(0);
        final byte[] cloudMaskPixels = cloudMaskData.getByteData(0);

        int targetLineOffset = targetData.bandOffsets[0];

        final int w = rectangle.width;
        final int h = rectangle.height;
        final int halfKernelSize = kernelSize / 2;

        for (int y = 0; y < h; ++y) {
            int targetPixelOffset = targetLineOffset;

            final int yMin = Math.max(0, y - halfKernelSize);
            final int yMax = Math.min(h, y + halfKernelSize);

            for (int x = 0; x < w; ++x) {
                final int xMin = Math.max(0, x - halfKernelSize);
                final int xMax = Math.min(w, x + halfKernelSize);

                int hyperMask = hyperMaskPixels[targetPixelOffset];
                int cloudMask = cloudMaskPixels[targetPixelOffset];

                if ((hyperMask & 3) == 0 && cloudMask == 0) {
                    int sum = 0;
                    int count = 0;

                    int sourceLineOffset = sourceData.bandOffsets[0] + yMin * sourceData.lineStride;

                    for (int kernelY = yMin; kernelY < yMax; ++kernelY) {
                        int sourcePixelOffset = sourceLineOffset + xMin * sourceData.pixelStride;

                        for (int kernelX = xMin; kernelX < xMax; ++kernelX) {

                            hyperMask = hyperMaskPixels[sourcePixelOffset];
                            cloudMask = cloudMaskPixels[sourcePixelOffset];

                            if ((hyperMask & 3) == 0 && cloudMask == 0) {
                                sum += sourcePixels[sourcePixelOffset];
                                count++;
                            }
                            sourcePixelOffset += sourceData.pixelStride;
                        }
                        sourceLineOffset += sourceData.lineStride;
                    }
                    if (count > 0) {
                        targetPixels[targetPixelOffset] = (short) (sum / count);
                    }
                }

                targetPixelOffset += targetData.pixelStride;
            }

            targetLineOffset += targetData.lineStride;
        }

        targetAccessor.setPixels(targetData);
    }
}
