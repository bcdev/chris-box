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
import org.esa.beam.chris.operators.internal.ModtranLookupTable;
import org.esa.beam.chris.operators.internal.ModtranLookupTableReader;
import org.esa.beam.chris.operators.internal.RtcTable;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.chris.util.math.internal.*;
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

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.OpImage;
import java.awt.*;
import java.awt.image.Raster;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * Operator for performing the CHRIS atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.ComputeSurfaceReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Computes surface reflectances from a CHRIS/PROBA RCI with cloud product.")
public class ComputeSurfaceReflectancesOp extends Operator {
    // the number of dark pixels
    private static final int DARK_PIXEL_COUNT = 100;
    // target band names
    private static final String SURFACE_REFL = "reflectance";

    private static final String WATER_VAPOUR = "water_vapour";
    // target band scaling factors
    private static final double SURFACE_REFL_SCALING_FACTOR = 1.0E-4;

    private static final double WATER_VAPOUR_SCALING_FACTOR = 2.0E-4;

    @SourceProduct(alias = "source", type = "CHRIS_M.*")
    private Product sourceProduct;

    @Parameter(defaultValue = "0.0",
               interval = "[0.0, 1.0]",
               label = "Aerosol optical thickness",
               description = "The value of the aerosol optical thickness (AOT) at 550 nm.")
    private double aot550;

    @Parameter(defaultValue = "1.0",
               interval = "[0.0, 5.0]",
               label = "Initial water vapour column (g cm-2)",
               description = "The initial guess of the water vapour (WV) column (g cm-2) used for WV retrieval.")
    private double cwvIni;

    @Parameter(defaultValue = "0.05",
               interval = "[0.0, 1.0]",
               label = "Cloud product threshold",
               description = "The threshold used for deciding whether a pixel is cloudy or not. Pixels with cloud product values exceeding the threshold are considered as cloudy.")
    private double cloudProductThreshold;

    @Parameter(defaultValue = "false",
               label = "Perform adjacency correction",
               description = "If 'true' an adjacency correction is performed.")
    private boolean performAdjacencyCorrection;

    @Parameter(defaultValue = "false",
               label = "Perform spectral polishing",
               description = "If 'false' no spectral polishing is done for modes 1, 3 and 5.")
    private boolean performSpectralPolishing;

    @Parameter(defaultValue = "false",
               label = "Generate water vapour map",
               description = "If 'true' a water vapour map is generated for modes 1, 3 and 5.")
    private boolean generateWvMap;
    // source bands
    private transient Band[] toaBands;
    private transient Band[] toaMaskBands;
    private transient Band cloudProductBand;
    // target bands
    private transient Band[] rhoBands;

    private transient Band wvBand;
    private transient OpImage hyperMaskImage;
    private transient OpImage cloudMaskImage;

    private transient OpImage waterMaskImage;

    private transient int mode;
    private transient double[] nominalWavelengths;

    private transient double[] nominalBandwidths;
    private transient Ac ac;
    private transient double smileCorrection;
    private transient double[] lpwCor;

    @Override
    public void initialize() throws OperatorException {
        // get source bands
        toaBands = OpUtils.findBands(sourceProduct, "radiance");
        toaMaskBands = OpUtils.findBands(sourceProduct, "mask");
        cloudProductBand = sourceProduct.getBand("cloud_product");

        // get CHRIS mode
        mode = OpUtils.getAnnotationInt(sourceProduct, ChrisConstants.ATTR_NAME_CHRIS_MODE, 0, 1);

        if (mode < 1 || mode > 5) {
            throw new OperatorException(MessageFormat.format(
                    "unsupported CHRIS mode: {0} = {1}", ChrisConstants.ATTR_NAME_CHRIS_MODE, mode));
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
            if (hyperMaskImage == null) {
                initialize2(targetRectangle, pm);
            }
        }

        ac.computeTileStack(targetTileMap, targetRectangle, pm);

        // compute remaining bands
        for (final Band targetBand : targetTileMap.keySet()) {
            final String name = targetBand.getName();
            if (name.startsWith(SURFACE_REFL) || name.equals(WATER_VAPOUR)) {
                continue;
            }

            final Band sourceBand = sourceProduct.getBand(name);
            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle, pm);
            final Tile targetTile = targetTileMap.get(targetBand);

            targetTile.setRawSamples(sourceTile.getRawSamples());
        }
    }

    @Override
    public void dispose() {
        ac = null;

        mode = 0;
        nominalWavelengths = null;

        if (hyperMaskImage != null) {
            hyperMaskImage.dispose();
        }
        if (cloudMaskImage != null) {
            cloudMaskImage.dispose();
        }
        if (waterMaskImage != null) {
            waterMaskImage.dispose();
        }
        hyperMaskImage = null;
        cloudMaskImage = null;
        waterMaskImage = null;

        rhoBands = null;
        wvBand = null;

        toaBands = null;
        toaMaskBands = null;
        cloudProductBand = null;
    }

    private Product createTargetProduct() {
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        final String type = sourceProduct.getProductType() + "_AC";
        final Product targetProduct = new Product("CHRIS_SURFACE_REFL", type, w, h);

        // set start and stop times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // copy flag codings
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        // create valid pixel expression
        final StringBuilder validPixelExpression = new StringBuilder("");
        if (cloudProductBand != null) {
            validPixelExpression.append("cloud_product < ").append(cloudProductThreshold).append(" && ");
        }
        for (final Band band : toaMaskBands) {
            validPixelExpression.append("(").append(band.getName()).append(" & 3)").append(" == 0");

            if (band != toaMaskBands[toaMaskBands.length - 1]) {
                validPixelExpression.append(" && ");
            }
        }

        // add surface reflectance bands
        rhoBands = new Band[toaBands.length];
        for (int i = 0; i < toaBands.length; ++i) {
            final Band toaBand = toaBands[i];
            final String boaBandName = toaBand.getName().replaceAll("radiance", SURFACE_REFL);
            final Band rhoBand = new Band(boaBandName, ProductData.TYPE_INT16, w, h);

            rhoBand.setDescription(MessageFormat.format("Surface reflectance for spectral band {0}", i + 1));
            rhoBand.setUnit("dl");
            rhoBand.setScalingFactor(SURFACE_REFL_SCALING_FACTOR);
            rhoBand.setValidPixelExpression(validPixelExpression.toString());
            rhoBand.setSpectralBandIndex(toaBand.getSpectralBandIndex());
            rhoBand.setSpectralWavelength(toaBand.getSpectralWavelength());
            rhoBand.setSpectralBandwidth(toaBand.getSpectralBandwidth());

            targetProduct.addBand(rhoBand);
            rhoBands[i] = rhoBand;
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

        // add water vapour band, if applicable
        if (mode == 1 || mode == 3 || mode == 5) {
            if (generateWvMap) {
                wvBand = new Band(WATER_VAPOUR, ProductData.TYPE_INT16, w, h);
                wvBand.setUnit("g cm-2");
                wvBand.setScalingFactor(WATER_VAPOUR_SCALING_FACTOR);
                wvBand.setValidPixelExpression(validPixelExpression.toString());

                targetProduct.addBand(wvBand);
            }
        }

        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        // set preferred tile size
        targetProduct.setPreferredTileSize(w, h);

        return targetProduct;
    }

    private void initialize2(Rectangle targetRectangle, ProgressMonitor pm) {
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
        final double alt = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_TARGET_ALT) / 1000.0;

        // get nominal wavelengths and bandwidths
        nominalWavelengths = OpUtils.getWavelenghts(toaBands);
        nominalBandwidths = OpUtils.getBandwidths(toaBands);

        // create mask images
        hyperMaskImage = HyperMaskOpImage.createImage(toaMaskBands);
        if (cloudProductBand != null) {
            cloudMaskImage = CloudMaskOpImage.createImage(cloudProductBand, cloudProductThreshold);
        } else {
            cloudMaskImage = ZeroOpImage.createImage(sourceProduct.getSceneRasterWidth(),
                                                     sourceProduct.getSceneRasterHeight());
        }
        if (mode == 1 || mode == 2 || mode == 3 || mode == 5) {
            final int redIndex = OpUtils.findBandIndex(toaBands, 688.0);
            final int nirIndex = OpUtils.findBandIndex(toaBands, 780.0);

            final double[][] solarIrradianceTable = OpUtils.readThuillierTable();
            final double[] irradiances = new Resampler(solarIrradianceTable[0], nominalWavelengths,
                                                       nominalBandwidths).resample(solarIrradianceTable[1]);
            final double redScaling = Math.PI / (Math.cos(Math.toRadians(sza)) * 1000.0 * irradiances[redIndex]);
            final double nirScaling = Math.PI / (Math.cos(Math.toRadians(sza)) * 1000.0 * irradiances[nirIndex]);

            waterMaskImage = WaterMaskOpImage.createImage(toaBands[redIndex], toaBands[nirIndex], redScaling,
                                                          nirScaling);
        }

        final ModtranLookupTable modtranLookupTable;
        try {
            modtranLookupTable = new ModtranLookupTableReader().readModtranLookupTable();
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }

        // calculate TOA scaling
        final int day = OpUtils.getAcquisitionDay(sourceProduct);
        final double toaScaling = 1.0E-3 / OpUtils.getSolarIrradianceCorrectionFactor(day);

        // create resampler factory
        final ResamplerFactory resamplerFactory = new ResamplerFactory(modtranLookupTable.getWavelengths(),
                                                                       nominalWavelengths,
                                                                       nominalBandwidths);
        // compute initial water vapour column if zero
        if (cwvIni == 0.0) {
            final double cwvMax = 2.0;
            final double cwvMin = 0.5;

            cwvIni = (cwvMax - cwvMin) * Math.sin(day / 365) + cwvMin;
        }

        // compute aerosol optical thickness and path radiance correction
        lpwCor = new double[toaBands.length];
        if (aot550 == 0.0) {
            final RtcTableFactoryAot tableFactory =
                    new RtcTableFactoryAot(modtranLookupTable, resamplerFactory.createResampler(0.0), vza, sza, ada,
                                           alt, cwvIni);
            if (mode == 2) {
                computeAotWater(tableFactory, targetRectangle, pm);
            } else {
                computeAotLand(tableFactory, targetRectangle, pm);
            }
            OpUtils.setAnnotationString(getTargetProduct(), "Aerosol Optical Thickness",
                                        new DecimalFormat("0.000").format(aot550));
        }

        // create calculator factory
        final RtcTable table = modtranLookupTable.getRtcTable(vza, sza, ada, alt, aot550, cwvIni);
        final CalculatorFactory calculatorFactory = new CalculatorFactory(table, lpwCor, toaScaling);

        // calculate smile correction
        if (mode == 1 || mode == 5) {
            final SmileCorrectionCalculator scc = new SmileCorrectionCalculator();
            smileCorrection = scc.calculate(toaBands, hyperMaskImage, cloudMaskImage, resamplerFactory,
                                            calculatorFactory);
            OpUtils.setAnnotationString(getTargetProduct(), "Smile Correction",
                                        new DecimalFormat("0.000").format(smileCorrection));
        } else {
            smileCorrection = 0.0;
        }
        final Resampler resampler = resamplerFactory.createResampler(smileCorrection);

        // create atmospheric correction
        if (mode == 1 || mode == 3 || mode == 5) {
            final CalculatorFactoryCwv ac1CalculatorFactory = new CalculatorFactoryCwv(modtranLookupTable, resampler,
                                                                                       vza, sza, ada, alt, aot550,
                                                                                       lpwCor, toaScaling);
            ac = new Ac1(ac1CalculatorFactory);
        } else {
            ac = new Ac2(calculatorFactory.createCalculator(resampler));
        }
    }

    private void computeAotLand(RtcTableFactoryAot tableFactory, Rectangle targetRectangle, ProgressMonitor pm) {
        int lowerVis = -1;
        int upperVis = -1;
        for (int i = 0; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] >= 420.0) {
                lowerVis = i;
                break;
            }
        }
        for (int i = lowerVis + 1; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] <= 690.0) {
                upperVis = i;
            } else {
                break;
            }
        }

        final double[][] darkPixels = new double[toaBands.length][DARK_PIXEL_COUNT];
        for (double[] samples : darkPixels) {
            Arrays.fill(samples, Double.POSITIVE_INFINITY);
        }

        final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);

        for (int i = 0; i < toaBands.length; ++i) {
            final Tile toaTile = getSourceTile(toaBands[i], targetRectangle, pm);

            for (final Tile.Pos pos : toaTile) {
                if (pos.x == targetRectangle.x) {
                    checkForCancelation(pm);
                }
                final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);

                if ((hyperMask & 3) == 0) {
                    final double toa = toaTile.getSampleDouble(pos.x, pos.y);

                    if (toa > 0.0 && toa < darkPixels[i][DARK_PIXEL_COUNT - 1]) {
                        darkPixels[i][DARK_PIXEL_COUNT - 1] = toa;
                        Arrays.sort(darkPixels[i]);
                    }
                }

                if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                    pm.worked(1);
                }
            }
        }

        double aot = findMaxAot(tableFactory, darkPixels, tableFactory.getMinAot(), 0.05, lowerVis, upperVis);
        if (aot > tableFactory.getMinAot()) {
            aot = findMaxAot(tableFactory, darkPixels, aot, 0.005, lowerVis, upperVis);
        }

        aot550 = aot;

        if (aot550 == tableFactory.getMinAot()) {
            final double fza = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE);

            if (fza >= 55.0 || fza <= -55.0) {
                final RtcTable table = tableFactory.createRtcTable(aot550);

                for (int i = 0; i < lpwCor.length; ++i) {
                    lpwCor[i] = table.getLpw(i) - Statistics.mean(darkPixels[i]);
                }
            }
        }
        for (int i = 0; i < lpwCor.length; ++i) {
            if (lpwCor[i] > 0.0) {
                lpwCor[i] = 1.0;
            } else {
                lpwCor[i] = 0.0;
            }
        }
    }

    private void computeAotWater(RtcTableFactoryAot tableFactory, Rectangle targetRectangle, ProgressMonitor pm) {
        int lowerVis = -1;
        int upperVis = -1;
        for (int i = 0; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] >= 435.0) {
                lowerVis = i;
                break;
            }
        }
        for (int i = lowerVis + 1; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] <= 690.0) {
                upperVis = i;
            } else {
                break;
            }
        }

        final double[][] darkPixels = new double[toaBands.length][DARK_PIXEL_COUNT];
        for (double[] samples : darkPixels) {
            Arrays.fill(samples, Double.POSITIVE_INFINITY);
        }

        final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);
        final Raster waterMaskRaster = waterMaskImage.getData(targetRectangle);

        for (int i = 0; i < toaBands.length; ++i) {
            final Tile toaTile = getSourceTile(toaBands[i], targetRectangle, pm);

            for (final Tile.Pos pos : toaTile) {
                if (pos.x == targetRectangle.x) {
                    checkForCancelation(pm);
                }
                final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                final int waterMask = waterMaskRaster.getSample(pos.x, pos.y, 0);

                if ((hyperMask & 3) == 0 && waterMask != 0) {
                    final double toa = toaTile.getSampleDouble(pos.x, pos.y);

                    if (toa > 0.0 && toa < darkPixels[i][DARK_PIXEL_COUNT - 1]) {
                        darkPixels[i][DARK_PIXEL_COUNT - 1] = toa;
                        Arrays.sort(darkPixels[i]);
                    }
                }

                if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                    pm.worked(1);
                }
            }
        }

        double aot = findMaxAot(tableFactory, darkPixels, tableFactory.getMinAot(), 0.05, lowerVis, upperVis);
        if (aot > tableFactory.getMinAot()) {
            aot = findMaxAot(tableFactory, darkPixels, aot, 0.005, lowerVis, upperVis);
        }

        aot550 = aot;

        if (aot550 == tableFactory.getMinAot()) {
            final double fza = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE);

            if (fza >= 55.0 || fza <= -55.0) {
                final RtcTable table = tableFactory.createRtcTable(aot550);

                for (int i = 0; i < lpwCor.length; ++i) {
                    lpwCor[i] = table.getLpw(i) - Statistics.mean(darkPixels[i]);
                }
                // polynomial fit
                final double[][] p = new double[4][lpwCor.length];
                new LegendrePolynomials().calculate(nominalWavelengths, p);
                new Regression(p).fit(lpwCor, lpwCor);
            }
        }
        for (int i = 0; i < lpwCor.length; ++i) {
            if (lpwCor[i] > 0.0) {
                lpwCor[i] = 1.0;
            } else {
                lpwCor[i] = 0.0;
            }
        }
    }

    private static double findMaxAot(RtcTableFactoryAot tableFactory, double[][] darkPixels, double aotMin,
                                     double aotInc, int lowerVis, int upperVis) {
        int iter = 0;
        int count;

        do {
            final double aot = aotMin + iter * aotInc;
            if (aot > tableFactory.getMaxAot()) {
                break;
            }

            final RtcTable table = tableFactory.createRtcTable(aot);
            count = 0;

            for (int i = lowerVis; i < upperVis + 1; ++i) {
                for (int k = 0; k < DARK_PIXEL_COUNT; ++k) {
                    if (darkPixels[i][k] < table.getLpw(i)) {
                        ++count;
                        break;
                    }
                }
            }
            ++iter;
        } while (count < DARK_PIXEL_COUNT);

        return aotMin + (iter - 1) * aotInc;
    }

    private interface Ac {

        void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm);
    }

    private class Ac1 implements Ac {

        private static final int SPIKY_PIXEL_COUNT = 50;
        private static final int WV_RETRIEVAL_MAX_ITER = 10000;

        private static final double WV_A_LOWER_BOUND = 770.0;
        private static final double WV_A_UPPER_BOUND = 890.0;
        private static final double WV_B_UPPER_BOUND = 921.0;

        private final CalculatorFactoryCwv calculatorFactory;
        // indexes for water vapour absorption bands
        private final int lowerWva;
        private final int upperWva;
        private final int upperWvb;
        // endmember regression used for spectral polishing
        private final Regression endmemberRegression;

        private Ac1(CalculatorFactoryCwv calculatorFactory) {
            this.calculatorFactory = calculatorFactory;

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

            if (performSpectralPolishing && (mode == 1 || mode == 5)) {
                final double[][] endmemberTable = readEndmemberTable();
                final Resampler resampler = new Resampler(endmemberTable[0], nominalWavelengths, nominalBandwidths,
                                                          smileCorrection);

                final double[] constant = new double[nominalWavelengths.length];
                Arrays.fill(constant, 1.0);
                final double[] veg = resampler.resample(endmemberTable[1]);
                final double[] sue = resampler.resample(endmemberTable[2]);

                endmemberRegression = new Regression(constant, veg, sue);
            } else {
                endmemberRegression = null;
            }
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            final int tileWork = targetRectangle.height;

            int totalWork = tileWork * 2;
            if (performAdjacencyCorrection) {
                totalWork += tileWork * rhoBands.length * 2;
            }
            if (performSpectralPolishing && (mode == 1 || mode == 5)) {
                totalWork += tileWork * rhoBands.length * 2;
            }
            try {
                pm.beginTask("Performing atmospheric correction", totalWork);

                // Inversion of Lambertian equation
                final double wv = ac(targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork * 2));
                if (performAdjacencyCorrection) {
                    final AdjacencyCorrection ac = new AdjacencyCorrection(calculatorFactory.createCalculator(wv));
                    ac.computeTileStack(targetTileMap, targetRectangle,
                                        SubProgressMonitor.create(pm, tileWork * rhoBands.length * 2));
                }
                if (performSpectralPolishing && (mode == 1 || mode == 5)) {
                    performSpectralPolishing(targetTileMap, targetRectangle,
                                             SubProgressMonitor.create(pm, tileWork * rhoBands.length * 2));
                }
            } finally {
                pm.done();
            }
        }

        private void performSpectralPolishing(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                              ProgressMonitor pm) {
            try {
                pm.beginTask("Performing spectral polishing", targetRectangle.height * rhoBands.length * 2);

                // 1. Find pixels with spiky spectra
                final Band redBand = OpUtils.findBand(rhoBands, new BandFilter() {
                    @Override
                    public boolean accept(Band band) {
                        return band.getSpectralWavelength() >= 670.0;
                    }
                });
                final Band nirBand = OpUtils.findBand(rhoBands, new BandFilter() {
                    @Override
                    public boolean accept(Band band) {
                        return band.getSpectralWavelength() >= 785.0;
                    }
                });

                final Tile redTile = targetTileMap.get(redBand);
                final Tile nirTile = targetTileMap.get(nirBand);
                final List<SpikyPixel> spikyPixelList = new ArrayList<SpikyPixel>();

                for (final Tile.Pos pos : redTile) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }
                    final double nir = nirTile.getSampleDouble(pos.x, pos.y);
                    final double red = redTile.getSampleDouble(pos.x, pos.y);
                    final double ndvi = (nir - red) / (nir + red);

                    if (ndvi > 0.1 && nir > 0.2 && nir <= 0.75) {
                        spikyPixelList.add(new SpikyPixel(pos, ndvi));
                    }
                    if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                        pm.worked(1);
                    }
                }

                if (spikyPixelList.size() > SPIKY_PIXEL_COUNT) {
                    // 2. Select pixels with reference spectra
                    Collections.sort(spikyPixelList);
                    spikyPixelList.subList(SPIKY_PIXEL_COUNT / 2, spikyPixelList.size() - SPIKY_PIXEL_COUNT / 2).clear();

                    // 3. Calculate smoothed spectra
                    final double[][] originalSpectra = new double[SPIKY_PIXEL_COUNT][rhoBands.length];
                    final double[][] smoothedSpectra = new double[SPIKY_PIXEL_COUNT][rhoBands.length];
                    final double[] c = new double[3];
                    final double[] w = new double[3];

                    for (int i = 0; i < SPIKY_PIXEL_COUNT; i++) {
                        final double[] original = originalSpectra[i];

                        for (int j = 0; j < rhoBands.length; j++) {
                            final Tile rhoTile = targetTileMap.get(rhoBands[j]);
                            final Tile.Pos pos = spikyPixelList.get(i).pos;

                            original[j] = rhoTile.getSampleDouble(pos.x, pos.y);
                        }

                        endmemberRegression.fit(original, smoothedSpectra[i], c, w);
                    }

                    // 4. Calculate calibration factors for individual bands
                    final double[] calibrationFactors = new double[rhoBands.length];
                    final double[] c1 = new double[1];
                    final double[] w1 = new double[1];

                    for (int j = 0; j < rhoBands.length; j++) {
                        final double[] originalSamples = new double[SPIKY_PIXEL_COUNT];
                        final double[] smoothedSamples = new double[SPIKY_PIXEL_COUNT];

                        for (int i = 0; i < SPIKY_PIXEL_COUNT; i++) {
                            originalSamples[i] = originalSpectra[i][j];
                            smoothedSamples[i] = smoothedSpectra[i][j];
                        }

                        new Regression(smoothedSamples).fit(originalSamples, originalSamples, c1, w1);
                        calibrationFactors[j] = 1.0 / c1[0];
                    }

                    final int lowerRed = OpUtils.findBandIndex(rhoBands, new BandFilter() {
                        @Override
                        public boolean accept(Band band) {
                            return band.getSpectralWavelength() >= 694.7;
                        }
                    });
                    final int upperRed = OpUtils.findBandIndex(rhoBands, new BandFilter() {
                        @Override
                        public boolean accept(Band band) {
                            return band.getSpectralWavelength() > 772.5;
                        }
                    });

                    // 5. Special treatment for calibration factors for bands in the red
                    final double[] originalRedCalibrationFactors = Arrays.copyOfRange(calibrationFactors, lowerRed, upperRed);
                    final double[] smoothedRedCalibrationFactors = new double[upperRed - lowerRed];
                    final LowessRegressionWeightCalculator weightCalculator = new LowessRegressionWeightCalculator();
                    final LocalRegressionSmoother smoother = new LocalRegressionSmoother(weightCalculator, 0, 5);
                    smoother.smooth(originalRedCalibrationFactors, smoothedRedCalibrationFactors);

                    for (int i = 0; i < originalRedCalibrationFactors.length; i++) {
                        calibrationFactors[lowerRed + i] = 1.0 + (originalRedCalibrationFactors[i] - smoothedRedCalibrationFactors[i]);
                    }

                    // 6. Recalibrate bands
                    for (int i = 0; i < rhoBands.length; ++i) {
                        final Tile targetTile = targetTileMap.get(rhoBands[i]);
                        for (final Tile.Pos pos : targetTile) {
                            if (pos.x == targetRectangle.x) {
                                checkForCancelation(pm);
                            }

                            final double rho = targetTile.getSampleDouble(pos.x, pos.y);
                            targetTile.setSample(pos.x, pos.y, rho * calibrationFactors[i]);

                            if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                                pm.worked(1);
                            }
                        }
                    }
                }
            } finally {
                pm.done();
            }
        }

        private class SpikyPixel implements Comparable<SpikyPixel> {
            private double ndvi;
            private Tile.Pos pos;

            SpikyPixel(Tile.Pos pos, double ndvi) {
                this.pos = pos;
                this.ndvi = ndvi;
            }

            @Override
            public final int compareTo(SpikyPixel o) {
                return Double.compare(ndvi, o.ndvi);
            }
        }

        public double ac(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            try {
                pm.beginTask("Computing surface reflectances...", targetRectangle.height * 2);

                final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);
                final Raster cloudMaskRaster = cloudMaskImage.getData(targetRectangle);
                final Raster waterMaskRaster = waterMaskImage.getData(targetRectangle);

                final Tile[] toaTiles = new Tile[toaBands.length];
                final Tile[] rhoTiles = new Tile[rhoBands.length];

                for (int i = 0; i < toaBands.length; i++) {
                    toaTiles[i] = getSourceTile(toaBands[i], targetRectangle, pm);
                }
                for (int i = 0; i < rhoBands.length; i++) {
                    rhoTiles[i] = targetTileMap.get(rhoBands[i]);
                }
                final Tile wvTile = targetTileMap.get(wvBand);

                double wvSum = 0.0;
                int wvCount = 0;

                // first pass for land pixels only
                for (final Tile.Pos pos : rhoTiles[0]) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }

                    final double[] toa = new double[toaBands.length];
                    final double[] rho = new double[rhoBands.length];

                    final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                    final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);
                    final int waterMask = waterMaskRaster.getSample(pos.x, pos.y, 0);

                    if ((hyperMask & 3) == 0 && cloudMask == 0 && waterMask == 0) {
                        for (int i = 0; i < toaTiles.length; i++) {
                            toa[i] = toaTiles[i].getSampleDouble(pos.x, pos.y);
                        }

                        final double wv = wv(toa, rho);
                        wvSum += wv;
                        wvCount++;

                        for (int i = 0; i < rhoTiles.length; i++) {
                            rhoTiles[i].setSample(pos.x, pos.y, rho[i]);
                        }
                        if (wvTile != null) {
                            wvTile.setSample(pos.x, pos.y, wv);
                        }
                    }
                    if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                        pm.worked(1);
                    }
                }

                final double wvMean = wvCount > 0 ? wvSum / wvCount : 0.0;
                final Calculator calculator = calculatorFactory.createCalculator(wvMean);

                // second pass for water pixels only
                for (final Tile.Pos pos : rhoTiles[0]) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }
                    final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                    final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);
                    final int waterMask = waterMaskRaster.getSample(pos.x, pos.y, 0);

                    if ((hyperMask & 3) == 0 && cloudMask == 0 && waterMask != 0) {
                        for (int i = 0; i < rhoTiles.length; i++) {
                            final double toa = toaTiles[i].getSampleDouble(pos.x, pos.y);
                            final double rho = calculator.getBoaReflectance(i, toa);

                            rhoTiles[i].setSample(pos.x, pos.y, rho);
                        }
                        if (wvTile != null) {
                            wvTile.setSample(pos.x, pos.y, wvMean);
                        }
                    }
                    if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                        pm.worked(1);
                    }
                }

                return wvMean;
            } finally {
                pm.done();
            }
        }

        /**
         * Simultaneously calculates the surface reflectance spectrum and the columnar
         * water vapour.
         *
         * @param toa the TOA radiance spectrum.
         * @param rho the surface reflectance spectrum calculated.
         *
         * @return the columnar water vapour.
         */
        private double wv(final double[] toa, final double[] rho) {
            final Calculator calculator = calculatorFactory.createCalculator(cwvIni);
            calculator.calculateBoaReflectances(toa, rho, lowerWva, upperWva + 1);

            // 2. Extrapolate surface reflectances from region A to region B
            final SimpleLinearRegression lg = new SimpleLinearRegression(nominalWavelengths, rho,
                                                                         lowerWva, upperWva + 1);
            for (int i = upperWva + 1; i < upperWvb + 1; ++i) {
                rho[i] = lg.getIntercept() + lg.getSlope() * nominalWavelengths[i];
            }

            final Roots.Bracket bracket = new Roots.Bracket(calculatorFactory.getMinCwv(),
                                                            calculatorFactory.getMaxCwv());
            final double[] sim = new double[toa.length];

            // 3. Define the merit-function for retrieving columnar water vapour
            final UnivariateFunction function = new UnivariateFunction() {
                @Override
                public double value(double cwv) {
                    final Calculator calculator = calculatorFactory.createCalculator(cwv);
                    calculator.calculateToaRadiances(rho, sim, upperWva + 1, upperWvb + 1);

                    double sum = 0.0;
                    for (int i = upperWva + 1; i < upperWvb + 1; ++i) {
                        sum += toa[i] - sim[i];
                    }

                    return sum;
                }
            };
            // 4. Calculate columnar water vapour by finding the root of the merit-function
            Roots.brent(function, bracket, WV_RETRIEVAL_MAX_ITER);
            // 5. Calculate surface reflectances
            calculatorFactory.createCalculator(bracket.root).calculateBoaReflectances(toa, rho);

            return bracket.root;
        }
    }

    private class Ac2 implements Ac {

        private static final double O2_A_WAVELENGTH = 760.5;
        private static final double O2_B_WAVELENGTH = 687.5;

        private static final double O2_A_BANDWIDTH = 1.5;
        private static final double O2_B_BANDWIDTH = 1.5;

        private final Calculator calculator;

        // indexes for O2 absorption bands
        private final int o2a;
        private final int o2b;

        private Ac2(Calculator calculator) {
            this.calculator = calculator;

            o2a = OpUtils.findBandIndex(toaBands, O2_A_WAVELENGTH, O2_A_BANDWIDTH);
            o2b = OpUtils.findBandIndex(toaBands, O2_B_WAVELENGTH, O2_B_BANDWIDTH);
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            final int tileWork = targetRectangle.height;

            int totalWork = tileWork * rhoBands.length;
            if (o2a != -1) {
                totalWork += tileWork;
            }
            if (o2b != -1) {
                totalWork += tileWork;
            }
            if (performAdjacencyCorrection) {
                totalWork += tileWork * rhoBands.length * 2;
            }

            try {
                pm.beginTask("Performing atmospheric correction", totalWork);
                // atmospheric correction
                ac(targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork * rhoBands.length));
                // polishing of spikes for O2-A and O2-B absorption features
                if (o2a != -1) {
                    interpolateRhoTile(o2a, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
                }
                if (o2b != -1) {
                    interpolateRhoTile(o2b, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
                }
                if (performAdjacencyCorrection) {
                    final Ac ac = new AdjacencyCorrection(calculator);
                    ac.computeTileStack(targetTileMap, targetRectangle,
                                        SubProgressMonitor.create(pm, tileWork * rhoBands.length * 2));
                }
            } finally {
                pm.done();
            }
        }

        private void ac(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            try {
                pm.beginTask("Computing surface reflectances...", targetRectangle.height * rhoBands.length);

                final Raster hyperMaskRaster = hyperMaskImage.getData(targetRectangle);
                final Raster cloudMaskRaster = cloudMaskImage.getData(targetRectangle);

                for (int i = 0; i < rhoBands.length; ++i) {
                    final Tile toaTile = getSourceTile(toaBands[i], targetRectangle, pm);
                    final Tile rhoTile = targetTileMap.get(rhoBands[i]);

                    for (final Tile.Pos pos : rhoTile) {
                        if (pos.x == targetRectangle.x) {
                            checkForCancelation(pm);
                        }
                        final int hyperMask = hyperMaskRaster.getSample(pos.x, pos.y, 0);
                        final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);

                        if ((hyperMask & 3) == 0 && cloudMask == 0) {
                            final double toa = toaTile.getSampleDouble(pos.x, pos.y);
                            final double rho = calculator.getBoaReflectance(i, toa);

                            rhoTile.setSample(pos.x, pos.y, rho);
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

        private void interpolateRhoTile(int bandIndex, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                        ProgressMonitor pm) {
            try {
                pm.beginTask("Interpolating surface reflectances", targetRectangle.height);

                final Tile interTile = targetTileMap.get(rhoBands[bandIndex]);
                final Tile lowerTile = targetTileMap.get(rhoBands[bandIndex - 1]);
                final Tile upperTile = targetTileMap.get(rhoBands[bandIndex + 1]);

                final double innerWavelength = rhoBands[bandIndex].getSpectralWavelength();
                final double lowerWavelength = rhoBands[bandIndex - 1].getSpectralWavelength();
                final double upperWavelength = rhoBands[bandIndex + 1].getSpectralWavelength();

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

    private class AdjacencyCorrection implements Ac {

        private final Calculator calculator;
        private final int kernelSize;

        private AdjacencyCorrection(Calculator calculator) {
            this.calculator = calculator;

            if (mode == 1) {
                kernelSize = 27;
            } else {
                kernelSize = 59;
            }
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
            try {
                pm.beginTask("Performing adjacency correction", targetRectangle.height * rhoBands.length * 2);

                for (int i = 0; i < rhoBands.length; i++) {
                    final Tile targetTile = targetTileMap.get(rhoBands[i]);
                    final short[] targetSamples = targetTile.getDataBufferShort();
                    final short[][] means = computeMeans(targetTile, targetRectangle,
                                                         SubProgressMonitor.create(pm, targetRectangle.height));

                    int targetLineOffset = targetTile.getScanlineOffset();
                    for (int y = 0; y < targetRectangle.height; y++) {
                        checkForCancelation(pm);

                        int targetPixelIndex = targetLineOffset;
                        for (int x = 0; x < targetRectangle.width; x++) {
                            double rho = rhoBands[i].scale(targetSamples[targetPixelIndex]);
                            rho += calculator.getAdjacencyCorrection(i, rho, rhoBands[i].scale(means[x][y]));
                            targetSamples[targetPixelIndex] = (short) rhoBands[i].scaleInverse(rho);

                            targetPixelIndex++;
                        }
                        targetLineOffset += targetTile.getScanlineStride();

                        pm.worked(1);
                    }

                }
            } finally {
                pm.done();
            }
        }

        private short[][] computeMeans(Tile targetTile, Rectangle targetRectangle, ProgressMonitor pm) {

            final short[][] means = new short[targetRectangle.width][targetRectangle.height];

            try {
                pm.beginTask("Computing smoothed image", targetRectangle.height);

                final int halfKernelSize = kernelSize / 2;
                final short[] targetSamples = targetTile.getDataBufferShort();

                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                    checkForCancelation(pm);

                    final int minY = Math.max(targetRectangle.y, y - halfKernelSize);
                    final int maxY = Math.min(targetRectangle.height, y + halfKernelSize);

                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                        if (targetSamples[y * targetRectangle.width + x] != 0) {
                            final int minX = Math.max(targetRectangle.x, x - halfKernelSize);
                            final int maxX = Math.min(targetRectangle.width, x + halfKernelSize);

                            means[x - targetRectangle.x][y - targetRectangle.y] = computeMean(targetTile,
                                                                                              minX,
                                                                                              minY, maxX, maxY);
                        }
                    }

                    pm.worked(1);
                }

            } finally {
                pm.done();
            }

            return means;
        }

        private short computeMean(Tile targetTile, int minX, int minY, int maxX, int maxY) {
            final short[] targetSamples = targetTile.getDataBufferShort();

            int sum = 0;
            int count = 0;

            int targetLineOffset = targetTile.getScanlineOffset() + minY * targetTile.getScanlineStride();

            for (int y = minY; y < maxY; y++) {
                int targetPixelIndex = targetLineOffset + minX;

                for (int x = minX; x < maxX; x++) {
                    final short targetSample = targetSamples[targetPixelIndex];
                    if (targetSample != 0) {
                        sum += targetSample;
                        count++;
                    }

                    targetPixelIndex++;
                }

                targetLineOffset += targetTile.getScanlineStride();
            }

            if (count > 0) {
                sum /= count;
            }

            return (short) sum;
        }
    }

    static double[][] readEndmemberTable() throws OperatorException {
        final ImageInputStream iis = OpUtils.getResourceAsImageInputStream(ComputeSurfaceReflectancesOp.class,
                                                                           "endmembers.img");

        try {
            final int length = iis.readInt();
            final double[] wavelength = new double[length];
            final double[] rhoVeg = new double[length];
            final double[] rhoSue = new double[length];

            iis.readFully(wavelength, 0, length);
            iis.readFully(rhoVeg, 0, length);
            iis.readFully(rhoSue, 0, length);

            return new double[][]{wavelength, rhoVeg, rhoSue};
        } catch (Exception e) {
            throw new OperatorException("could not read endmember table", e);
        } finally {
            try {
                iis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeSurfaceReflectancesOp.class);
        }
    }
}
