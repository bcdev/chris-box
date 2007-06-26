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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import static java.lang.Math.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for calculating the vertical striping correction factors for noise
 * due to the CCD elements.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public class VerticalStripingCorrectionOperator extends AbstractOperator {

    @SourceProducts
    Product[] sourceProducts;
    @TargetProduct
    Product targetProduct;

    @Parameter
    int smoothingOrder;

    private double edgeDetectionThreshold;
    private int spectralBandCount;

    private transient Map<String, Double> thresholdMap;
    private transient LocalRegressionSmoothing smoothing;

    private transient Band[] targetBands;
    private transient Band[][] sourceDataBands;
    private transient Band[][] sourceMaskBands;
    private transient Panorama panorama;
    private transient boolean[][] edgeMask;

    /**
     * Creates an instance of this class.
     *
     * @param spi the operator service provider interface.
     */
    public VerticalStripingCorrectionOperator(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor pm) throws OperatorException {
        // initialization
        thresholdMap = new HashMap<String, Double>(6);
        thresholdMap.put("1", 0.08);
        thresholdMap.put("2", 0.05);
        thresholdMap.put("3", 0.08);
        thresholdMap.put("3A", 0.08);
        thresholdMap.put("4", 0.08);
        thresholdMap.put("5", 0.08);

        smoothing = new LocalRegressionSmoothing(2, smoothingOrder, 2);

        setEdgeDetectionThreshold();
        setSpectralBandCount();

        // set up source bands
        sourceDataBands = new Band[spectralBandCount][sourceProducts.length];
        sourceMaskBands = new Band[spectralBandCount][sourceProducts.length];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String dataBandName = MessageFormat.format("radiance_{0}", i + 1);
            final String maskBandName = MessageFormat.format("mask_{0}", i + 1);

            for (int j = 0; j < sourceProducts.length; ++j) {
                sourceDataBands[i][j] = sourceProducts[j].getBand(dataBandName);
                sourceMaskBands[i][j] = sourceProducts[j].getBand(maskBandName);

                if (sourceDataBands == null) {
                    throw new OperatorException("");
                    // todo - message
                }
                if (sourceMaskBands == null) {
                    throw new OperatorException("");
                    // todo - message
                }
            }
        }

        // create panorama
        panorama = new Panorama(sourceProducts);

        // set up target product
        targetProduct = new Product("product_name", "product_type", sourceProducts[0].getSceneRasterWidth(), 1);
        targetBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            targetBands[i] = targetProduct.addBand(
                    MessageFormat.format("vs_correction_{0}", i + 1), ProductData.TYPE_FLOAT64);

            targetBands[i].setSpectralBandIndex(i + 1);
            targetBands[i].setDescription(
                    MessageFormat.format("Vertical striping correction factors for radiance band {0}", i + 1));
        }

        return targetProduct;
    }

    @Override
    public void computeTiles(Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
    	System.out.println("Compute: "+targetRectangle.toString());
        try {
            pm.beginTask("computing correction factors", 2 * spectralBandCount + 3);
            edgeMask = createEdgeMask(new SubProgressMonitor(pm, spectralBandCount + 3));

            for (int i = 0; i < spectralBandCount; ++i) {
            	System.out.println("***** BandNr:"+i);
                computeCorrectionFactors(sourceDataBands[i], sourceMaskBands[i], targetBands[i], targetRectangle);
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Computes the vertical striping correction factors for a single target band.
     *
     * @param sourceDataBands the source data bands.
     * @param sourceMaskBands the source mask bands.
     * @param targetBand      the target band.
     * @param targetRectangle the target rectangle.
     *
     * @throws OperatorException
     */
    private void computeCorrectionFactors(Band[] sourceDataBands,
                                          Band[] sourceMaskBands,
                                          Band targetBand,
                                          Rectangle targetRectangle)
            throws OperatorException {
        final int colCount = panorama.getWidth();

        // 1. Compute the average across-track spatial derivative profile
        final double[] p = new double[colCount];
        int[] count = new int[colCount]; 
        int panY = 0;
        for (int i = 0; i < sourceProducts.length; ++i) {
            final Raster data = getTile(sourceDataBands[i], panorama.getRectangle(i));
            final Raster mask = getTile(sourceMaskBands[i], panorama.getRectangle(i));

            for (int col = 1; col < colCount; ++col) {
                for (int sceneY = 0; sceneY < panorama.getHeight(i); ++sceneY) {
                    if (!edgeMask[panY+sceneY][col] && mask.getInt(col, sceneY) == 0 && mask.getInt(col - 1, sceneY) == 0) {
                        p[col] += log(data.getDouble(col, sceneY) / data.getDouble(col - 1, sceneY));
                        ++count[col];
                    }
                }
                if (count[col] > 0) {
                    p[col] /= count[col];
                } else {
                    p[col] = p[col - 1];
                }
            }
            panY += panorama.getHeight(i);
        }
        // 2. Compute the integrated profile
        for (int col = 1; col < colCount; ++col) {
            p[col] += p[col - 1];
        }
        // 3. Smooth the integrated profile to get rid of small-scale variations (noise)
        final double[] s = new double[colCount];
        smoothing.smooth(p, s);
        // 4. Compute the noise profile
        double meanNoise = 0.0;
        for (int col = 0; col < colCount; ++col) {
            p[col] -= s[col];
            meanNoise += p[col];
        }
        meanNoise /= colCount;
        for (int col = 0; col < colCount; ++col) {
            p[col] -= meanNoise;
        }
        // 5. Compute the correction factors
        final Raster targetRaster = getTile(targetBand, targetRectangle);
        for (int col = targetRectangle.x; col < targetRectangle.x + targetRectangle.width; ++col) {
            targetRaster.setDouble(col, 0, exp(-p[col]));
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
        final int rowCount = panorama.getHeight();
        final int colCount = panorama.getWidth();

        try {
            pm.beginTask("creating spatio-spectral edge mask", spectralBandCount + 3);

            // 1. Compute the squares and across-track scalar products of the spectral vectors
            final double[][] sad = new double[colCount][rowCount];
            final double[][] sca = new double[colCount][rowCount];

            for (final Band[] bands : sourceDataBands) {
                for (int panY = 0, i = 0; i < bands.length; ++i) {
                    final Raster data = getTile(bands[i], panorama.getRectangle(i));

                    for (int sceneY = 0; sceneY < data.getHeight(); ++sceneY, ++panY) {
                        double r1 = data.getDouble(0, sceneY);
                        sad[0][panY] += r1 * r1;

                        for (int col = 1; col < colCount; ++col) {
                            final double r2 = data.getDouble(col, sceneY);

                            sca[col][panY] += r2 * r1;
                            sad[col][panY] += r2 * r2;
                            r1 = r2;
                        }
                    }
                }
                pm.worked(1);
            }
            // 2. Compute the across-track spectral angle differences
            for (int row = 0; row < rowCount; ++row) {
                double norm1 = sqrt(sad[0][row]);
                sad[0][row] = 0.0;

                for (int col = 1; col < colCount; ++col) {
                    final double norm2 = sqrt(sad[col][row]);

                    sad[col][row] = acos(sca[col][row] / (norm1 * norm2));
                    norm1 = norm2;
                }
            }
            pm.worked(1);

            final int minIndex = (int) (0.60 * rowCount);
            final int maxIndex = (int) (0.80 * rowCount);

            double minThreshold = 0.0;
            double maxThreshold = 0.0;

            // 3. Adjust the edge-detection threshold
            for (int col = 1; col < colCount; ++col) {
                final double[] values = Arrays.copyOf(sad[col], rowCount);
                Arrays.sort(values);

                minThreshold = max(minThreshold, values[minIndex]);
                maxThreshold = max(maxThreshold, values[maxIndex]);
            }
            final double threshold = min(max(edgeDetectionThreshold, minThreshold), maxThreshold);
            pm.worked(1);

            // 4. Create the edge mask
            final boolean[][] edge = new boolean[rowCount][colCount];
            for (int col = 1; col < colCount; ++col) {
                for (int row = 0; row < rowCount; ++ row) {
                    if (sad[col][row] > threshold) {
                        edge[row][col] = true;
                    }
                }
            }
            pm.worked(1);

            return edge;
        } finally {
            pm.done();
        }
    }

    private void setEdgeDetectionThreshold() throws OperatorException {
        final String mode = getChrisAnnotation(sourceProducts[0], ChrisConstants.ATTR_NAME_CHRIS_MODE);

        if (mode == null) {
            throw new OperatorException("");
            // todo - message
        }
        if (thresholdMap.containsKey(mode)) {
            edgeDetectionThreshold = thresholdMap.get(mode);
        } else {
            throw new OperatorException("");
            // todo - message
        }
    }

    private void setSpectralBandCount() throws OperatorException {
        final String annotation = getChrisAnnotation(sourceProducts[0], ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        if (annotation == null) {
            throw new OperatorException("");
            // todo - message
        }
        try {
            spectralBandCount = Integer.parseInt(annotation);
        } catch (NumberFormatException e) {
            throw new OperatorException("", e);
            // todo - message
        }
    }

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation or {@code null} if the annotation could not be found.
     */
    private static String getChrisAnnotation(Product product, String name) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        String annotation = null;

        if (metadataRoot != null) {
            final MetadataElement mph = metadataRoot.getElement(ChrisConstants.MPH_NAME);

            if (mph != null) {
                annotation = mph.getAttributeString(name, null);
            }
        }

        return annotation;
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(VerticalStripingCorrectionOperator.class, "VerticalStripingCorrection");
        }
    }


    /**
     * Image panorama.
     */
    private static class Panorama {

        private int height;
        private Rectangle[] rectangles;

        public Panorama(Product[] products) throws OperatorException {
            int height = products[0].getSceneRasterHeight();
            int width = products[0].getSceneRasterWidth();

            for (int i = 1; i < products.length; ++i) {
                if (width != products[i].getSceneRasterWidth()) {
                    throw new OperatorException("");
                    // todo -- message
                }
                height += products[i].getSceneRasterHeight();
            }

            this.height = height;
            rectangles = new Rectangle[products.length];

            for (int i = 0; i < products.length; ++i) {
                rectangles[i] = new Rectangle(0, 0, width, products[i].getSceneRasterHeight());
            }
        }

        public int getHeight() {
            return height;
        }

        public int getHeight(int i) {
            return rectangles[i].height;
        }

        public int getWidth() {
            return rectangles[0].width;
        }

        public Rectangle getRectangle(int i) {
            return rectangles[i];
        }
    }

}
