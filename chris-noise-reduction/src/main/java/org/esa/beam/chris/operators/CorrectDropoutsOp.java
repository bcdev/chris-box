/* $Id$
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
import org.esa.beam.dataio.chris.internal.DropoutCorrection;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Operator for computing the CHRIS dropout correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.CorrectDropouts",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Carries out the dropout correction for a CHRIS RCI.")
public class CorrectDropoutsOp extends Operator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "5", interval = "[1, 62]")
    private Integer neighborBandCount;

    @Parameter(defaultValue = "N4", valueSet = {"N4", "N8"})
    private DropoutCorrection.Type neighborhoodType;

    private DropoutCorrection dropoutCorrection;
    private int spectralBandCount;

    private Band[] sourceRciBands;
    private Band[] sourceMaskBands;
    private Band[] targetRciBands;
    private Band[] targetMaskBands;

    @Override
    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

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
        dropoutCorrection = new DropoutCorrection(neighborhoodType);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("computing dropout correction...", spectralBandCount);
        try {
            final Rectangle sourceRectangle = createSourceRectangle(targetRectangle);

            for (int bandIndex = 0; bandIndex < spectralBandCount; ++bandIndex) {
                checkForCancelation(pm);
                computeDropoutCorrection(bandIndex, targetTileMap, targetRectangle, sourceRectangle, pm);
                pm.worked(1);
            }
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

    private void computeDropoutCorrection(int bandIndex, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                          Rectangle sourceRectangle, ProgressMonitor pm) throws OperatorException {
        final int minBandIndex = max(bandIndex - neighborBandCount, 0);
        final int maxBandIndex = min(bandIndex + neighborBandCount, spectralBandCount - 1);
        final int bandCount = maxBandIndex - minBandIndex + 1;

        final int[][] sourceRciData = new int[bandCount][];
        final short[][] sourceMaskData = new short[bandCount][];

        final Tile sourceTile1 = getSourceTile(sourceRciBands[bandIndex], sourceRectangle, pm);
        final Tile sourceTile2 = getSourceTile(sourceMaskBands[bandIndex], sourceRectangle, pm);

        final int sourceScanlineOffset = sourceTile1.getScanlineOffset();
        final int sourceScanlineStride = sourceTile1.getScanlineStride();

        Assert.state(sourceScanlineOffset == sourceTile2.getScanlineOffset());
        Assert.state(sourceScanlineStride == sourceTile2.getScanlineStride());

        sourceRciData[0] = sourceTile1.getDataBufferInt();
        sourceMaskData[0] = sourceTile2.getDataBufferShort();

        for (int i = minBandIndex, j = 1; i <= maxBandIndex; ++i) {
            if (i != bandIndex) {
                final Tile tile1 = getSourceTile(sourceRciBands[i], sourceRectangle, pm);
                final Tile tile2 = getSourceTile(sourceMaskBands[i], sourceRectangle, pm);

                Assert.state(sourceScanlineOffset == tile1.getScanlineOffset());
                Assert.state(sourceScanlineStride == tile1.getScanlineStride());
                Assert.state(sourceScanlineOffset == tile2.getScanlineOffset());
                Assert.state(sourceScanlineStride == tile2.getScanlineStride());

                sourceRciData[j] = tile1.getDataBufferInt();
                sourceMaskData[j] = tile2.getDataBufferShort();
                ++j;
            }
        }

        final Tile targetTile1 = targetTileMap.get(targetRciBands[bandIndex]);
        final Tile targetTile2 = targetTileMap.get(targetMaskBands[bandIndex]);

        final int targetScanlineStride = targetTile1.getScanlineStride();
        final int targetScanlineOffset = targetTile1.getScanlineOffset();

        Assert.state(targetScanlineOffset == targetTile2.getScanlineOffset());
        Assert.state(targetScanlineStride == targetTile2.getScanlineStride());

        final int[] targetRciData = targetTile1.getDataBufferInt();
        final short[] targetMaskData = targetTile2.getDataBufferShort();

        dropoutCorrection.compute(sourceRciData, sourceMaskData, sourceRectangle, sourceScanlineOffset,
                                  sourceScanlineStride,
                                  targetRciData, targetMaskData, targetRectangle, targetScanlineOffset,
                                  targetScanlineStride);
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

    private static void assertValidity(Product product) throws OperatorException {
        try {
            getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()), e);
        }
        // todo - add further validation criteria
    }

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     * @return the annotation or {@code null} if the annotation could not be found.
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


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CorrectDropoutsOp.class);
        }
    }
}
