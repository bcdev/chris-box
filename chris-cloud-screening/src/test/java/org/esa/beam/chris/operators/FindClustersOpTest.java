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

import java.io.IOException;
import java.util.Random;

/**
 * Tests for class {@link FindClustersOp}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class FindClustersOpTest extends TestCase {

    public void testClusters() throws IOException {
        final Product sourceProduct = new Product("F", "FT", 8, 8);

        final double[] values = {
                4, 4, 4, 1, 1, 1, 1, 1,
                4, 4, 4, 1, 1, 1, 1, 1,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
                2, 2, 2, 3, 3, 3, 3, 3,
        };
        final int[] expectedMemberships = {
                3, 3, 3, 2, 2, 2, 2, 2,
                3, 3, 3, 2, 2, 2, 2, 2,
                1, 1, 1, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, 0, 0,
                1, 1, 1, 0, 0, 0, 0, 0,
        };
        final Random random = new Random(5489);
        for (int i = 0; i < values.length; i++) {
            values[i] += 0.01 * random.nextGaussian();
        }
        addSourceBand(sourceProduct, "features", values);

        final String[] sourceBandNames = {"features"};
        final FindClustersOp op = new FindClustersOp(sourceProduct, sourceBandNames, 4, 10, 0.1);

        final Product targetProduct = op.getTargetProduct();
        assertEquals(5, targetProduct.getNumBands());

        op.setClusterCount(4);
        op.setClusterDistance(0.1);
        final Band[] targetBands = targetProduct.getBands();
        final int[] memberships = targetBands[4].readPixels(0, 0, 8, 8, new int[64]);

        for (int i = 0; i < memberships.length; i++) {
            assertEquals("index: " + i, expectedMemberships[i], memberships[i]);
        }
    }

    private static Band addSourceBand(Product product, String name, double[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_FLOAT64);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));
        band.setImage(new RasterDataNodeOpImage(band));

        return band;
    }
}
