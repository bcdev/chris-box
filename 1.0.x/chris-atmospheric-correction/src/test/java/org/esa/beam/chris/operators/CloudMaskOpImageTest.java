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

    private static final int W = 2;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(0, raster.getSample(0, 0, 0));
        assertEquals(0, raster.getSample(1, 0, 0));
        assertEquals(0, raster.getSample(0, 1, 0));
        assertEquals(1, raster.getSample(1, 1, 0));
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);

        final Band cloudProductBand = addBand(product, "cloud_product", new double[]{0.0, 0.0, 0.5, 1.0});

        return CloudMaskOpImage.createImage(cloudProductBand, 0.5);
    }

    private static Band addBand(Product product, String name, double[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT64);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
