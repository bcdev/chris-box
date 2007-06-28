/* $Id: $
 *
 * Copyright (C) 2002-2007 by Brockmann Consult
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

import static java.lang.Math.acos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.esa.beam.chris.operators.internal.LocalRegressionSmoothing;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import com.bc.ceres.core.ProgressMonitor;

/**
 * Operator for calculating the vertical striping correction factors for noise
 * due to the CCD elements.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 */
public class DestripingFactorsOp extends AbstractOperator {

    @SourceProducts
    Product[] sourceProducts;
    @TargetProduct
    Product targetProduct;

    @Parameter
    int smoothingOrder;

    private double edgeDetectionThreshold;
    private int spectralBandCount;

    private transient Map<String, Double> thresholdMap;
    private transient LocalRegressionSmoothing smoother;

    private transient Band[] targetBands;
    private transient Band[][] sourceDataBands;
    private transient Band[][] sourceMaskBands;
    private transient Panorama panorama;
    private boolean[][] edgeMask;

    /**
     * Creates an instance of this class.
     *
     * @param spi the operator service provider interface.
     */
    public DestripingFactorsOp(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor pm) throws OperatorException {
        thresholdMap = new HashMap<String, Double>(6);
        thresholdMap.put("1", 0.08);
        thresholdMap.put("2", 0.05);
        thresholdMap.put("3", 0.08);
        thresholdMap.put("3A", 0.08);
        thresholdMap.put("4", 0.08);
        thresholdMap.put("5", 0.08);

        spectralBandCount = getAnnotationInt(sourceProducts[0], ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        // set up source bands
        sourceDataBands = new Band[spectralBandCount][sourceProducts.length];
        sourceMaskBands = new Band[spectralBandCount][sourceProducts.length];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String dataBandName = new StringBuilder("radiance_").append(i + 1).toString();
            final String maskBandName = new StringBuilder("mask_").append(i + 1).toString();

            for (int j = 0; j < sourceProducts.length; ++j) {
                sourceDataBands[i][j] = sourceProducts[j].getBand(dataBandName);
                sourceMaskBands[i][j] = sourceProducts[j].getBand(maskBandName);

                if (sourceDataBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("could not find band {0}", dataBandName));
                }
                if (sourceMaskBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("could not find band {0}", maskBandName));
                }
            }
        }

        panorama = new Panorama(sourceProducts);
        edgeDetectionThreshold = getEdgeDetectionThreshold();
        edgeMask = createEdgeMask(pm);
        smoother = new LocalRegressionSmoothing(2, smoothingOrder, 2);

        // set up target product and bands
        targetProduct = new Product("VSC", "CHRIS_VSC", sourceProducts[0].getSceneRasterWidth(), 1);
        targetBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            targetBands[i] = targetProduct.addBand(
                    new StringBuilder("vs_corr_").append(i + 1).toString(), ProductData.TYPE_FLOAT64);

            targetBands[i].setSpectralBandIndex(i + 1);
            targetBands[i].setDescription(MessageFormat.format(
                    "Vertical striping correction factors for radiance band {0}", i + 1));
            pm.worked(1);
        }

        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        final RasterDataNode node = targetRaster.getRasterDataNode();

        for (int i = 0; i < targetBands.length; ++i) {
            if (targetBands[i].equals(node)) {
                computeCorrectionFactors(i, targetRaster.getRectangle(), pm);
                return;
            }
        }
    }

    @Override
    public void dispose() {
        smoother = null;
        edgeMask = null;
    }

    /**
     * Computes the vertical striping correction factors for a single target band.
     *
     * @param bandIndex       the band index.
     * @param targetRectangle the target rectangle.
     * @param pm              the {@link ProgressMonitor}.
     *
     * @throws OperatorException
     */
    private void computeCorrectionFactors(int bandIndex, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {
            pm.beginTask("computing corection factors", panorama.height + 5);

            // 1. Accumulate the across-track spatial derivative profile
            final double[] p = new double[panorama.width];
            final int[] count = new int[panorama.width];

            for (int j = 0, panoramaY = 0; j < sourceProducts.length; ++j) {
                final Rectangle sourceRectangle = panorama.getRectangle(j);
                final Raster data = getTile(sourceDataBands[bandIndex][j], sourceRectangle);
                final Raster mask = getTile(sourceMaskBands[bandIndex][j], sourceRectangle);

                for (int sceneY = 0; sceneY < sourceRectangle.height; ++sceneY, ++panoramaY) {
                    for (int x = 1; x < panorama.width; ++x) {
                        if (!edgeMask[panoramaY][x] && mask.getInt(x, sceneY) == 0 && mask.getInt(x - 1, sceneY) == 0) {
                            p[x] += log(data.getDouble(x, sceneY) / data.getDouble(x - 1, sceneY));
                            ++count[x];
                        }
                    }
                    pm.worked(1);
                }
            }
            // 2. Compute the average profile
            for (int x = 1; x < panorama.width; ++x) {
                if (count[x] > 0) {
                    p[x] /= count[x];
                } else {
                    p[x] = p[x - 1];
                }
            }
            pm.worked(1);
            // 3. Compute the integrated profile
            for (int x = 1; x < panorama.width; ++x) {
                p[x] += p[x - 1];
            }
            pm.worked(1);
            // 4. Smooth the integrated profile to get rid of small-scale variations (noise)
            final double[] s = new double[panorama.width];
            smoother.smooth(p, s);
            pm.worked(1);
            // 5. Compute the noise profile
            double meanNoise = 0.0;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= s[x];
                meanNoise += p[x];
            }
            meanNoise /= panorama.width;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= meanNoise;
            }
            pm.worked(1);
            // 6. Compute the correction factors
            final Raster targetRaster = getTile(targetBands[bandIndex], targetRectangle);
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                targetRaster.setDouble(x, 0, exp(-p[x]));
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    /**
     * Creates the spatio-spectral edge mask for a hyperspectral image.
     *
     * @param pm the {@link ProgressMonitor}.
     *
     * @return the edge mask. The value {@code true} indicates changes in the surface
     *         texture or coverage.
     *
     * @throws OperatorException
     */
    private boolean[][] createEdgeMask(ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("creating edge mask", spectralBandCount + panorama.width + 2);

            // 1. Compute the squares and across-track scalar products of the spectral vectors
            final double[][] sad = new double[panorama.width][panorama.height];
            final double[][] sca = new double[panorama.width][panorama.height];

            for (final Band[] bands : sourceDataBands) {
                for (int i = 0, panoramaY = 0; i < bands.length; i++) {
                    final Rectangle sourceRectangle = panorama.getRectangle(i);
                    final Raster data = getTile(bands[i], sourceRectangle);

                    for (int sceneY = 0; sceneY < sourceRectangle.height; ++sceneY, ++panoramaY) {
                        double r1 = data.getDouble(0, sceneY);
                        sad[0][panoramaY] += r1 * r1;

                        for (int x = 1; x < panorama.width; ++x) {
                            final double r2 = data.getDouble(x, sceneY);

                            sca[x][panoramaY] += r2 * r1;
                            sad[x][panoramaY] += r2 * r2;
                            r1 = r2;
                        }
                    }
                }
                pm.worked(1);
            }
            // 2. Compute the across-track spectral angle differences
            for (int y = 0; y < panorama.height; ++y) {
                double norm1 = sqrt(sad[0][y]);
                sad[0][y] = 0.0;

                for (int x = 1; x < panorama.width; ++x) {
                    final double norm2 = sqrt(sad[x][y]);

                    sad[x][y] = acos(sca[x][y] / (norm1 * norm2));
                    norm1 = norm2;
                }
            }
            pm.worked(1);

            final int minIndex = (int) (0.60 * panorama.height);
            final int maxIndex = (int) (0.80 * panorama.height);

            double minThreshold = 0.0;
            double maxThreshold = 0.0;

            // 3. Adjust the edge-detection threshold
            for (int x = 1; x < panorama.width; ++x) {
                final double[] values = Arrays.copyOf(sad[x], panorama.height);
                Arrays.sort(values);

                minThreshold = max(minThreshold, values[minIndex]);
                maxThreshold = max(maxThreshold, values[maxIndex]);
                pm.worked(1);
            }
            final double threshold = min(max(edgeDetectionThreshold, minThreshold), maxThreshold);

            // 4. Create the edge mask
            final boolean[][] edgeMask = new boolean[panorama.height][panorama.width];
            for (int y = 0; y < panorama.height; ++ y) {
                for (int x = 1; x < panorama.width; ++x) {
                    if (sad[x][y] > threshold) {
                        edgeMask[y][x] = true;
                    }
                }
            }
            pm.worked(1);

            return edgeMask;
        } finally {
            pm.done();
        }
    }

    private double getEdgeDetectionThreshold() throws OperatorException {
        final String mode = getAnnotationString(sourceProducts[0], ChrisConstants.ATTR_NAME_CHRIS_MODE);

        if (thresholdMap.containsKey(mode)) {
            return thresholdMap.get(mode);
        } else {
            throw new OperatorException(MessageFormat.format(
                    "could not get edge detection threshold because CHRIS Mode ''{0}'' is not known", mode));
        }
    }

    // todo -- move
    private static int getAnnotationInt(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation or {@code null} if the annotation could not be found.
     *
     * @throws OperatorException if the annotation could not be read.
     */
    // todo -- move
    private static String getAnnotationString(Product product, String name) throws OperatorException {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        String string = null;

        if (metadataRoot != null) {
            final MetadataElement mph = metadataRoot.getElement(ChrisConstants.MPH_NAME);

            if (mph != null) {
                string = mph.getAttributeString(name, null);
            }
        }
        if (string == null) {
            throw new OperatorException(MessageFormat.format("could not read CHRIS annotation ''{0}''", name));
        }

        return string;
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(DestripingFactorsOp.class, "DestripingFactors");
            // todo -- set description etc.
        }
    }


    /**
     * Image panorama.
     */
    private static class Panorama {

        public int height;
        public int width;

        private Rectangle[] rectangles;

        public Panorama(Product[] products) throws OperatorException {
            int height = products[0].getSceneRasterHeight();
            int width = products[0].getSceneRasterWidth();

            for (int i = 1; i < products.length; ++i) {
                if (width != products[i].getSceneRasterWidth()) {
                    throw new OperatorException("input products have inconsistent raster widths");
                }
                height += products[i].getSceneRasterHeight();
            }

            this.height = height;
            this.width = width;
            rectangles = new Rectangle[products.length];

            for (int i = 0; i < products.length; ++i) {
                rectangles[i] = new Rectangle(0, 0, width, products[i].getSceneRasterHeight());
            }
        }

        public Rectangle getRectangle(int i) {
            return rectangles[i];
        }
    }

}
