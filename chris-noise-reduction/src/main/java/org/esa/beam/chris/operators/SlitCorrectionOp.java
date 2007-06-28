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

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;

/**
 * Operator for correcting the vertical striping (VS) due to the entrance slit.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
public class SlitCorrectionOp extends AbstractOperator {

    /**
     * Name of the specific meta data element added to the source product
     */
    public static final String ELEM_NAME_NOISE_REDUCTION = "Noise Reduction";
    /**
     * Name of the attribute added to the {@link ELEM_NAME_NOISE_REDUCTION} element.
     */
    public static final String ATTR_NAME_SLIT_CORR = "slit_corr";

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    // todo -- operator parameters?
    private static final double G1 = 0.13045510094294;
    private static final double G2 = 0.28135856882126;
    private static final double S1 = -0.12107994955864;
    private static final double S2 = 0.65034734426230;

    private double[] f;
    private boolean alreadyCorrected;

    /**
     * Creates an instance of this class.
     *
     * @param spi the operator service provider interface.
     */
    public SlitCorrectionOp(OperatorSpi spi) {
        super(spi);
    }

    @Override
    protected Product initialize(ProgressMonitor pm) throws OperatorException {
        ensureValidProduct(sourceProduct);

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        for (final Band sourceBand : sourceProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);

            if (sourceBand.getFlagCoding() != null) {
                final FlagCoding sourceFlagCoding = sourceBand.getFlagCoding();
                if (targetProduct.getFlagCoding(sourceFlagCoding.getName()) == null) {
                    ProductUtils.copyFlagCoding(sourceFlagCoding, targetProduct);
                }
                targetBand.setFlagCoding(targetProduct.getFlagCoding(sourceFlagCoding.getName()));
            }
        }

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        final MetadataElement sourceRoot = sourceProduct.getMetadataRoot();
        final MetadataElement targetRoot = targetProduct.getMetadataRoot();

        copyMetadataElementsAndAttributes(sourceRoot, targetRoot);
        alreadyCorrected = sourceRoot.containsElement(ChrisConstants.SPH_NAME)
                           && sourceRoot.getElement(ChrisConstants.SPH_NAME).containsElement(ELEM_NAME_NOISE_REDUCTION);
        if (!alreadyCorrected) {
            computeCorrectionFactors();
            addSpecificMetadata(targetRoot,
                                ELEM_NAME_NOISE_REDUCTION, ATTR_NAME_SLIT_CORR, "Slit correction factors", f);
        }

        return targetProduct;
    }

    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        final String name = targetRaster.getRasterDataNode().getName();
        final Rectangle targetRectangle = targetRaster.getRectangle();

        if (alreadyCorrected || !name.startsWith("radiance")) {
            getRaster(sourceProduct.getBand(name), targetRectangle, targetRaster.getDataBuffer());
        } else {
            try {
                pm.beginTask("correcting slit artifacts", targetRectangle.height);
                final Raster sourceRaster = getRaster(sourceProduct.getBand(name), targetRectangle);

                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                        final int value = (int) (sourceRaster.getInt(x, y) * f[x] + 0.5);
                        targetRaster.setInt(x, y, value);
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    private void computeCorrectionFactors() throws OperatorException {
        final double[][] table = readReferenceSlitVsProfile();

        final double[] x = table[0];
        final double[] y = table[1];

        // shift and scale the reference profile according to actual temperature
        final double temperature = getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);
        final double scale = G1 * temperature + G2;
        final double shift = S1 * temperature + S2;
        for (int i = 0; i < x.length; ++i) {
            x[i] -= shift;
            y[i] = (y[i] - 1.0) * scale + 1.0;
        }

        int ppc;
        if ("1".equals(getAnnotationString(sourceProduct, ChrisConstants.ATTR_NAME_CHRIS_MODE))) {
            ppc = 2;
        } else {
            ppc = 1;
        }

        // rebin the profile onto CCD pixels
        f = new double[sourceProduct.getSceneRasterWidth()];
        for (int pixel = 0, i = 0; pixel < f.length; ++pixel) {
            int count = 0;
            for (; i < x.length; ++i) {
                if (x[i] > (pixel + 1) * ppc + 0.5) {
                    break;
                }
                if (x[i] > 0.5) {
                    f[pixel] += y[i];
                    ++count;
                }
            }
            if (count != 0) {
                f[pixel] = count / f[pixel];
            } else { // can only happen if the domain of the reference profile is too small
                f[pixel] = 1.0;
            }
        }
    }

    private static double[][] readReferenceSlitVsProfile() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("slit-vs-profile.img");

        try {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("could not read reference slit-VS profile", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static void ensureValidProduct(Product product) throws OperatorException {
        if (!product.getProductType().startsWith("CHRIS_M")) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()));
        }
    }

    // todo -- move
    private static void copyMetadataElementsAndAttributes(MetadataElement source, MetadataElement target) {
        for (final MetadataElement element : source.getElements()) {
            target.addElement(element.createDeepClone());
        }
        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    private static void addSpecificMetadata(MetadataElement target, String parentName, String childName,
                                            String description, double[] values) {
        if (!target.containsElement(ChrisConstants.SPH_NAME)) {
            target.addElement(new MetadataElement(ChrisConstants.SPH_NAME));
        }
        final MetadataElement sph = target.getElement(ChrisConstants.SPH_NAME);
        if (!sph.containsElement(parentName)) {
            sph.addElement(new MetadataElement(parentName));
        }

        final MetadataElement child = new MetadataElement(childName);
        child.setDescription(description);
        child.addAttribute(new MetadataAttribute(childName, ProductData.createInstance(values), true));

        sph.getElement(parentName).addElement(child);
    }

    private static double getAnnotationDouble(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Double.parseDouble(string);
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

    /**
     * Returns an {@link ImageInputStream} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     *
     * @return the image input stream.
     *
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
     */
    private static ImageInputStream getResourceAsImageInputStream(String name) throws OperatorException {
        final URL url = SlitCorrectionOp.class.getResource(name);

        if (url == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }
        try {
            return new FileImageInputStream(new File(url.toURI()));
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format(
                    "could not create image input stream for resource {0}", name), e);
        }
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(SlitCorrectionOp.class, "SlitCorrection");
            // todo -- set description etc.
        }
    }

}
