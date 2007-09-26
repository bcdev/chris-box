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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.text.MessageFormat;

/**
 * Operator for applying the vertical striping (VS) correction factors calculated by
 * the {@link DestripingFactorsOp}.
 *
 * @author Ralf Quast
 * @author Marco Z�hlke
 * @version $Revision$ $Date$
 */
public class DestripingOp extends AbstractOperator {

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
    public DestripingOp(OperatorSpi spi) {
        super(spi);
    }

    @Override
	protected Product initialize(ProgressMonitor pm) throws OperatorException {
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
                targetBand.setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);

        ProductUtils.copyElementsAndAttributes(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NR_APPLIED, "Yes");
        setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NR_ACQUISITION_SET,
                            getAnnotationString(factorProduct, ChrisConstants.ATTR_NAME_NR_ACQUISITION_SET));

        return targetProduct;
    }

    @Override
    public void computeBand(Band band, Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        final String name = band.getName();

        if (name.startsWith("radiance")) {
            computeRciBand(name, targetRaster, pm);
        } else {
            computeMaskBand(name, targetRaster, pm);
        }
    }

    private void computeRciBand(String name, Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("removing vertical striping artifacts", targetRaster.getHeight());

            final Band sourceBand = sourceProduct.getBand(name);
            final Band factorBand = factorProduct.getBand(name.replace("radiance", "vs_corr"));

            final Rectangle targetRectangle = targetRaster.getRectangle();
            final Rectangle factorRectangle = new Rectangle(targetRectangle.x, 0, targetRectangle.width, 1);
            final Raster sourceRaster = getRaster(sourceBand, targetRectangle);
            final Raster factorRaster = getRaster(factorBand, factorRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                    final int value = (int) (sourceRaster.getInt(x, y) * factorRaster.getDouble(x, 0) + 0.5);
                    targetRaster.setInt(x, y, value);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void computeMaskBand(String name, Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("copying mask band", targetRaster.getHeight());

            final Rectangle targetRectangle = targetRaster.getRectangle();
            final Raster sourceRaster = getRaster(sourceProduct.getBand(name), targetRaster.getRectangle());

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                    targetRaster.setInt(x, y, sourceRaster.getInt(x, y));
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
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
    private static void setAnnotationString(Product product, String name, String value) throws OperatorException {
        MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);
        if (element == null) {
            element = new MetadataElement(ChrisConstants.MPH_NAME);
            product.getMetadataRoot().addElement(element);
        }
        if (element.containsAttribute(name)) {
            throw new OperatorException(MessageFormat.format(
                    "could not set CHRIS annotation ''{0}'' because it already exists", name));
        }
        element.addAttribute(new MetadataAttribute(name, ProductData.createInstance(value), true));
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(DestripingOp.class, "Destriping");
            // todo -- set description etc.
        }
    }

}
