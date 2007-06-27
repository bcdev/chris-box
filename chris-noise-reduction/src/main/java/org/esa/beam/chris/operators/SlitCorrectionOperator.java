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
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Operator for correcting the vertical striping (VS) due to irregularities of
 * the entrance slit.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class SlitCorrectionOperator extends AbstractOperator {

    @SourceProduct
    Product sourceProduct;
    @TargetProduct
    Product targetProduct;

    private int spectralBandCount;

    private double[] vsPixels;
    private double[] vsMean;

    private transient Band[] targetBands;
    private transient Band[][] sourceDataBands;
    private transient Band[][] sourceMaskBands;

    private static final double P2VSG_1 = 0.13045510094294;
    private static final double P2VSG_2 = 0.28135856882126;
    private static final double P2VSS_1 = -0.12107994955864;
    private static final double P2VSS_2 = 0.65034734426230;
    private double[] vs;


    /**
     * Creates an instance of this class.
     *
     * @param spi the operator service provider interface.
     */
    public SlitCorrectionOperator(OperatorSpi spi) {
        super(spi);
    }

    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        setSpectralBandCount();
        setReferenceSlitVsProfile();

        adjustSlitVsProfile();

        targetProduct = new Product("slit_corrected", sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        for (String sourceBandName : sourceProduct.getBandNames()) {
            ProductUtils.copyBand(sourceBandName, sourceProduct, targetProduct);
        }

        return targetProduct;
    }

    private void adjustSlitVsProfile() throws OperatorException {
        final double temperature = getChrisTemperature(sourceProduct);

        final double vsGain = P2VSG_1 * temperature + P2VSG_2;
        final double vsShift = P2VSS_1 * temperature + P2VSS_2;

        // Correction of VS dependence on temperature
        for (int i = 0; i < vsPixels.length; ++i) {
            vsMean[i] = (vsMean[i] - 1.0) * vsGain + 1.0;
            vsPixels[i] -= vsShift;
        }

        int ppc = 1;
        if ("1".equals(getAnnotationString(sourceProduct, ChrisConstants.ATTR_NAME_CHRIS_MODE))) {
            ppc = 2;
        }

        final int width = sourceProduct.getSceneRasterWidth();
        vs = new double[width];
        int x = 0;
        int count = 0;

        for (int i = 0; i < vsPixels.length; ++i) {
            if (x < width) {
                if (vsPixels[i] > 0.5 && vsPixels[i] < (x + 1) * ppc + 0.5) {
                    vs[x] += vsMean[i];
                    count++;
                }
                if (i == vsPixels.length - 1) {
                    vs[x] /= count;
                } else if (vsPixels[i + 1] >= (x + 1) * ppc + 0.5) {
                    vs[x] /= count;
                    x++;
                    count = 0;
                }
            }
        }
    }

    @Override
    public void computeTile(Tile targetTile, ProgressMonitor progressMonitor) throws OperatorException {
        String name = targetTile.getRasterDataNode().getName();
        Rectangle rect = targetTile.getRectangle();
        if (name.startsWith("mask")) {
            getTile(sourceProduct.getBand(name), targetTile.getRectangle(), targetTile.getDataBuffer());
        } else {
            Tile sourceTile = getTile(sourceProduct.getBand(name), targetTile.getRectangle());
            for (int y = rect.y; y < rect.y + rect.height; y++) {
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    int correctedValue = (int) (sourceTile.getInt(x, y) / vs[x] + 0.5);
                    targetTile.setInt(x, y, correctedValue);
                }
            }
        }
    }

    @Override
    public void dispose() {
        // todo - add any clean-up code here, the targetProduct is disposed by the framework
    }

    private void setReferenceSlitVsProfile() throws OperatorException {
        final Scanner scanner = getResourceAsScanner("slit-vs-profile.dat");

        final List<Double> abscissaList = new ArrayList<Double>();
        final List<Double> ordinateList = new ArrayList<Double>();

        try {
            while (scanner.hasNext()) {
                abscissaList.add(scanner.nextDouble());
                ordinateList.add(scanner.nextDouble());
            }
        } catch (Exception e) {
            throw new OperatorException("", e);
            // todo - message
        } finally {
            scanner.close();
        }

        vsPixels = new double[abscissaList.size()];
        vsMean = new double[ordinateList.size()];

        for (int i = 0; i < vsPixels.length; ++i) {
            vsPixels[i] = abscissaList.get(i);
            vsMean[i] = ordinateList.get(i);
        }
    }

    private void setSpectralBandCount() throws OperatorException {
        final String annotation = getAnnotationString(sourceProduct, ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        if (annotation == null) {
            throw new OperatorException("");
            // todo - message
        }
        try {
            spectralBandCount = Integer.parseInt(annotation);
        } catch (NumberFormatException e) {
            throw new OperatorException("", e);
            // todo - message
        }
    }

    private static double getChrisTemperature(Product product) throws OperatorException {
        final String annotation = getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);

        if (annotation == null) {
            throw new OperatorException(MessageFormat.format(
                    "could not read annotation ''{0}''", ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE));
        }
        try {
            return Double.parseDouble(annotation);
        } catch (NumberFormatException e) {
            throw new OperatorException(MessageFormat.format(
                    "could not parse annotation: ''{0}''", ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE));
        }
    }

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation or {@code null} if the annotation could not be found.
     */
    private static String getAnnotationString(Product product, String name) {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        String annotation = null;

        if (metadataRoot != null) {
            final MetadataElement mph = metadataRoot.getElement(ChrisConstants.MPH_NAME);

            if (mph != null) {
                annotation = mph.getAttributeString(name, null);
            }
        }

        return annotation;
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
        final InputStream is = SlitCorrectionOperator.class.getResourceAsStream(name);

        if (is == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        return scanner;
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(SlitCorrectionOperator.class, "SlitCorrection");
        }
    }

}
