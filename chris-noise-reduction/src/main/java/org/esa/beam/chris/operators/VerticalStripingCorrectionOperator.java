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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.*;
import java.text.MessageFormat;
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
    double smoothingOrder;

    private double edgeDetectionThreshold;
    private int spectralBandCount = 18; // todo - set

    private static Map<String, Double> thresholdMap = new HashMap<String, Double>(6);

    static {
        thresholdMap.put("CHRIS_M1", 0.08);
        thresholdMap.put("CHRIS_M2", 0.05);
        thresholdMap.put("CHRIS_M3", 0.08);
        thresholdMap.put("CHRIS_M3A", 0.08);
        thresholdMap.put("CHRIS_M4", 0.08);
        thresholdMap.put("CHRIS_M5", 0.08);
    }

    public VerticalStripingCorrectionOperator(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        // todo - add any set-up code here

        setEdgeDetectionThreshold();

        targetProduct = new Product("product_name", "product_type", sourceProducts[0].getSceneRasterWidth(), 1);

        // todo - set product properties here

        for (int i = 0; i < spectralBandCount; ++i) {
            final Band targetBand = targetProduct.addBand(
                    MessageFormat.format("vs_correction_{0}", i + 1), ProductData.TYPE_FLOAT64);

            targetBand.setSpectralBandIndex(i + 1);
            targetBand.setDescription(
                    MessageFormat.format("Vertical striping correction factors for radiance band {0}", i + 1));
        }

        return targetProduct;
    }

    public void computeTiles(Rectangle rectangle, ProgressMonitor progressMonitor) throws OperatorException {
        final Raster[] data = new PanoramaRaster[spectralBandCount];
        final Raster[] mask = new PanoramaRaster[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            final Raster[] dataRasters = new Raster[sourceProducts.length];
            final Raster[] maskRasters = new Raster[sourceProducts.length];

            for (int j = 0; j < sourceProducts.length; ++j) {
                final Band dataBand = sourceProducts[j].getBand(MessageFormat.format("radiance_{0}", i + 1));
                final Band maskBand = sourceProducts[j].getBand(MessageFormat.format("mask_{0}", i + 1));

                if (sourceProducts[0].getSceneRasterWidth() != sourceProducts[j].getSceneRasterWidth()) {
                    throw new OperatorException("");
                    // todo - message
                }
                final Rectangle sourceRectangle = new Rectangle(0, 0, sourceProducts[j].getSceneRasterWidth(),
                        sourceProducts[j].getSceneRasterHeight());

                dataRasters[j] = getTile(dataBand, sourceRectangle);
                maskRasters[j] = getTile(maskBand, sourceRectangle);
            }

            data[i] = new PanoramaRaster(dataRasters);
            mask[i] = new PanoramaRaster(maskRasters);
        }

        performVerticalStripingReduction(data, mask, rectangle);
    }

    public void dispose() {
        // todo - add any clean-up code here, the targetProduct is disposed by the framework
    }

    private void performVerticalStripingReduction(Raster[] data, Raster[] mask, Rectangle rectangle) {
        
    }

    private void setEdgeDetectionThreshold() throws OperatorException {
        final Double threshold = thresholdMap.get(sourceProducts[0].getProductType());

        if (threshold == null) {
            throw new OperatorException("");
            // todo - message
        }
        edgeDetectionThreshold = threshold;
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
