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

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.InputStream;
import static java.lang.Math.round;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

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
                FlagCoding srcFlagCoding = sourceBand.getFlagCoding();
                if (targetProduct.getFlagCoding(srcFlagCoding.getName()) == null) {
                    ProductUtils.copyFlagCoding(srcFlagCoding, targetProduct);
                }
                targetBand.setFlagCoding(targetProduct.getFlagCoding(srcFlagCoding.getName()));
            }
        }
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        cloneMetadataElementsAndAttributes(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot(), 0);

        return targetProduct;
    }

    /////////////////////////////////////////////////////
    // TODO move to a more apropriate place! super??
    protected void cloneMetadataElementsAndAttributes(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        cloneMetadataElements(sourceRoot, destRoot, level);
        cloneMetadataAttributes(sourceRoot, destRoot);
    }

    protected void cloneMetadataElements(MetadataElement sourceRoot, MetadataElement destRoot, int level) {
        for (int i = 0; i < sourceRoot.getNumElements(); i++) {
            MetadataElement sourceElement = sourceRoot.getElementAt(i);
            MetadataElement element = new MetadataElement(sourceElement.getName());
            element.setDescription(sourceElement.getDescription());
            destRoot.addElement(element);
            cloneMetadataElementsAndAttributes(sourceElement, element, level + 1);
        }
    }

    protected void cloneMetadataAttributes(MetadataElement sourceRoot, MetadataElement destRoot) {
        for (int i = 0; i < sourceRoot.getNumAttributes(); i++) {
            MetadataAttribute sourceAttribute = sourceRoot.getAttributeAt(i);
            destRoot.addAttribute(sourceAttribute.createDeepClone());
        }
    }
    // TODO move to a more apropriate place! super??
    /////////////////////////////////////////////////////

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
                        int value = (int) round(sourceTile.getInt(x, y) / noiseFactors[x]);
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

    private static double[][] readReferenceSlitVsProfile() throws OperatorException {
        final Scanner scanner = getResourceAsScanner("slit-vs-profile.dat");

        final List<Double> abscissaList = new ArrayList<Double>(150000);
        final List<Double> ordinateList = new ArrayList<Double>(150000);

        try {
            while (scanner.hasNext()) {
                abscissaList.add(scanner.nextDouble());
                ordinateList.add(scanner.nextDouble());
            }
        } catch (Exception e) {
            throw new OperatorException("could not read reference slit-VS profile", e);
        } finally {
            scanner.close();
        }

        final double[] abscissas = new double[abscissaList.size()];
        final double[] ordinates = new double[ordinateList.size()];

        for (int i = 0; i < abscissas.length; ++i) {
            abscissas[i] = abscissaList.get(i);
            ordinates[i] = ordinateList.get(i);
        }

        return new double[][]{abscissas, ordinates};
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
     * Returns a {@link Scanner} for a resource file of interest.
     *
     * @param name the name of the resource file of interest.
     *
     * @return the scanner.
     *
     * @throws OperatorException if the resource could not be found.
     */
    private static Scanner getResourceAsScanner(String name) throws OperatorException {
        final InputStream is = SlitCorrectionOp.class.getResourceAsStream(name);

        if (is == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }

        final Scanner scanner = new Scanner(new BufferedInputStream(is));
        scanner.useLocale(Locale.ENGLISH);

        return scanner;
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(SlitCorrectionOp.class, "SlitCorrection");
            // todo -- set description etc.
        }
    }

}
