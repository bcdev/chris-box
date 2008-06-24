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
 * Creates a valid-pixel mask image.
 * <p/>
 * A pixel is considered as valid if its cloud product value is below the threshold and
 * its mask pixel values do not indicate saturation.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ValidMaskOpImage extends PointOpImage {

    private final double cloudProductThreshold;

    /**
     * Creates a valid-pixel mask image from the cloud product threshold, the cloud product
     * band and the CHRIS mask bands supplied.
     * <p/>
     * A pixel is considered as valid if its cloud product value is below the threshold and
     * its mask pixel values do not indicate uncorrected dropouts or saturation.
     *
     * @param cloudProductThreshold the cloud product threshold.
     * @param cloudProductBand      the cloud product band.
     * @param maskBands             the mask bands.
     *
     * @return the saturation mask.
     */
    public static RenderedImage createImage(double cloudProductThreshold, Band cloudProductBand, Band[] maskBands) {
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

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new ValidMaskOpImage(imageLayout, sourceImageVector, cloudProductThreshold);
    }

    private ValidMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector,
                             double cloudProductThreshold) {
        super(sourceImageVector, imageLayout, null, true);

        this.cloudProductThreshold = cloudProductThreshold;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_SHORT, true);
        final short[] targetPixels = targetData.getShortData(0);

        computeSurfaceMask(sources, target, rectangle, targetData, targetPixels);

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
                    if (targetPixels[targetPixelOffset] == 1) {
                        if ((sourcePixels[sourcePixelOffset] != 0 && sourcePixels[sourcePixelOffset] != 256)) {
                            // uncorrected dropout or saturated
                            targetPixels[targetPixelOffset] = 0;
                        }
                    }

                    sourcePixelOffset += sourceData.pixelStride;
                    targetPixelOffset += targetData.pixelStride;
                }

                sourceLineOffset += sourceData.lineStride;
                targetLineOffset += targetData.lineStride;
            }
        }

        targetAccessor.setPixels(targetData);
    }

    private void computeSurfaceMask(Raster[] sources, WritableRaster target, Rectangle rectangle,
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
                if (sourcePixels[sourcePixelOffset] <= cloudProductThreshold) { // surface pixel, not cloud
                    targetPixels[targetPixelOffset] = 1;
                }

                sourcePixelOffset += sourceData.pixelStride;
                targetPixelOffset += targetData.pixelStride;
            }

            sourceLineOffset += sourceData.lineStride;
            targetLineOffset += targetData.lineStride;
        }
    }
}
