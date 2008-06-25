package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Water mask image indicating water surface.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class WaterMaskOpImage extends PointOpImage {

    private static final double RED_LOWER_BOUND = 0.01;
    private static final double NIR_LOWER_BOUND = 0.01;
    private static final double NIR_UPPER_BOUND = 0.1;

    private final double redScalingFactor;
    private final double nirScalingFactor;

    /**
     * Creates the water mask image.
     *
     * @param redBand the red band.
     * @param nirBand the NIR band.
     * @param redIrr  the solar irradiance in the red.
     * @param nirIrr  the solar irradiance in the NIR.
     * @param sza     the solar zenith angle.
     *
     * @return the water mask image.
     */
    public static RenderedImage createImage(Band redBand, Band nirBand, double redIrr, double nirIrr, double sza) {
        RenderedImage redImage = redBand.getImage();
        if (redImage == null) {
            redImage = new RasterDataNodeOpImage(redBand);
        }
        RenderedImage nirImage = redBand.getImage();
        if (nirImage == null) {
            nirImage = new RasterDataNodeOpImage(nirBand);
        }

        int w = redBand.getRasterWidth();
        int h = redBand.getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        final double redScalingFactor = Math.PI / Math.cos(Math.toRadians(sza)) / redIrr;
        final double nirScalingFactor = Math.PI / Math.cos(Math.toRadians(sza)) / nirIrr;

        return new WaterMaskOpImage(redImage, nirImage, imageLayout, redScalingFactor, nirScalingFactor);
    }

    private WaterMaskOpImage(RenderedImage redImage, RenderedImage nirImage, ImageLayout imageLayout,
                             double redScalingFactor, double nirScalingFactor) {
        super(redImage, nirImage, imageLayout, null, true);

        this.redScalingFactor = redScalingFactor;
        this.nirScalingFactor = nirScalingFactor;
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

        for (int y = 0; y < target.getHeight(); ++y) {
            int redPixelOffset = redLineOffset;
            int nirPixelOffset = nirLineOffset;
            int targetPixelOffset = targetLineOffset;

            for (int x = 0; x < target.getWidth(); ++x) {
                double red = redPixels[redPixelOffset];
                double nir = nirPixels[nirPixelOffset];

                if (redPixels[redPixelOffset] > nirPixels[nirPixelOffset]) {
                    red *= redScalingFactor;
                    nir *= nirScalingFactor;

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
