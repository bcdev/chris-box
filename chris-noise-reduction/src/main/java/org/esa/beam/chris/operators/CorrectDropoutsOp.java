/* $Id: CorrectDropoutsOp.java 2293 2008-06-20 11:30:35Z ralf $
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

import java.awt.Rectangle;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Operator for computing the CHRIS/PROBA dropout correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.CorrectDropouts",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Carries out the dropout correction for a CHRIS RCI/PROBA.")
public class CorrectDropoutsOp extends Operator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "5", interval = "[1, 62]")
    private int neighborBandCount;

    @Parameter(defaultValue = "N4", valueSet = {"N4", "N8"})
    private DropoutCorrection.Type neighborhoodType;

    private DropoutCorrection dropoutCorrection;
    private int spectralBandCount;

    private Band[] sourceRciBands;
    private Band[] sourceMskBands;
    private Band[] targetRciBands;
    private Band[] targetMskBands;

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

        spectralBandCount = OpUtils.getAnnotationInt(sourceProduct, ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        sourceRciBands = new Band[spectralBandCount];
        sourceMskBands = new Band[spectralBandCount];
        targetRciBands = new Band[spectralBandCount];
        targetMskBands = new Band[spectralBandCount];

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
            sourceMskBands[i] = sourceProduct.getBand(bandName);

            if (sourceMskBands[i] == null) {
                throw new OperatorException(MessageFormat.format("could not find band {0}", bandName));
            }
            targetMskBands[i] = ProductUtils.copyBand(bandName, sourceProduct, targetProduct);

            final FlagCoding flagCoding = sourceMskBands[i].getFlagCoding();
            if (flagCoding != null) {
                targetMskBands[i].setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        dropoutCorrection = new DropoutCorrection(neighborhoodType);
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 16);
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
        sourceMskBands = null;
        targetRciBands = null;
        targetMskBands = null;
    }

    private void computeDropoutCorrection(int bandIndex, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                          Rectangle sourceRectangle, ProgressMonitor pm) throws OperatorException {
        final int minBandIndex = max(bandIndex - neighborBandCount, 0);
        final int maxBandIndex = min(bandIndex + neighborBandCount, spectralBandCount - 1);
        final int bandCount = maxBandIndex - minBandIndex + 1;

        final int[][] sourceRciData = new int[bandCount][];
        final short[][] sourceMskData = new short[bandCount][];

        final Tile sourceRciTile = getSourceTile(sourceRciBands[bandIndex], sourceRectangle, pm);
        final Tile sourceMskTile = getSourceTile(sourceMskBands[bandIndex], sourceRectangle, pm);

        final int sourceScanlineOffset = sourceRciTile.getScanlineOffset();
        final int sourceScanlineStride = sourceRciTile.getScanlineStride();

        Assert.state(sourceScanlineOffset == sourceMskTile.getScanlineOffset());
        Assert.state(sourceScanlineStride == sourceMskTile.getScanlineStride());

        sourceRciData[0] = sourceRciTile.getDataBufferInt();
        sourceMskData[0] = sourceMskTile.getDataBufferShort();

        for (int i = minBandIndex, j = 1; i <= maxBandIndex; ++i) {
            if (i != bandIndex) {
                final Tile neighborRciTile = getSourceTile(sourceRciBands[i], sourceRectangle, pm);
                final Tile neighborMskTile = getSourceTile(sourceMskBands[i], sourceRectangle, pm);

                Assert.state(sourceScanlineOffset == neighborRciTile.getScanlineOffset());
                Assert.state(sourceScanlineStride == neighborRciTile.getScanlineStride());
                Assert.state(sourceScanlineOffset == neighborMskTile.getScanlineOffset());
                Assert.state(sourceScanlineStride == neighborMskTile.getScanlineStride());

                sourceRciData[j] = neighborRciTile.getDataBufferInt();
                sourceMskData[j] = neighborMskTile.getDataBufferShort();
                ++j;
            }
        }

        final Tile targetRciTile = targetTileMap.get(targetRciBands[bandIndex]);
        final Tile targetMskTile = targetTileMap.get(targetMskBands[bandIndex]);

        final int targetScanlineStride = targetRciTile.getScanlineStride();
        final int targetScanlineOffset = targetRciTile.getScanlineOffset();

        Assert.state(targetScanlineOffset == targetMskTile.getScanlineOffset());
        Assert.state(targetScanlineStride == targetMskTile.getScanlineStride());

        final int[] targetRciData = targetRciTile.getDataBufferInt();
        final short[] targetMskData = targetMskTile.getDataBufferShort();

        dropoutCorrection.compute(sourceRciData, sourceMskData, sourceRectangle, sourceScanlineOffset,
                                  sourceScanlineStride,
                                  targetRciData, targetMskData, targetRectangle, targetScanlineOffset,
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
            OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()), e);
        }
        // todo - add further validation criteria
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CorrectDropoutsOp.class);
        }
    }
}
