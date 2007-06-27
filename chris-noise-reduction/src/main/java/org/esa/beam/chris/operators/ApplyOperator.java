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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import static java.lang.Math.round;

/**
 * Operator for applying the vertical striping (VS) correction factors calculated by
 * the {@link VerticalStripingCorrectionOperator}.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class ApplyOperator extends AbstractOperator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @SourceProduct(alias = "factors")
    Product factorProduct;
    @TargetProduct
    Product targetProduct;

    /**
     * Creates an instance of this class.
     *
     * @param spi the operator service provider interface.
     */
    public ApplyOperator(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        for (final String sourceBandName : sourceProduct.getBandNames()) {
            ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct);
        }

        return targetProduct;
    }

    @Override
    public void computeTile(Tile targetTile, ProgressMonitor progressMonitor) throws OperatorException {
        final String name = targetTile.getRasterDataNode().getName();
        final Rectangle targetRectangle = targetTile.getRectangle();

        if (name.startsWith("mask")) {
            getTile(sourceProduct.getBand(name), targetRectangle, targetTile.getDataBuffer());
        } else {
            final Raster data = getTile(sourceProduct.getBand(name), targetRectangle);
            final Raster corr = getTile(factorProduct.getBand(name.replace("radiance", "vs_correction")),
                                        new Rectangle(targetRectangle.x, 0, targetRectangle.width, 1));

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    targetTile.setInt(x, y, (int) round(data.getInt(x, y) * corr.getDouble(x, 0)));
                }
            }
        }
    }

    @Override
    public void dispose() {
        // todo - add any clean-up code here, the targetProduct is disposed by the framework
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(ApplyOperator.class, "Apply");
        }
    }

}
