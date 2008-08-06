package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import static java.lang.Math.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for computing TOA reflectances from CHRIS response corrected
 * images.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ComputeToaReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Computes TOA reflectances from a CHRIS/PROBA RCI.")
public class ComputeToaReflectancesOp extends Operator {

    private static final String TOA_REFL = "reflectance";
    private static final double TOA_REFL_SCALING_FACTOR = 10000.0;

    @SourceProduct(alias = "input", type = "CHRIS_M[1-5][A0]?_NR")
    private Product sourceProduct;

    @SuppressWarnings({"FieldCanBeLocal"})
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
               label = "Copy radiance bands",
               description = "If 'true' all radiance bands from the source product are copied to the target product.")
    private boolean copyRadianceBands;
    
    private transient Map<Band, Band> sourceBandMap;
    private transient Map<Band, Double> conversionFactorMap;

    @Override
    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        sourceBandMap = new HashMap<Band, Band>(sourceProduct.getNumBands());
        conversionFactorMap = new HashMap<Band, Double>(sourceProduct.getNumBands());

        final double solarZenithAngle = OpUtils.getAnnotationDouble(sourceProduct,
                                                                    ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
        final double[][] table = OpUtils.readThuillierTable();
        final int day = OpUtils.getAcquisitionDay(sourceProduct);
        computeSolarIrradianceTable(table, day);

        final String name = sourceProduct.getName().replace("_NR", "_TOA_REFL");
        final String type = sourceProduct.getProductType().replace("_NR", "_TOA_REFL");
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
                final Band targetBand = new Band(sourceBand.getName().replaceFirst("radiance", TOA_REFL),
                                                 ProductData.TYPE_INT16,
                                                 sourceBand.getSceneRasterWidth(),
                                                 sourceBand.getSceneRasterHeight());

                targetBand.setDescription(MessageFormat.format(
                        "Reflectance for spectral band {0}", sourceBand.getSpectralBandIndex() + 1));
                targetBand.setUnit("dl");
                targetBand.setScalingFactor(TOA_REFL_SCALING_FACTOR);
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
            } else if (sourceBand.getName().startsWith("mask")) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                final double solarIrradiance = getAverageValue(table,
                                                               sourceBand.getSpectralWavelength(),
                                                               sourceBand.getSpectralBandwidth());
                targetBand.setSolarFlux((float) solarIrradiance);
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
                }
                sourceBandMap.put(targetBand, sourceBand);
            } else {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                sourceBandMap.put(targetBand, sourceBand);
            }
        }

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (targetBand.getName().startsWith(TOA_REFL)) {
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

            final double conversionFactor = conversionFactorMap.get(targetBand);

            for (final Tile.Pos pos : targetTile) {
                if (pos.x == targetRectangle.x) {
                    checkForCancelation(pm);
                }

                final double radiance = sourceTile.getSampleDouble(pos.x, pos.y);
                final double toaRefl = radiance * conversionFactor;
                targetTile.setSample(pos.x, pos.y, toaRefl);

                if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                    pm.worked(1);
                }
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

        final double factor = OpUtils.getSolarIrradianceCorrectionFactor(day);

        for (int i = 0; i < irradiances.length; ++i) {
            irradiances[i] *= factor;
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeToaReflectancesOp.class);
        }
    }
}
