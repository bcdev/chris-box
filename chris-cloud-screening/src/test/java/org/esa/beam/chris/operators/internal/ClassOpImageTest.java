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
package org.esa.beam.chris.operators.internal;

import junit.framework.TestCase;
import org.esa.beam.cluster.Distribution;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link ClassOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ClassOpImageTest extends TestCase {

    private static final IndexFilter NO_FILTERING = new IndexFilter() {
        @Override
        public boolean accept(int index) {
            return true;
        }
    };

    public void testComputation() {
        final Product product = createTestProduct();
        final Distribution[] distributions = new Distribution[4];

        distributions[0] = new StandardMultinormalDistribution(new double[]{10.0, 10.0, 10.0, 10.0});
        distributions[1] = new StandardMultinormalDistribution(new double[]{20.0, 20.0, 20.0, 20.0});
        distributions[2] = new StandardMultinormalDistribution(new double[]{30.0, 30.0, 30.0, 30.0});
        distributions[3] = new StandardMultinormalDistribution(new double[]{40.0, 40.0, 40.0, 40.0});

        final double[] priors = {1.0, 1.0, 1.0, 1.0};
        final ProbabilityCalculator calculator = new ProbabilityCalculator(distributions, priors);

        final RenderedImage image = ClassOpImage.createImage(product.getBands(), calculator, NO_FILTERING, 4);
        final Raster data = image.getData();

        assertEquals(0, data.getSample(0, 0, 0));
        assertEquals(3, data.getSample(1, 0, 0));
        assertEquals(2, data.getSample(0, 1, 0));
        assertEquals(1, data.getSample(1, 1, 0));
    }

    private static Product createTestProduct() {
        final Product product = new Product("Features", "Features", 2, 2);

        addSourceBand(product, "feature_0", new short[]{101, 401, 301, 201});
        addSourceBand(product, "feature_1", new short[]{102, 402, 302, 202});
        addSourceBand(product, "feature_2", new short[]{103, 403, 303, 203});
        addSourceBand(product, "feature_3", new short[]{104, 404, 304, 204});

        return product;
    }

    private static Band addSourceBand(Product product, String name, short[] samples) {
        final Band band = product.addBand(name, ProductData.TYPE_INT16);
        band.setScalingFactor(1.0 / 10.0);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(samples));

        return band;
    }

    private static class StandardMultinormalDistribution implements Distribution {
        private final double[] mean;

        public StandardMultinormalDistribution(double[] mean) {
            this.mean = mean;
        }

        @Override
        public final double probabilityDensity(double[] y) {
            return Math.exp(logProbabilityDensity(y));
        }

        @Override
        public final double logProbabilityDensity(double[] y) {
            if (y.length != mean.length) {
                throw new IllegalArgumentException("y.length != mean.length");
            }

            return -0.5 * squaredDistance(y);
        }

        private double squaredDistance(double[] y) {
            double u = 0.0;

            for (int i = 0; i < mean.length; ++i) {
                u += ((y[i] - mean[i]) * (y[i] - mean[i]));
            }

            return u;
        }
    }
}
