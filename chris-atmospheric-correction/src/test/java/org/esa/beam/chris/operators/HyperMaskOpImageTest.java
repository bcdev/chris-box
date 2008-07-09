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

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link HyperMaskOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class HyperMaskOpImageTest extends TestCase {

    private static final int W = 2;
    private static final int H = 2;

    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        assertEquals(0, raster.getSample(0, 0, 0));
        assertEquals(1, raster.getSample(1, 0, 0));
        assertEquals(2, raster.getSample(0, 1, 0));
        assertEquals(3, raster.getSample(1, 1, 0));
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);

        addBand(product, "mask_0", new short[]{0, 0, 0, 2});
        addBand(product, "mask_1", new short[]{0, 1, 0, 2});
        addBand(product, "mask_2", new short[]{0, 0, 2, 0});
        addBand(product, "mask_3", new short[]{0, 0, 0, 1});

        return HyperMaskOpImage.createImage(product.getBands());
    }

    private static Band addBand(Product product, String name, short[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT16);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
