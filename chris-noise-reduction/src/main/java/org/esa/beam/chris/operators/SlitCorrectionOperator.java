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
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

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

    private double[] positions;
    private double[] gains;

    private transient Band[] targetBands;
    private transient Band[][] sourceDataBands;
    private transient Band[][] sourceMaskBands;

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

        return null;
    }

    @Override
    public void computeTiles(Rectangle targetRectangle, ProgressMonitor progressMonitor) throws OperatorException {
        // todo - implement
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

        final double[] positions = new double[abscissaList.size()];
        final double[] gains = new double[ordinateList.size()];

        for (int i = 0; i < positions.length; ++i) {
            positions[i] = abscissaList.get(i);
            gains[i] = ordinateList.get(i);
        }
    }

    private void setSpectralBandCount() throws OperatorException {
        final String annotation = getChrisAnnotation(sourceProduct, ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

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

    /**
     * Returns a CHRIS annotation for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation or {@code null} if the annotation could not be found.
     */
    private static String getChrisAnnotation(Product product, String name) {
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
