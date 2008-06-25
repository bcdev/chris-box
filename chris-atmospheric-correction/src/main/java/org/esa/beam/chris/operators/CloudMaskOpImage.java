package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

/**
 * Cloud mask image indicating clouds and the quality of the spectrum.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CloudMaskOpImage extends PointOpImage {

    private final double cloudProductThreshold;

    /**
     * Creates a cloud mask image from given CHRIS mask bands and the cloud product
     * band. The resulting image is obtained by logically or-ing the mask bands and
     * setting bit 10 when the cloud product exceeds the threshold.
     *
     * @param maskBands             the mask bands.
     * @param cloudProductBand      the cloud product band.
     * @param cloudProductThreshold the cloud product threshold.
     *
     * @return the cloud mask image.
     */
    public static RenderedImage createImage(Band[] maskBands, Band cloudProductBand, double cloudProductThreshold) {
        final Collection<Band> sourceBandList = new ArrayList<Band>(maskBands.length + 1);
        Collections.addAll(sourceBandList, cloudProductBand);
        Collections.addAll(sourceBandList, maskBands);

        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>(maskBands.length + 1);
        for (final Band band : sourceBandList) {
            RenderedImage image = band.getImage();
            if (image == null) {
                image = new RasterDataNodeOpImage(band);
            }
            sourceImageVector.add(image);
        }

        int w = cloudProductBand.getRasterWidth();
        int h = cloudProductBand.getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_SHORT, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new CloudMaskOpImage(sourceImageVector, imageLayout, cloudProductThreshold);
    }

    private CloudMaskOpImage(Vector<RenderedImage> sourceImageVector, ImageLayout imageLayout,
                             double cloudProductThreshold) {
        super(sourceImageVector, imageLayout, null, true);

        this.cloudProductThreshold = cloudProductThreshold;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_SHORT, true);
        final short[] targetPixels = targetData.getShortData(0);

        computeCloudMask(sources, target, rectangle, targetData, targetPixels);

        for (int i = 1; i < sources.length; ++i) {
            final PixelAccessor sourceAccessor = new PixelAccessor(getSourceImage(i));
            final UnpackedImageData sourceData = sourceAccessor.getPixels(sources[i], rectangle, DataBuffer.TYPE_SHORT,
                                                                          false);
            final short[] sourcePixels = sourceData.getShortData(0);

            int sourceLineOffset = sourceData.bandOffsets[0];
            int targetLineOffset = targetData.bandOffsets[0];

            for (int y = 0; y < target.getHeight(); ++y) {
                int sourcePixelOffset = sourceLineOffset;
                int targetPixelOffset = targetLineOffset;

                for (int x = 0; x < target.getWidth(); ++x) {
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

    private void computeCloudMask(Raster[] sources, WritableRaster target, Rectangle rectangle,
                                  UnpackedImageData targetData, short[] targetPixels) {
        final PixelAccessor sourceAccessor = new PixelAccessor(getSourceImage(0));
        final UnpackedImageData sourceData = sourceAccessor.getPixels(sources[0], rectangle, DataBuffer.TYPE_DOUBLE,
                                                                      false);
        final double[] sourcePixels = sourceData.getDoubleData(0);

        int sourceLineOffset = sourceData.bandOffsets[0];
        int targetLineOffset = targetData.bandOffsets[0];

        for (int y = 0; y < target.getHeight(); ++y) {
            int sourcePixelOffset = sourceLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < target.getWidth(); ++x) {
                if (sourcePixels[sourcePixelOffset] > cloudProductThreshold) {
                    targetPixels[targetPixelOffset] = 0x400; // set bit 10
                }

                sourcePixelOffset += sourceData.pixelStride;
                targetPixelOffset += targetData.pixelStride;
            }

            sourceLineOffset += sourceData.lineStride;
            targetLineOffset += targetData.lineStride;
        }
    }
}
