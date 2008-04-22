package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Math.*;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for computing TOA reflectances from CHRIS response corrected
 * images.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ComputeReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Computes TOA reflectances from a CHRIS/PROBA RCI.")
public class ComputeReflectancesOp extends Operator {

    private static final double INVERSE_SCALING_FACTOR = 10000.0;

    @SourceProduct(alias = "input", type = "CHRIS_M[1-5][A0]?_NR")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "true")
    private boolean copyRadianceBands;
    private transient Map<Band, Band> sourceBandMap;
    private transient Map<Band, Double> conversionFactorMap;

    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        sourceBandMap = new HashMap<Band, Band>();
        conversionFactorMap = new HashMap<Band, Double>();

        final double solarZenithAngle = getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
        final double[][] table = readThuillierTable();
        final int day = getAcquisitionDay(sourceProduct);
        computeSolarIrradianceTable(table, day);

        final String name = sourceProduct.getName().replace("_NR", "_REFL");
        final String type = sourceProduct.getProductType().replace("_NR", "_REFL");
        targetProduct = new Product(name, type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        if (copyRadianceBands) {
            for (final Band sourceBand : sourceProduct.getBands()) {
                if (sourceBand.getName().startsWith("radiance")) {
                    final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                    final double solarIrradiance = getAverageValue(table,
                                                                   sourceBand.getSpectralWavelength(),
                                                                   sourceBand.getSpectralBandwidth());
                    targetBand.setSolarFlux((float) solarIrradiance);
                    sourceBandMap.put(targetBand, sourceBand);
                }
            }
        }
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("radiance")) {
                final Band targetBand = new Band(sourceBand.getName().replaceFirst("radiance", "reflectance"),
                                                 ProductData.TYPE_INT16,
                                                 sourceBand.getSceneRasterWidth(),
                                                 sourceBand.getSceneRasterHeight());

                targetBand.setDescription(MessageFormat.format(
                        "Reflectance for spectral band {0}", sourceBand.getSpectralBandIndex() + 1));
                targetBand.setUnit("dl");
                targetBand.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
                targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
                targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
                final double solarIrradiance = getAverageValue(table,
                                                               sourceBand.getSpectralWavelength(),
                                                               sourceBand.getSpectralBandwidth());
                targetBand.setSolarFlux((float) solarIrradiance);
                targetProduct.addBand(targetBand);
                sourceBandMap.put(targetBand, sourceBand);

                final double conversionFactor = PI / (cos(toRadians(solarZenithAngle)) * 1000.0 * solarIrradiance);
                conversionFactorMap.put(targetBand, conversionFactor);
            }
        }
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("mask")) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                final double solarIrradiance = getAverageValue(table,
                                                               sourceBand.getSpectralWavelength(),
                                                               sourceBand.getSpectralBandwidth());
                targetBand.setSolarFlux((float) solarIrradiance);
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
                }
                sourceBandMap.put(targetBand, sourceBand);
            }
        }

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (targetBand.getName().startsWith("reflectance")) {
            computeReflectances(targetBand, targetTile, pm);
        } else {
            final Band sourceBand = sourceBandMap.get(targetBand);
            final Tile sourceTile = getSourceTile(sourceBand, targetTile.getRectangle(), pm);

            targetTile.setRawSamples(sourceTile.getRawSamples());
        }
    }

    @Override
    public void dispose() {
        sourceBandMap.clear();
        sourceBandMap = null;

        conversionFactorMap.clear();
        conversionFactorMap = null;
    }

    private void computeReflectances(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("computing reflectances...", targetTile.getHeight());
        try {
            final Band sourceBand = sourceBandMap.get(targetBand);
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle, pm);

            final int[] sourceSamples = sourceTile.getDataBufferInt();
            final short[] targetSamples = targetTile.getDataBufferShort();

            final double conversionFactor = conversionFactorMap.get(targetBand);

            int sourceOffset = sourceTile.getScanlineOffset();
            int sourceStride = sourceTile.getScanlineStride();
            int targetOffset = targetTile.getScanlineOffset();
            int targetStride = targetTile.getScanlineStride();

            for (int y = 0; y < targetTile.getHeight(); ++y) {
                int sourceIndex = sourceOffset;
                int targetIndex = targetOffset;
                
                for (int x = 0; x < targetTile.getWidth(); ++x) {
                    checkForCancelation(pm);

                    targetSamples[targetIndex] = (short) (INVERSE_SCALING_FACTOR * sourceSamples[sourceIndex]
                            * conversionFactor + 0.5);
                    ++sourceIndex;
                    ++targetIndex;
                }
                sourceOffset += sourceStride;
                targetOffset += targetStride;

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    static void assertValidity(Product product) {
        if (!product.getProductType().matches("CHRIS_M[1-5][A0]?_NR")) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not of appropriate type", product.getName()));
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
    private static double getAnnotationDouble(Product product, String name) throws OperatorException {
        final String string = getAnnotationString(product, name);

        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    // todo - move or make an averager class
    private static double getAverageValue(double[][] table, double wavelength, double width) {
        final double[] x = table[0];
        final double[] y = table[1];

        double ws = 0.0;
        double ys = 0.0;

        for (int i = 0; i < table[0].length; ++i) {
            if (x[i] > wavelength + width) {
                break;
            }
            if (x[i] > wavelength - width) {
                final double w = 1.0 / pow(1.0 + abs(2.0 * (x[i] - wavelength) / width), 4.0);

                ys += y[i] * w;
                ws += w;
            }
        }

        return ys / ws;
    }


    /**
     * Computes the solar irradiance for a given acquisition day.
     *
     * @param table the nominal solar irradiance table. On output contains the
     *              solar irradiance for the given acquisition day.
     * @param day   the acquisition day number.
     */
    private static void computeSolarIrradianceTable(double[][] table, int day) {
        final double[] irradiances = table[1];

        final double e = 0.01673;
        final double factor = 1.0 / sqr(1.0 - e * cos(toRadians(0.9856 * (day - 4))));

        for (int i = 0; i < irradiances.length; ++i) {
            irradiances[i] *= factor;
        }
    }

    private static int getAcquisitionDay(Product product) {
        final ProductData.UTC utc = product.getStartTime();

        if (utc != null) {
            return utc.getAsCalendar().get(Calendar.DAY_OF_YEAR);
        } else {
            throw new OperatorException(MessageFormat.format(
                    "no date for product ''{0}''", product.getName()));
        }
    }

    // todo - generalize
    static double[][] readThuillierTable() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("thuillier.img");

        try {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("could not read extraterrestrial solar irradiance table", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // todo - move
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
        final InputStream is = ComputeReflectancesOp.class.getResourceAsStream(name);

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

    private static double sqr(double x) {
        return x == 0.0 ? 0.0 : x * x;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeReflectancesOp.class);
        }
    }
}
