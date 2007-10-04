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

import static java.lang.Math.acos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.esa.beam.chris.operators.internal.LocalRegressionSmoother;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.dataio.chris.internal.Sorter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.AbstractOperatorSpi;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

/**
 * Operator for calculating the vertical striping correction factors for noise
 * due to the CCD elements.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision: $ $Date: $
 */
public class DestripingFactorsOp extends AbstractOperator {

    // todo -- operator parameters?
    private static final double G1 = 0.13045510094294;
    private static final double G2 = 0.28135856882126;
    private static final double S1 = -0.12107994955864;
    private static final double S2 = 0.65034734426230;

    @SourceProducts
    Product[] sourceProducts;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "27", interval = "[11, 99]")
    Integer smoothingOrder;

    @Parameter(defaultValue = "true")
    Boolean slitCorrection;

    private int spectralBandCount;

    private transient LocalRegressionSmoother smoother;

    private transient Band[][] sourceRciBands;
    private transient Band[][] sourceMaskBands;
    private transient Band[] targetBands;
    private transient Panorama panorama;
    private boolean[][] edgeMask;
    private double[] slitNoiseFactors;


    @Override
    protected Product initialize() throws OperatorException {
        for (Product sourceProduct : sourceProducts) {
            assertValidity(sourceProduct);
        }

        spectralBandCount = getAnnotationInt(sourceProducts[0], ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        // set up source bands
        sourceRciBands = new Band[spectralBandCount][sourceProducts.length];
        sourceMaskBands = new Band[spectralBandCount][sourceProducts.length];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String rciBandName = new StringBuilder("radiance_").append(i + 1).toString();
            final String maskBandName = new StringBuilder("mask_").append(i + 1).toString();

            for (int j = 0; j < sourceProducts.length; ++j) {
                sourceRciBands[i][j] = sourceProducts[j].getBand(rciBandName);
                sourceMaskBands[i][j] = sourceProducts[j].getBand(maskBandName);

                if (sourceRciBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("could not find band {0}", rciBandName));
                }
                if (sourceMaskBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("could not find band {0}", maskBandName));
                }
            }
        }

        // set up target product and bands
        targetProduct = new Product(sourceProducts[0].getName() + "_VSC", "CHRIS_VSC",
                                    sourceProducts[0].getSceneRasterWidth(), 1);
        targetBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            targetBands[i] = targetProduct.addBand(
                    new StringBuilder("vs_corr_").append(i + 1).toString(), ProductData.TYPE_FLOAT64);

            targetBands[i].setSpectralBandIndex(i);
            targetBands[i].setDescription(MessageFormat.format(
                    "Vertical striping correction factors for radiance band {0}", i + 1));
            targetBands[i].setSpectralBandwidth(sourceRciBands[i][0].getSpectralBandwidth());
            targetBands[i].setSpectralWavelength(sourceRciBands[i][0].getSpectralWavelength());
        }

        final StringBuilder sb = new StringBuilder("Applied with ");
        for (int i = 0; i < sourceProducts.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getAnnotationString(sourceProducts[i], ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE));
            sb.append("°");
        }
        setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NOISE_REDUCTION, sb.toString());
        // todo -- consider writing the sources metadata to the SPH

        panorama = new Panorama(sourceProducts);
        smoother = new LocalRegressionSmoother(2, smoothingOrder, 2);
        try {
//            pm.beginTask("computing edge mask...", 10);
            if (slitCorrection) {
                slitNoiseFactors = getSlitNoiseFactors(sourceProducts[0]);
            }
//            pm.worked(1);
//            edgeMask = createEdgeMask(ProgressMonitor.NULL);
        } finally {
//            pm.done();
        }

        return targetProduct;
    }

    @Override
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
        ProgressMonitor pm = createProgressMonitor();
        try {
            if (edgeMask == null) {
                pm.beginTask("computing correction factors...", 100);
                edgeMask = createEdgeMask(SubProgressMonitor.create(pm, 90));
            } else {
                pm.beginTask("computing correction factors...", 10);
            }
            for (int i = 0; i < targetBands.length; ++i) {
                if (targetBands[i].equals(band)) {
                    computeCorrectionFactors(i, targetTile, SubProgressMonitor.create(pm, 10));
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        sourceRciBands = null;
        sourceMaskBands = null;
        targetBands = null;
        panorama = null;
        smoother = null;
        edgeMask = null;
        slitNoiseFactors = null;
    }

    /**
     * Computes the vertical striping correction factors for a single target band.
     *
     * @param bandIndex    the band index.
     * @param targetTile the target raster.
     * @param pm           the {@link ProgressMonitor}.
     *
     * @throws OperatorException
     */
    private void computeCorrectionFactors(int bandIndex, Tile targetTile, ProgressMonitor pm)
            throws OperatorException {
        try {
            pm.beginTask("computing corection factors", panorama.height + 5);

            // 1. Accumulate the across-track spatial derivative profile
            final double[] p = new double[panorama.width];
            final int[] count = new int[panorama.width];

            for (int j = 0; j < sourceProducts.length; ++j) {
                final Tile rci = getSceneTile(sourceRciBands[bandIndex][j]);
                final Tile mask = getSceneTile(sourceMaskBands[bandIndex][j]);

                for (int y = 0; y < rci.getHeight(); ++y) {
                    double r1 = getDouble(rci, 0, y);

                    for (int x = 1; x < rci.getWidth(); ++x) {
                        final double r2 = getDouble(rci, x, y);

                        if (!edgeMask[panorama.getY(j, y)][x] && mask.getSampleInt(x, y) == 0 && mask.getSampleInt(x - 1, y) == 0) {
                            p[x] += log(r2 / r1);
                            ++count[x];
                        }
                        r1 = r2;
                    }
                    pm.worked(1);
                }
            }
            // 2. Compute the average profile
            for (int x = 1; x < panorama.width; ++x) {
                if (count[x] > 0) {
                    p[x] /= count[x];
                } else {
                    p[x] = p[x - 1];
                }
            }
            pm.worked(1);
            // 3. Compute the integrated profile
            for (int x = 1; x < panorama.width; ++x) {
                p[x] += p[x - 1];
            }
            pm.worked(1);
            // 4. Smooth the integrated profile to get rid of small-scale variations (noise)
            final double[] s = new double[panorama.width];
            smoother.smooth(p, s);
            pm.worked(1);
            // 5. Compute the noise profile
            double meanNoise = 0.0;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= s[x];
                meanNoise += p[x];
            }
            meanNoise /= panorama.width;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= meanNoise;
            }
            pm.worked(1);
            // 6. Compute the correction factors
            for (int x = targetTile.getMinX(); x < targetTile.getMinX() + targetTile.getWidth(); ++x) {
                setDouble(targetTile, x, 0, exp(-p[x]));
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private static double[] getSlitNoiseFactors(Product product) throws OperatorException {
        final double[][] table = readReferenceSlitVsProfile();

        final double[] x = table[0];
        final double[] y = table[1];

        // shift and scale the reference profile according to actual temperature
        final double temperature = getAnnotationDouble(product, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);
        final double scale = G1 * temperature + G2;
        final double shift = S1 * temperature + S2;
        for (int i = 0; i < x.length; ++i) {
            x[i] -= shift;
            y[i] = (y[i] - 1.0) * scale + 1.0;
        }

        int ppc;
        if ("1".equals(getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE))) {
            ppc = 2;
        } else {
            ppc = 1;
        }

        // rebin the profile onto CCD pixels
        double[] f = new double[product.getSceneRasterWidth()];
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
                f[pixel] /= count;
            } else { // can only happen if the domain of the reference profile is too small
                f[pixel] = 1.0;
            }
        }

        return f;
    }

    /**
     * Creates the spatio-spectral edge mask for a hyperspectral image.
     *
     * @param pm the {@link ProgressMonitor}.
     *
     * @return the edge mask. The value {@code true} indicates changes in the surface
     *         texture or coverage.
     *
     * @throws OperatorException
     */
    private boolean[][] createEdgeMask(ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("creating edge mask", spectralBandCount + panorama.width + 2);

            final double[][] sad = new double[panorama.width][panorama.height];
            final double[][] sca = new double[panorama.width][panorama.height];

            // 1. Compute the squares and across-track scalar products of the spectral vectors
            for (final Band[] bands : sourceRciBands) {
                for (int i = 0; i < bands.length; i++) {
                    final Tile data = getSceneTile(bands[i]);

                    for (int y = 0; y < data.getHeight(); ++y) {
                        double r1 = getDouble(data, 0, y);
                        sad[0][panorama.getY(i, y)] += r1 * r1;

                        for (int x = 1; x < data.getWidth(); ++x) {
                            final double r2 = getDouble(data, x, y);

                            sca[x][panorama.getY(i, y)] += r2 * r1;
                            sad[x][panorama.getY(i, y)] += r2 * r2;
                            r1 = r2;
                        }
                    }
                }
                pm.worked(1);
            }
            // 2. Compute the across-track spectral angle differences
            for (int y = 0; y < panorama.height; ++y) {
                double norm1 = sqrt(sad[0][y]);
                sad[0][y] = 0.0;

                for (int x = 1; x < panorama.width; ++x) {
                    final double norm2 = sqrt(sad[x][y]);

                    sad[x][y] = acos(sca[x][y] / (norm1 * norm2));
                    norm1 = norm2;
                }
            }
            pm.worked(1);

            final int minIndex = (int) (0.60 * panorama.height);
            final int maxIndex = (int) (0.80 * panorama.height);

            double minThreshold = 0.0;
            double maxThreshold = 0.0;

            // 3. Adjust the edge-detection threshold
            for (int x = 1; x < panorama.width; ++x) {
                final double[] values = Arrays.copyOf(sad[x], panorama.height);
                minThreshold = max(minThreshold, Sorter.nthElement(values, minIndex));
                maxThreshold = max(maxThreshold, Sorter.nthElement(values, maxIndex));

                pm.worked(1);
            }
            final double threshold = min(max(getEdgeDetectionThreshold(sourceProducts[0]), minThreshold), maxThreshold);

            // 4. Create the edge mask
            final boolean[][] edgeMask = new boolean[panorama.height][panorama.width];
            for (int y = 0; y < panorama.height; ++ y) {
                for (int x = 1; x < panorama.width; ++x) {
                    if (sad[x][y] > threshold) {
                        edgeMask[y][x] = true;
                    }
                }
            }
            pm.worked(1);

            return edgeMask;
        } finally {
            pm.done();
        }
    }

    private double getDouble(Tile tile, int x, int y) {
        if (slitCorrection) {
            return tile.getSampleDouble(x, y) / slitNoiseFactors[x];
        } else {
            return tile.getSampleDouble(x, y);
        }
    }

    private void setDouble(Tile tile, int x, int y, double v) {
        if (slitCorrection) {
            tile.setSample(x, y, v / slitNoiseFactors[x]);
        } else {
            tile.setSample(x, y, v);
        }
    }

    private Tile getSceneTile(Band band) throws OperatorException {
        return getSourceTile(band, new Rectangle(0, 0, band.getSceneRasterWidth(), band.getSceneRasterHeight()));
    }

    private static double getEdgeDetectionThreshold(Product product) throws OperatorException {
        // todo - store this map as resource
        final Map<String, Double> thresholdMap = new HashMap<String, Double>();
        thresholdMap.put("1", 0.08);
        thresholdMap.put("2", 0.05);
        thresholdMap.put("3", 0.08);
        thresholdMap.put("3A", 0.08);
        thresholdMap.put("4", 0.08);
        thresholdMap.put("5", 0.08);

        final String mode = getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);

        if (thresholdMap.containsKey(mode)) {
            return thresholdMap.get(mode);
        } else {
            throw new OperatorException(MessageFormat.format(
                    "could not get edge detection threshold because CHRIS Mode ''{0}'' is not known", mode));
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

    // todo -- move
    private static double getAnnotationDouble(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
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
        element.setAttributeString(name, value);
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
        final InputStream is = DestripingFactorsOp.class.getResourceAsStream(name);

        if (is == null) {
            throw new OperatorException(MessageFormat.format("resource {0} not found", name));
        }
        try {
            return new FileCacheImageInputStream(is, null);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format(
                    "could not create image input stream for resource {0}", name), e);
        }
    }


    public static class Spi extends AbstractOperatorSpi {

        public Spi() {
            super(DestripingFactorsOp.class, "DestripingFactors");
            // todo -- set description etc.
        }
    }


    /**
     * Image panorama.
     */
    private static class Panorama {

        public int width;
        public int height;
        private Rectangle[] rectangles;

        public Panorama(Product[] products) throws OperatorException {
            width = products[0].getSceneRasterWidth();

            for (Product product : products) {
                if (width != product.getSceneRasterWidth()) {
                    throw new OperatorException("input products have inconsistent raster widths");
                }
                height += product.getSceneRasterHeight();
            }

            rectangles = new Rectangle[products.length];

            for (int i = 0, y = 0; i < products.length; ++i) {
                rectangles[i] = new Rectangle(0, y, width, y += products[i].getSceneRasterHeight());
            }
        }

        public final int getY(int i, int y) {
            return rectangles[i].y + y;
        }
    }

}
