package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link ValidMaskOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ValidMaskOpImageTest extends TestCase {
    private static final int W = 3;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(1, raster.getSample(0, 0, 0)); // ok
        assertEquals(0, raster.getSample(1, 0, 0)); // uncorrected dropout
        assertEquals(0, raster.getSample(2, 0, 0)); // saturation
        assertEquals(1, raster.getSample(0, 1, 0)); // corrected dropout
        assertEquals(0, raster.getSample(1, 1, 0)); // cloud
        assertEquals(0, raster.getSample(2, 1, 0)); // cloud
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);
        final Band[] maskBands = new Band[4];

        maskBands[0] = addBand(product, "mask_0", new short[]{0, 0, 0, 256, 0, 0});
        maskBands[1] = addBand(product, "mask_1", new short[]{0, 1, 0, 0, 0, 0});
        maskBands[2] = addBand(product, "mask_2", new short[]{0, 0, 2, 0, 0, 2});
        maskBands[3] = addBand(product, "mask_3", new short[]{0, 0, 0, 0, 0, 0});
        final Band cloudProductBand = addBand(product, "cloud_product", new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 1.0});

        return ValidMaskOpImage.createImage(0.5, cloudProductBand, maskBands);
    }

    private static Band addBand(Product product, String name, short[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT16);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }

    private static Band addBand(Product product, String name, double[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT64);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
