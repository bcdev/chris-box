package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link CloudMaskOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class CloudMaskOpImageTest extends TestCase {
    
    private static final int W = 3;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(0x000, raster.getSample(0, 0, 0)); // ok
        assertEquals(0x001, raster.getSample(1, 0, 0)); // uncorrected dropout
        assertEquals(0x002, raster.getSample(2, 0, 0)); // saturation
        assertEquals(0x100, raster.getSample(0, 1, 0)); // corrected dropout
        assertEquals(0x400, raster.getSample(1, 1, 0)); // cloud
        assertEquals(0x402, raster.getSample(2, 1, 0)); // cloud and saturation
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);
        final Band[] maskBands = new Band[4];

        maskBands[0] = addBand(product, "mask_0", new short[]{0x000, 0x000, 0x000, 0x100, 0x000, 0x000});
        maskBands[1] = addBand(product, "mask_1", new short[]{0x000, 0x001, 0x000, 0x000, 0x000, 0x000});
        maskBands[2] = addBand(product, "mask_2", new short[]{0x000, 0x000, 0x002, 0x000, 0x000, 0x002});
        maskBands[3] = addBand(product, "mask_3", new short[]{0x000, 0x000, 0x000, 0x000, 0x000, 0x000});
        final Band cloudProductBand = addBand(product, "cloud_product", new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 1.0});

        return CloudMaskOpImage.createImage(maskBands, cloudProductBand, 0.5);
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
