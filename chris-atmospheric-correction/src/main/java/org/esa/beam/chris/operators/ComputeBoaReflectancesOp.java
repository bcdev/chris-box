/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.chris.operators.internal.Roots;
import org.esa.beam.chris.operators.internal.SimpleLinearRegression;
import org.esa.beam.chris.operators.internal.UnivariateFunction;
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
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Operator for performing the CHRIS atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.ComputeBoaReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Computes surface reflectances from a CHRIS/PROBA RCI with cloud product.")
public class ComputeBoaReflectancesOp extends Operator {

    private static final double BOA_REFL_SCALING_FACTOR = 1.0E-4;
    private static final double BOA_REFL_NO_DATA_VALUE = 0.0;

    private static final double RED_WAVELENGTH = 688.0;
    private static final double NIR_WAVELENGTH = 780.0;

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "false")
    private boolean performSpectralPolishing;

    @Parameter(defaultValue = "false")
    private boolean performAdjacencyCorrection;

    @Parameter(defaultValue = "0",
               interval = "[0.0, 1.0]",
               description = "Value of the aerosol optical thickness (AOT) at 550 nm. If nonzero, AOT retrieval is disabled.")
    private double aot550;

    @Parameter(defaultValue = "0.0",
               interval = "[0.0, 5.0]",
               description = "Initial water vapour (WV) column guess used for WV retrieval.")
    private double cwvIni;

    @Parameter(defaultValue = "false",
               description = "If 'false' no water vapour map is generated for modes 1, 3 and 5.")
    private boolean wvMap;

    @Parameter(defaultValue = "0.05",
               description = "Cloud product threshold for generating the cloud mask.")
    private double cloudProductThreshold;

    private transient Band[] toaBands;
    private transient Band[] maskBands;
    private transient Band[] boaBands;

    private transient Band cloudProductBand;

    private transient RenderedImage hyperMaskImage;
    private transient RenderedImage cloudMaskImage;

    private transient ModtranLookupTable modtranLookupTable;

    private transient int mode;
    private transient double[] nominalWavelengths;

    private transient Ac ac;

    @Override
    public void initialize() throws OperatorException {
        // get source bands
        toaBands = OpUtils.findBands(sourceProduct, "radiance");
        maskBands = OpUtils.findBands(sourceProduct, "mask");
        cloudProductBand = sourceProduct.getBand("cloud_product");

        if (cloudProductBand == null) {
            throw new OperatorException("band 'cloud_product' does not exists.");
        }
        // todo - further validation

        // create target product
        final Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        synchronized (this) {
            if (modtranLookupTable == null) {
                initialize2();
            }
        }

        ac.computeTileStack(targetTileMap, targetRectangle, pm);

        // Compute non-reflectance bands
        for (final Band band : targetTileMap.keySet()) {
            if (band.getName().startsWith("refl")) {
                continue;
            }
            final Band sourceBand = sourceProduct.getBand(band.getName());
            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle, pm);
            final Tile targetTile = targetTileMap.get(band);

            targetTile.setRawSamples(sourceTile.getRawSamples());
        }
    }

    @Override
    public void dispose() {
        ac = null;
        modtranLookupTable = null;

        mode = 0;
        nominalWavelengths = null;

        hyperMaskImage = null;
        cloudMaskImage = null;

        cloudProductBand = null;

        boaBands = null;
        maskBands = null;
        toaBands = null;
    }

    private void initialize2() {
        try {
            modtranLookupTable = new ModtranLookupTableReader().readModtranLookupTable();
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }

        // create mask images
        hyperMaskImage = HyperMaskOpImage.createImage(maskBands);
        cloudMaskImage = CloudMaskOpImage.createImage(cloudProductBand, cloudProductThreshold);

        // create resampler factory
        nominalWavelengths = OpUtils.getWavelenghts(toaBands);
        final ResamplerFactory resamplerFactory = new ResamplerFactory(modtranLookupTable.getWavelengths(),
                                                                       nominalWavelengths,
                                                                       OpUtils.getBandwidths(toaBands));

        // get annotations
        final double vaa = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_OBSERVATION_AZIMUTH_ANGLE);
        final double saa = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_SOLAR_AZIMUTH_ANGLE);
        final double vza = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_OBSERVATION_ZENITH_ANGLE);
        final double sza = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
        final double ada = OpUtils.getAzimuthalDifferenceAngle(vaa, saa);
        final double alt = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_TARGET_ALT);

        // calculate TOA scaling
        final int day = OpUtils.getAcquisitionDay(sourceProduct);
        final double toaScaling = 1.0E-3 / OpUtils.getSolarIrradianceCorrectionFactor(day);

        // create calculator factory
        final RtcTable table = modtranLookupTable.getRtcTable(vza, sza, ada, alt, aot550, cwvIni);
        final CalculatorFactory calculatorFactory = new CalculatorFactory(table, toaScaling);

        mode = OpUtils.getAnnotationInt(sourceProduct, ChrisConstants.ATTR_NAME_CHRIS_MODE, 0, 1);

        final double smileCorrection;
        if (mode == 1 || mode == 5) {
            final SmileCorrectionCalculator scc = new SmileCorrectionCalculator();
            smileCorrection = scc.calculate(toaBands, hyperMaskImage, cloudMaskImage, resamplerFactory,
                                            calculatorFactory);
        } else {
            smileCorrection = 0.0;
        }
        final Resampler resampler = resamplerFactory.createResampler(smileCorrection);

        // create atmospheric correction
        switch (mode) {
            case 1:
            case 3:
            case 5:
                ac = new Ac135(new CalculatorFactoryCwv(modtranLookupTable, resampler, vza, sza, ada, alt, aot550,
                                                        toaScaling));
                break;
            case 2:
            case 4:
                ac = new Ac24(calculatorFactory.createCalculator(resampler));
                break;
        }

        // initialize water vapour column if non-zero
        if (cwvIni == 0.0) {
            // todo - properly initialize water vapour column
        }
    }

    private Product createTargetProduct() {
        final Product targetProduct = new Product("CHRIS_BOA_REFL", "CHRIS_BOA_REFL",
                                                  sourceProduct.getSceneRasterWidth(),
                                                  sourceProduct.getSceneRasterHeight());
        // set start and stop times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // add BOA reflectance bands
        boaBands = new Band[toaBands.length];
        for (int i = 0; i < toaBands.length; ++i) {
            final Band toaBand = toaBands[i];
            final Band boaBand = new Band(toaBand.getName().replaceAll("radiance", "refl"), ProductData.TYPE_INT16,
                                          toaBand.getRasterWidth(),
                                          toaBand.getRasterHeight());

            boaBand.setDescription(MessageFormat.format("Surface reflectance for spectral band {0}", i + 1));
            boaBand.setUnit("dl");
            boaBand.setScalingFactor(BOA_REFL_SCALING_FACTOR);
            boaBand.setValidPixelExpression(toaBand.getValidPixelExpression());
            boaBand.setSpectralBandIndex(toaBand.getSpectralBandIndex());
            boaBand.setSpectralWavelength(toaBand.getSpectralWavelength());
            boaBand.setSpectralBandwidth(toaBand.getSpectralBandwidth());
            boaBand.setNoDataValue(BOA_REFL_NO_DATA_VALUE);
            boaBand.setNoDataValueUsed(true);
            // todo - set solar flux ?

            targetProduct.addBand(boaBand);
            boaBands[i] = boaBand;
        }
        // copy all non-radiance bands from source product to target product
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("radiance")) {
                continue;
            }
            final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
            final FlagCoding flagCoding = sourceBand.getFlagCoding();
            if (flagCoding != null) {
                targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
            }
        }

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        return targetProduct;
    }

    private interface Ac {
        void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm);
    }

    private class Ac24 implements Ac {

        private static final double O2_A_WAVELENGTH = 760.5;
        private static final double O2_B_WAVELENGTH = 687.5;

        private static final double O2_A_BANDWIDTH = 1.5;
        private static final double O2_B_BANDWIDTH = 1.5;

        private final Calculator calculator;

        // indexes for O2 absorption bands
        private final int o2a;
        private final int o2b;

        private Ac24(Calculator calculator) {
            this.calculator = calculator;

            o2a = OpUtils.findBandIndex(toaBands, O2_A_WAVELENGTH, O2_A_BANDWIDTH);
            o2b = OpUtils.findBandIndex(toaBands, O2_B_WAVELENGTH, O2_B_BANDWIDTH);
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            final int tileWork = targetRectangle.height;

            try {
                // atmospheric correction
                ac(targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork * boaBands.length));
                // polishing of spikes for O2-A and O2-B absorption features
                if (o2a != -1) {
                    interpolateTile(o2a, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
                }
                if (o2b != -1) {
                    interpolateTile(o2b, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
                }
                if (performAdjacencyCorrection) {
                    // todo - adjacency correction
                }
            } finally {
                pm.done();
            }
        }

        private void ac(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            try {
                pm.beginTask("Computing surface reflectances...", targetRectangle.height * boaBands.length);

                final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);
                final Raster cloudMaskRaster = cloudMaskImage.getData(targetRectangle);

                for (int i = 0; i < boaBands.length; ++i) {
                    final Tile toaTile = getSourceTile(toaBands[i], targetRectangle, pm);
                    final Tile boaTile = targetTileMap.get(boaBands[i]);

                    for (final Tile.Pos pos : boaTile) {
                        if (pos.x == targetRectangle.x) {
                            checkForCancelation(pm);
                        }
                        final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                        final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);

                        if (hyperMask == 0 && cloudMask == 0) {
                            final double toa = toaTile.getSampleDouble(pos.x, pos.y);
                            final double boa = calculator.getBoaReflectance(i, toa);

                            boaTile.setSample(pos.x, pos.y, boa);
                        } else {
                            boaTile.setSample(pos.x, pos.y, BOA_REFL_NO_DATA_VALUE);
                        }
                        if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                            pm.worked(1);
                        }
                    }
                }
            } finally {
                pm.done();
            }
        }

        private void interpolateTile(int bandIndex, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                     ProgressMonitor pm) {
            try {
                pm.beginTask("Interpolating surface reflectances", targetRectangle.height);

                final Tile interTile = targetTileMap.get(boaBands[bandIndex]);
                final Tile lowerTile = targetTileMap.get(boaBands[bandIndex - 1]);
                final Tile upperTile = targetTileMap.get(boaBands[bandIndex + 1]);

                final double innerWavelength = boaBands[bandIndex].getSpectralWavelength();
                final double lowerWavelength = boaBands[bandIndex - 1].getSpectralWavelength();
                final double upperWavelength = boaBands[bandIndex + 1].getSpectralWavelength();

                final double w = (innerWavelength - lowerWavelength) / (upperWavelength - lowerWavelength);

                for (final Tile.Pos pos : interTile) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }

                    final double lowerSample = lowerTile.getSampleDouble(pos.x, pos.y);
                    final double upperSample = upperTile.getSampleDouble(pos.x, pos.y);
                    interTile.setSample(pos.x, pos.y, lowerSample + (upperSample - lowerSample) * w);

                    if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                        pm.worked(1);
                    }
                }
            } finally {
                pm.done();
            }
        }
    }


    private class Ac135 implements Ac {

        private static final int WV_RETRIEVAL_MAX_ITER = 10000;

        private static final double WV_A_LOWER_BOUND = 770.0;
        private static final double WV_A_UPPER_BOUND = 890.0;
        private static final double WV_B_UPPER_BOUND = 921.0;

        private final CalculatorFactoryCwv calculatorFactory;

        // minimum and maximum water vapour columns
        private final double cwvMin;
        private final double cwvMax;

        // indexes for water vapour absorption bands
        private final int lowerWva;
        private final int upperWva;
        private final int upperWvb;

        private Ac135(CalculatorFactoryCwv calculatorFactory) {
            this.calculatorFactory = calculatorFactory;

            cwvMin = modtranLookupTable.getDimension(ModtranLookupTable.CWV).getMin();
            cwvMax = modtranLookupTable.getDimension(ModtranLookupTable.CWV).getMax();

            int lowerWva = -1;
            int upperWva = -1;
            int upperWvb = -1;
            for (int i = 0; i < nominalWavelengths.length; ++i) {
                if (nominalWavelengths[i] >= WV_A_LOWER_BOUND) {
                    lowerWva = i;
                    break;
                }
            }
            for (int i = lowerWva + 1; i < nominalWavelengths.length; ++i) {
                if (nominalWavelengths[i] <= WV_A_UPPER_BOUND) {
                    upperWva = i;
                } else {
                    break;
                }
            }
            for (int i = upperWva + 1; i < nominalWavelengths.length; ++i) {
                if (nominalWavelengths[i] <= WV_B_UPPER_BOUND) {
                    upperWvb = i;
                } else {
                    break;
                }
            }
            if (lowerWva == -1 || upperWva == -1 || upperWvb == -1) {
                throw new OperatorException("No water vapour absorption bands.");
            }

            this.lowerWva = lowerWva;
            this.upperWva = upperWva;
            this.upperWvb = upperWvb;
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            final int tileWork = targetRectangle.height;

            try {
                // Inversion of Lambertian equation
                ac(targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork * boaBands.length));
                // Polishing of spikes for O2-A and O2-B absorption features
                if (performAdjacencyCorrection) {
                    // todo - adjacency correction
                }
                if ((mode == 1 || mode == 2)) {
                    if (performSpectralPolishing) {
                        // todo - spectral polishing
                    }
                }
            } finally {
                pm.done();
            }
        }

        public void ac(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            try {
                pm.beginTask("Computing surface reflectances...", targetRectangle.height * boaBands.length);

                final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);
                final Raster cloudMaskRaster = cloudMaskImage.getData(targetRectangle);

                final Tile[] toaTiles = new Tile[toaBands.length];
                final Tile[] boaTiles = new Tile[boaBands.length];

                for (int i = 0; i < toaBands.length; i++) {
                    toaTiles[i] = getSourceTile(toaBands[i], targetRectangle, pm);
                }
                for (int i = 0; i < boaBands.length; i++) {
                    boaTiles[i] = targetTileMap.get(boaBands[i]);
                }

                final double[] toa = new double[toaBands.length];
                final double[] boa = new double[toaBands.length];

                for (final Tile.Pos pos : boaTiles[0]) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }
                    final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                    final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);

                    if (hyperMask == 0 && cloudMask == 0) {
                        for (int i = 0; i < toaTiles.length; i++) {
                            toa[i] = toaTiles[i].getSampleDouble(pos.x, pos.y);
                        }
                        wv(toa, boa);
                        for (int i = 0; i < boaTiles.length; i++) {
                            boaTiles[i].setSample(pos.x, pos.y, boa[i]);
                        }
                    } else {
                        for (final Tile boaTile : boaTiles) {
                            boaTile.setSample(pos.x, pos.y, BOA_REFL_NO_DATA_VALUE);
                        }
                    }
                    if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                        pm.worked(1);
                    }
                }
            } finally {
                pm.done();
            }
        }

        private void wv(final double[] toa, final double[] boa) {
            final Calculator calculator = calculatorFactory.createCalculator(cwvIni);
            calculator.calculateBoaReflectances(toa, boa, lowerWva, upperWva + 1);

            final SimpleLinearRegression lg = new SimpleLinearRegression(nominalWavelengths, boa, lowerWva,
                                                                         upperWva + 1);
            final double a = lg.getSlope();
            final double b = lg.getIntercept();

            for (int i = upperWva + 1; i < upperWvb + 1; ++i) {
                boa[i] = a * nominalWavelengths[i] + b;
            }

            final Roots.Bracket bracket = new Roots.Bracket(cwvMin, cwvMax);
            final double[] sim = new double[toa.length];

            final UnivariateFunction function = new UnivariateFunction() {
                @Override
                public double value(double cwv) {
                    final Calculator calculator = calculatorFactory.createCalculator(cwv);
                    calculator.calculateToaRadiances(boa, sim, upperWva + 1, upperWvb + 1);

                    double sum = 0.0;
                    for (int i = upperWva + 1; i < upperWvb + 1; ++i) {
                        sum += toa[i] - sim[i];
                    }

                    return sum;
                }
            };

            Roots.brent(function, bracket, WV_RETRIEVAL_MAX_ITER);
            calculatorFactory.createCalculator(bracket.root).calculateBoaReflectances(toa, boa);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeBoaReflectancesOp.class);
        }
    }
}
