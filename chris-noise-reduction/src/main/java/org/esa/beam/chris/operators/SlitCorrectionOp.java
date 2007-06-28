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
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.round;
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

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    // todo -- operator parameters?
    private static final double G1 = 0.13045510094294;
    private static final double G2 = 0.28135856882126;
    private static final double S1 = -0.12107994955864;
    private static final double S2 = 0.65034734426230;

    private double[] noiseFactors;

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
        computeNoiseFactors();
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
        copyMetadataElementsAndAttributes(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        return targetProduct;
    }

    @Override
    public void computeTile(Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final String name = targetTile.getRasterDataNode().getName();
        final Rectangle targetRectangle = targetTile.getRectangle();

        if (name.startsWith("radiance")) {
            try {
                pm.beginTask("correcting slit vertical striping", targetRectangle.height);
                final Raster sourceTile = getTile(sourceProduct.getBand(name), targetRectangle);

                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                        final int value = (int) round(sourceTile.getInt(x, y) / noiseFactors[x]);
                        targetTile.setInt(x, y, value);
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        } else {
            getTile(sourceProduct.getBand(name), targetRectangle, targetTile.getDataBuffer());
        }
    }

    private void computeNoiseFactors() throws OperatorException {
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
        noiseFactors = new double[sourceProduct.getSceneRasterWidth()];
        for (int pixel = 0, i = 0; pixel < noiseFactors.length; ++pixel) {
            int count = 0;
            for (; i < x.length; ++i) {
                if (x[i] > (pixel + 1) * ppc + 0.5) {
                    break;
                }
                if (x[i] > 0.5) {
                    noiseFactors[pixel] += y[i];
                    ++count;
                }
            }
            if (count != 0) {
                noiseFactors[pixel] /= count;
            } else { // can only happen if the domain of the reference profile is too small
                noiseFactors[pixel] = 1.0;
            }
        }
    }

    private static double[][] readReferenceSlitVsProfile() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("slit-vs-profile.bin");

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

    // todo -- move
    private static void copyMetadataElementsAndAttributes(MetadataElement source, MetadataElement target) {
        for (final MetadataElement element : source.getElements()) {
            target.addElement(element.createDeepClone());
        }
        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
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
