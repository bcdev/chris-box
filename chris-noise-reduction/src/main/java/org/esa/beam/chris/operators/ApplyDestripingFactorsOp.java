/* $Id: ApplyDestripingFactorsOp.java 2293 2008-06-20 11:30:35Z ralf $
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
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.text.MessageFormat;

/**
 * Operator for applying the vertical striping (VS) correction factors calculated by
 * the {@link ComputeDestripingFactorsOp}.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ApplyDestripingFactors",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Applies a precomputed set of destriping factors to a CHRIS/Proba RCI.")
public class ApplyDestripingFactorsOp extends Operator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @SourceProduct(alias = "factors")
    Product factorProduct;
    @TargetProduct
    Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        targetProduct = new Product(sourceProduct.getName() + "_NR", sourceProduct.getProductType() + "_NR",
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        for (final Band sourceBand : sourceProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);

            final FlagCoding flagCoding = sourceBand.getFlagCoding();
            if (flagCoding != null) {
                targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);

        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        OpUtils.setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NOISE_REDUCTION,
                                    OpUtils.getAnnotationString(factorProduct,
                                                                ChrisConstants.ATTR_NAME_NOISE_REDUCTION));
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 16);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final String name = band.getName();
        if (name.startsWith("radiance")) {
            computeRciBand(name, targetTile, pm);
        } else {
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(name), targetTile.getRectangle(), pm);
            targetTile.setRawSamples(sourceTile.getRawSamples());
        }
    }

    private void computeRciBand(String name, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("removing vertical striping artifacts", targetTile.getHeight());
        try {
            final Band sourceBand = sourceProduct.getBand(name);
            final Band factorBand = factorProduct.getBand(name.replace("radiance", "vs_corr"));

            final Rectangle targetRectangle = targetTile.getRectangle();
            final Rectangle factorRectangle = new Rectangle(targetRectangle.x, 0, targetRectangle.width, 1);

            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle, pm);
            final Tile factorTile = getSourceTile(factorBand, factorRectangle, pm);

            final int[] sourceSamples = sourceTile.getDataBufferInt();
            final int[] targetSamples = targetTile.getDataBufferInt();
            final double[] factorSamples = factorTile.getDataBufferDouble();

            int sourceOffset = sourceTile.getScanlineOffset();
            int factorOffset = factorTile.getScanlineOffset();
            int targetOffset = targetTile.getScanlineOffset();

            for (int y = 0; y < targetTile.getHeight(); ++y) {
                checkForCancelation(pm);

                int sourceIndex = sourceOffset;
                int factorIndex = factorOffset;
                int targetIndex = targetOffset;
                for (int x = 0; x < targetTile.getWidth(); ++x) {
                    targetSamples[targetIndex] = (int) (sourceSamples[sourceIndex] * factorSamples[factorIndex] + 0.5);
                    ++sourceIndex;
                    ++factorIndex;
                    ++targetIndex;
                }
                sourceOffset += sourceTile.getScanlineStride();
                targetOffset += targetTile.getScanlineStride();

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
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
            super(ApplyDestripingFactorsOp.class);
        }
    }
}
