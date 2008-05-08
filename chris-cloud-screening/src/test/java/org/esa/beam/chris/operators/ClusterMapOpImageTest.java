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

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.ImageLayout;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

public class ClusterMapOpImageTest extends TestCase {

    public void testComputationByteByte() {
        final RenderedImage image = createTestImage(ProductData.TYPE_INT8, ProductData.TYPE_INT8);
        assertMembership(image);
    }

    public void testComputationByteShort() {
        final RenderedImage image = createTestImage(ProductData.TYPE_INT8, ProductData.TYPE_INT16);
        assertMembership(image);
    }

    public void testComputationByteFloat() {
        final RenderedImage image = createTestImage(ProductData.TYPE_INT8, ProductData.TYPE_FLOAT32);
        assertMembership(image);
    }

    public void testComputationIntInt() {
        final RenderedImage image = createTestImage(ProductData.TYPE_INT32, ProductData.TYPE_INT32);
        assertMembership(image);
    }

    public void testComputationFloatFloat() {
        final RenderedImage image = createTestImage(ProductData.TYPE_FLOAT32, ProductData.TYPE_FLOAT32);
        assertMembership(image);
    }

    private static void assertMembership(RenderedImage image) {
        final Raster data = image.getData();

        assertEquals(3, data.getSample(0, 0, 0));
        assertEquals(0, data.getSample(1, 0, 0));
        assertEquals(1, data.getSample(0, 1, 0));
        assertEquals(2, data.getSample(1, 1, 0));
    }

    private RenderedImage createTestImage(int sourceType, int targetType) {
        final Product product = new Product("C", "CT", 2, 2);
        final Band[] sourceBands = new Band[4];
        final Band targetBand = product.addBand("membership_mask", targetType);

        switch (sourceType) {
            case ProductData.TYPE_INT8:
                sourceBands[0] = addSourceBand(product, "probability_0", new byte[]{1, 4, 3, 2});
                sourceBands[1] = addSourceBand(product, "probability_1", new byte[]{2, 1, 4, 3});
                sourceBands[2] = addSourceBand(product, "probability_2", new byte[]{3, 2, 1, 4});
                sourceBands[3] = addSourceBand(product, "probability_3", new byte[]{4, 3, 2, 1});
                break;
            case ProductData.TYPE_INT32:
                sourceBands[0] = addSourceBand(product, "probability_0", new int[]{1, 4, 3, 2});
                sourceBands[1] = addSourceBand(product, "probability_1", new int[]{2, 1, 4, 3});
                sourceBands[2] = addSourceBand(product, "probability_2", new int[]{3, 2, 1, 4});
                sourceBands[3] = addSourceBand(product, "probability_3", new int[]{4, 3, 2, 1});
                break;
            case ProductData.TYPE_FLOAT32:
                sourceBands[0] = addSourceBand(product, "probability_0", new float[]{1, 4, 3, 2});
                sourceBands[1] = addSourceBand(product, "probability_1", new float[]{2, 1, 4, 3});
                sourceBands[2] = addSourceBand(product, "probability_2", new float[]{3, 2, 1, 4});
                sourceBands[3] = addSourceBand(product, "probability_3", new float[]{4, 3, 2, 1});
                break;
        }

        final ImageLayout imageLayout = RasterDataNodeOpImage.createSingleBandedImageLayout(targetBand);
        return ClusterMapOpImage.create(imageLayout, sourceBands);
    }

    private static Band addSourceBand(Product product, String name, byte[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT8);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));
        band.setImage(new RasterDataNodeOpImage(band));

        return band;
    }

    private static Band addSourceBand(Product product, String name, int[] vales) {
        final Band band = product.addBand(name, ProductData.TYPE_INT32);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(vales));
        band.setImage(new RasterDataNodeOpImage(band));

        return band;
    }

    private static Band addSourceBand(Product product, String name, float[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT32);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));
        band.setImage(new RasterDataNodeOpImage(band));

        return band;
    }
}
