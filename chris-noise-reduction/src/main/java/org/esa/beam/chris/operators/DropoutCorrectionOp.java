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
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.chris.internal.DropoutCorrection;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.MessageFormat;

/**
 * Operator for computing the CHRIS dropout correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class DropoutCorrectionOp extends AbstractOperator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "5", interval = "(1, 8)")
    private int neighborBandCount;

    @Parameter(defaultValue = "4", valueSet = {"2", "4", "8"})
    private int type;

    private DropoutCorrection dropoutCorrection;
    private int spectralBandCount;

    private Band[] sourceRciBands;
    private Band[] sourceMaskBands;
    private Band[] targetRciBands;
    private Band[] targetMaskBands;

    public DropoutCorrectionOp(OperatorSpi operatorSpi) {
        super(operatorSpi);
    }

    @Override
    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        assertValidity(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyElementsAndAttributes(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        spectralBandCount = getAnnotationInt(sourceProduct, ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        sourceRciBands = new Band[spectralBandCount];
        sourceMaskBands = new Band[spectralBandCount];
        targetRciBands = new Band[spectralBandCount];
        targetMaskBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String bandName = new StringBuilder("radiance_").append(i + 1).toString();
            sourceRciBands[i] = sourceProduct.getBand(bandName);

            if (sourceRciBands[i] == null) {
                throw new OperatorException(MessageFormat.format("could not find band {0}", bandName));
            }
            targetRciBands[i] = ProductUtils.copyBand(bandName, sourceProduct, targetProduct);
            targetRciBands[i].setValidPixelExpression(sourceRciBands[i].getValidPixelExpression());
        }
        for (int i = 0; i < spectralBandCount; ++i) {
            final String bandName = new StringBuilder("mask_").append(i + 1).toString();
            sourceMaskBands[i] = sourceProduct.getBand(bandName);

            if (sourceMaskBands[i] == null) {
                throw new OperatorException(MessageFormat.format("could not find band {0}", bandName));
            }
            targetMaskBands[i] = ProductUtils.copyBand(bandName, sourceProduct, targetProduct);

            final FlagCoding flagCoding = sourceMaskBands[i].getFlagCoding();
            if (flagCoding != null) {
                targetMaskBands[i].setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);

        dropoutCorrection = new DropoutCorrection();
        // todo -- consider type
        neighborBandCount = 5;

        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        final Band targetBand = (Band) targetRaster.getRasterDataNode();
        final int bandIndex = targetBand.getSpectralBandIndex();

        final int minBandIndex = max(bandIndex - neighborBandCount, 0);
        final int maxBandIndex = min(bandIndex + neighborBandCount, spectralBandCount - 1);
        final int bandCount = maxBandIndex - minBandIndex + 1;

        final Rectangle targetRectangle = targetRaster.getRectangle();
        final Rectangle sourceRectangle = createSourceRectangle(targetRectangle);
        final int[][] sourceRciData = new int[bandCount][];
        final short[][] sourceMaskData = new short[bandCount][];

        try {
            pm.beginTask("computing dropout correction", bandCount + 1);
            for (int i = minBandIndex, j = 1; i <= maxBandIndex; ++i) {
                if (i != bandIndex) {
                    sourceRciData[j] = (int[]) getRasterData(sourceRciBands[i], sourceRectangle);
                    sourceMaskData[j] = (short[]) getRasterData(sourceMaskBands[i], sourceRectangle);
                    ++j;
                } else {
                    sourceRciData[0] = (int[]) getRasterData(sourceRciBands[i], sourceRectangle);
                    sourceMaskData[0] = (short[]) getRasterData(sourceMaskBands[i], sourceRectangle);
                }
                pm.worked(1);
            }

            final int[] targetRciData;
            final short[] targetMaskData;
            if (targetBand.equals(targetRciBands[bandIndex])) {
                targetRciData = (int[]) targetRaster.getDataBuffer().getElems();
                targetMaskData = (short[]) getRasterData(targetMaskBands[bandIndex], targetRectangle);
            } else {
                targetRciData = (int[]) getRasterData(targetRciBands[bandIndex], targetRectangle);
                targetMaskData = (short[]) targetRaster.getDataBuffer().getElems();
            }

            dropoutCorrection.compute(sourceRciData, sourceMaskData, sourceRectangle.width, sourceRectangle.height,
                                      new Rectangle(targetRectangle.x - sourceRectangle.x,
                                                    targetRectangle.y - sourceRectangle.y,
                                                    targetRectangle.width, targetRectangle.height),
                                      targetRciData, targetMaskData, 0, 0, targetRectangle.width);
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        dropoutCorrection = null;
        sourceRciBands = null;
        sourceMaskBands = null;
        targetRciBands = null;
        targetMaskBands = null;
    }

    private Rectangle createSourceRectangle(Rectangle targetRectangle) {
        int x = targetRectangle.x;
        int y = targetRectangle.y;
        int width = targetRectangle.width;
        int height = targetRectangle.height;

        if (x > 0) {
            x -= 1;
            width += 1;
        }
        if (x + width < targetProduct.getSceneRasterWidth()) {
            width += 1;
        }
        if (y > 0) {
            y -= 1;
            height += 1;
        }
        if (y + height < targetProduct.getSceneRasterHeight()) {
            height += 1;
        }

        return new Rectangle(x, y, width, height);
    }

    private Object getRasterData(Band band, Rectangle rectangle) throws OperatorException {
        return getRaster(band, rectangle).getDataBuffer().getElems();
    }

    private static void assertValidity(Product product) throws OperatorException {
        try {
            getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
            getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()));
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
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            throw new OperatorException(MessageFormat.format("could not get CHRIS annotation ''{0}''", name));
        }
        return element.getAttributeString(name, null);
    }

    // todo -- move
    private static int getAnnotationInt(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(DropoutCorrectionOp.class, "DropoutCorrection");
            // todo -- set description etc.
        }
    }

}
