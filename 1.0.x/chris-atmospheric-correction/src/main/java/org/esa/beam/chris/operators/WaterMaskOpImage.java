package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.jai.BandOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Water mask image.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class WaterMaskOpImage extends PointOpImage {

    private static final double RED_LOWER_BOUND = 0.01;
    private static final double NIR_LOWER_BOUND = 0.01;
    private static final double NIR_UPPER_BOUND = 0.1;

    private final double redScaling;
    private final double nirScaling;

    /**
     * Creates the water mask image.
     *
     * @param redBand    the red TOA radiance band.
     * @param nirBand    the NIR TOA radiance band.
     * @param redScaling the scaling factor for obtaining TOA reflectances from TOA radiances for the red.
     * @param nirScaling the scaling factor for obtaining TOA reflectances from TOA radiances for the NIR.
     *
     * @return the water mask image.
     */
    public static OpImage createImage(Band redBand, Band nirBand, double redScaling, double nirScaling) {
        RenderedImage redImage = redBand.getSourceImage();
        if (redImage == null) {
            redImage = new BandOpImage(redBand);
            redBand.setSourceImage(redImage);
        }
        RenderedImage nirImage = nirBand.getSourceImage();
        if (nirImage == null) {
            nirImage = new BandOpImage(nirBand);
            nirBand.setSourceImage(nirImage);
        }

        int w = redBand.getRasterWidth();
        int h = redBand.getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new WaterMaskOpImage(redImage, nirImage, imageLayout, redScaling, nirScaling);
    }

    private WaterMaskOpImage(RenderedImage redImage, RenderedImage nirImage, ImageLayout imageLayout,
                             double redScaling, double nirScaling) {
        super(redImage, nirImage, imageLayout, null, true);

        this.redScaling = redScaling;
        this.nirScaling = nirScaling;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor redAccessor = new PixelAccessor(getSourceImage(0));
        final PixelAccessor nirAccessor = new PixelAccessor(getSourceImage(1));

        final UnpackedImageData redData = redAccessor.getPixels(sources[0], rectangle, DataBuffer.TYPE_INT, false);
        final UnpackedImageData nirData = nirAccessor.getPixels(sources[1], rectangle, DataBuffer.TYPE_INT, false);

        final int[] redPixels = redData.getIntData(0);
        final int[] nirPixels = nirData.getIntData(0);

        final PixelAccessor targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        final UnpackedImageData targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_BYTE, true);
        final byte[] targetPixels = targetData.getByteData(0);

        int redLineOffset = redData.bandOffsets[0];
        int nirLineOffset = nirData.bandOffsets[0];
        int targetLineOffset = targetData.bandOffsets[0];

        for (int y = 0; y < rectangle.height; ++y) {
            int redPixelOffset = redLineOffset;
            int nirPixelOffset = nirLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < rectangle.width; ++x) {
                double red = redPixels[redPixelOffset];
                double nir = nirPixels[nirPixelOffset];

                if (red > nir) {
                    red *= redScaling;
                    nir *= nirScaling;

                    if (red > RED_LOWER_BOUND && nir > NIR_LOWER_BOUND && nir < NIR_UPPER_BOUND) {
                        targetPixels[targetPixelOffset] = 1;
                    }
                }

                redPixelOffset += redData.pixelStride;
                nirPixelOffset += nirData.pixelStride;
                targetPixelOffset += targetData.pixelStride;
            }

            redLineOffset += redData.lineStride;
            nirLineOffset += nirData.lineStride;
            targetLineOffset += targetData.lineStride;
        }

        targetAccessor.setPixels(targetData);
    }
}
