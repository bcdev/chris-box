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

        assertEquals(0, raster.getSample(0, 0, 0)); // red < nir
        assertEquals(1, raster.getSample(1, 0, 0)); // red > nir
        assertEquals(0, raster.getSample(0, 1, 0)); // red > nir - but not within bounds
        assertEquals(0, raster.getSample(1, 1, 0)); // red < nir
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);

        final Band redBand = addBand(product, "red", new int[]{1, 2, 3, 1});
        final Band nirBand = addBand(product, "nir", new int[]{2, 1, 2, 2});

        return WaterMaskOpImage.createImage(redBand, nirBand, 40.0 * Math.PI, 20.0 * Math.PI, 0.0);
    }

    private static Band addBand(Product product, String name, int[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT32);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
