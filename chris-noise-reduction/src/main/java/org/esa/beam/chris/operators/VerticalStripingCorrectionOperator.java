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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
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
 * The operator implementation.
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

    public VerticalStripingCorrectionOperator(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        // initialization
        thresholdMap = new HashMap<String, Double>(6);
        thresholdMap.put("CHRIS_M1", 0.08);
        thresholdMap.put("CHRIS_M2", 0.05);
        thresholdMap.put("CHRIS_M3", 0.08);
        thresholdMap.put("CHRIS_M3A", 0.08);
        thresholdMap.put("CHRIS_M4", 0.08);
        thresholdMap.put("CHRIS_M5", 0.08);

        smoothing = new LocalRegressionSmoothing(2, smoothingOrder, 2);

        setEdgeDetectionThreshold();
        setSpectralBandCount();

        // check source products for consistency
        for (int j = 1; j < sourceProducts.length; ++j) {
            if (sourceProducts[0].getSceneRasterWidth() != sourceProducts[j].getSceneRasterWidth()) {
                throw new OperatorException("");
                // todo - message
            }
        }

        // set up source bands
        sourceDataBands = new Band[sourceProducts.length][spectralBandCount];
        sourceMaskBands = new Band[sourceProducts.length][spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String dataBandName = MessageFormat.format("radiance_{0}", i + 1);
            final String maskBandName = MessageFormat.format("mask_{0}", i + 1);

            for (int j = 0; j < sourceProducts.length; ++j) {
                sourceDataBands[j][i] = sourceProducts[j].getBand(dataBandName);
                sourceMaskBands[j][i] = sourceProducts[j].getBand(maskBandName);

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
    public void computeTiles(Rectangle targetRectangle, ProgressMonitor progressMonitor) throws OperatorException {
        final Raster[] sourceData = new PanoramaRaster[spectralBandCount];
        final Raster[] sourceMask = new PanoramaRaster[spectralBandCount];
        final Raster[] targetData = new Raster[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            final Raster[] data = new Raster[sourceProducts.length];
            final Raster[] mask = new Raster[sourceProducts.length];

            for (int j = 0; j < sourceProducts.length; ++j) {
                final Rectangle sourceRectangle = new Rectangle(0, 0, sourceProducts[j].getSceneRasterWidth(),
                                                                sourceProducts[j].getSceneRasterHeight());

                data[j] = getTile(sourceDataBands[j][i], sourceRectangle);
                mask[j] = getTile(sourceMaskBands[j][i], sourceRectangle);
            }

            sourceData[i] = new PanoramaRaster(data);
            sourceMask[i] = new PanoramaRaster(mask);
            targetData[i] = getTile(targetBands[i], targetRectangle);
        }

        computeCorrectionFactors(sourceData, sourceMask, targetData, targetRectangle);
    }

    public void dispose() {
        // todo - add any clean-up code here, the targetProduct is disposed by the framework
    }

    private void computeCorrectionFactors(Raster[] data, Raster[] mask, Raster[] target, Rectangle targetRectangle) {
        final boolean[][] edge = createSpatioSpectralEdgeMask(data);

        final int rowCount = data[0].getHeight();
        final int colCount = data[0].getWidth();

        for (int i = 0; i < data.length; ++i) {
            final Raster r = data[i];
            final Raster m = mask[i];

            // 1. Compute the average across-track spatial derivative profile
            final double[] p = new double[colCount];
            for (int count = 0, col = 1; col < colCount; ++col) {
                for (int row = 0; row < rowCount; ++row) {
                    if (!edge[row][col] && m.getInt(col, row) == 0 && m.getInt(col - 1, row) == 0) {
                        // use logarithmic difference to make noise additive
                        p[col] += log(r.getDouble(col, row) / r.getDouble(col - 1, row));
                        ++count;
                    }
                }
                if (count > 0) {
                    p[col] /= count;
                } else {
                    p[col] = p[col - 1];
                }
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
            for (int col = targetRectangle.x; col < targetRectangle.x + targetRectangle.width; ++col) {
                target[i].setDouble(col, 0, exp(-p[col]));
            }
        }
    }

    /**
     * Creates the spatio-spectral edge mask for a hyperspectral image.
     *
     * @param data the hyperspectral image.
     *
     * @return the edge mask. The value {@code true} indicates changes in the surface texture or
     *         cover type.
     */
    private boolean[][] createSpatioSpectralEdgeMask(Raster[] data) {
        final int rowCount = data[0].getHeight();
        final int colCount = data[0].getWidth();

        final double[][] sad = new double[colCount][rowCount];

        for (int row = 0; row < rowCount; ++row) {
            double norm1 = 0.0;

            for (final Raster raster : data) {
                norm1 += square(raster.getDouble(0, row));
            }
            norm1 = sqrt(norm1);

            for (int col = 1; col < colCount; ++col) {
                double norm2 = 0.0;
                double scalarProduct = 0.0;

                for (final Raster raster : data) {
                    norm2 += square(raster.getDouble(col, row));
                    scalarProduct += raster.getDouble(col - 1, row) * raster.getDouble(col, row);
                }
                norm2 = sqrt(norm2);
                sad[col][row] = acos(scalarProduct / (norm1 * norm2));
                norm1 = norm2;
            }
        }

        final int minIndex = (int) (0.60 * rowCount);
        final int maxIndex = (int) (0.80 * rowCount);

        double minThreshold = 0.0;
        double maxThreshold = 0.0;

        for (int col = 1; col < colCount; ++col) {
            final double[] values = Arrays.copyOf(sad[col], rowCount);
            Arrays.sort(values);

            minThreshold = max(minThreshold, values[minIndex]);
            maxThreshold = max(maxThreshold, values[maxIndex]);
        }

        final boolean[][] edge = new boolean[rowCount][colCount];
        final double threshold = min(max(edgeDetectionThreshold, minThreshold), maxThreshold);

        for (int col = 1; col < colCount; ++col) {
            for (int row = 0; row < rowCount; ++ row) {
                if (sad[col][row] > threshold) {
                    edge[row][col] = true;
                }
            }
        }

        return edge;
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

    /**
     * Calculates the square of a given number.
     *
     * @param number the number.
     *
     * @return the number squared.
     */
    private static double square(double number) {
        return number * number;
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(VerticalStripingCorrectionOperator.class, "VerticalStripingCorrection");
        }

    }


    private static class PanoramaRaster implements Raster {

        private Raster[] rasters;
        private int height;

        private int[] rasterIndexTable;
        private int[] rowIndexTable;

        public PanoramaRaster(Raster[] rasters) {
            int height = rasters[0].getHeight();
            int width = rasters[0].getWidth();

            for (int i = 1; i < rasters.length; ++i) {
                Assert.argument(width == rasters[i].getWidth());
                height += rasters[i].getHeight();
            }

            this.rasters = rasters;
            this.height = height;

            createIndexTables();
        }

        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return rasters[0].getWidth();
        }

        public int getOffsetX() {
            return 0;
        }

        public int getOffsetY() {
            return 0;
        }

        public ProductData getDataBuffer() {
            return null;
        }

        public double getDouble(int x, int y) {
            return rasters[rasterIndexTable[y]].getDouble(x, rowIndexTable[y]);
        }

        public void setDouble(int x, int y, double value) {
            rasters[rasterIndexTable[y]].setDouble(x, rowIndexTable[y], value);
        }

        public float getFloat(int x, int y) {
            return rasters[rasterIndexTable[y]].getFloat(x, rowIndexTable[y]);
        }

        public void setFloat(int x, int y, float value) {
            rasters[rasterIndexTable[y]].setFloat(x, rowIndexTable[y], value);
        }

        public int getInt(int x, int y) {
            return rasters[rasterIndexTable[y]].getInt(x, rowIndexTable[y]);
        }

        public void setInt(int x, int y, int value) {
            rasters[rasterIndexTable[y]].setInt(x, rowIndexTable[y], value);
        }

        private void createIndexTables() {
            rasterIndexTable = new int[height];
            rowIndexTable = new int[height];

            for (int i = 0, j = 0; i < rasters.length; ++i) {
                for (int k = 0; k < rasters[i].getHeight(); ++k, ++j) {
                    rasterIndexTable[j] = i;
                    rowIndexTable[j] = k;
                }
            }
        }

    }

}
