package org.esa.beam.chris.operators;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link WaterMaskOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class WaterMaskOpImageTest extends TestCase {

    private static final int W = 2;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(0, raster.getSample(0, 0, 0)); // red < NIR
        assertEquals(1024, raster.getSample(1, 0, 0)); // red > NIR
        assertEquals(0, raster.getSample(0, 1, 0)); // red > NIR - but NIR too big
        assertEquals(0, raster.getSample(1, 1, 0)); // red > NIR - but NIR too small
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);

        final Band redBand = addBand(product, "red", new int[]{20, 40, 180, 20});
        final Band nirBand = addBand(product, "nir", new int[]{40, 20, 160, 10});

        return WaterMaskOpImage.createImage(redBand, nirBand, 0.001, 0.001);
    }

    private static Band addBand(Product product, String name, int[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT32);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
