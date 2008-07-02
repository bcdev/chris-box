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
import static java.lang.Math.PI;
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

    private static final double REFL_SCALING_FACTOR = 1.0E-4;
    private static final double REFL_NO_DATA_VALUE = 0.0;

    private static final double O2_A_WAVELENGTH = 760.5;
    private static final double O2_B_WAVELENGTH = 687.5;

    private static final double O2_A_BANDWIDTH = 1.5;
    private static final double O2_B_BANDWIDTH = 1.5;

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
    private double wvIni;

    @Parameter(defaultValue = "false",
               description = "If 'false' no water vapour map is generated for modes 1, 3 and 5.")
    private boolean wvMap;

    @Parameter(defaultValue = "0.05",
               description = "Cloud product threshold for generating the cloud mask.")
    private double cloudProductThreshold;

    private transient Band[] radianceBands;
    private transient Band[] maskBands;
    private transient Band[] reflBands;
    private transient Band cloudProductBand;

    private transient RenderedImage cloudMaskImage;
    private transient ModtranLookupTable lut;

    private transient double[][] lutFilterMatrix;

    private double vza;
    private double sza;
    private double ada;
    private double alt;

    private double cwv;
    private double toaRadianceMultiplier;

    private double[][] lutValuesInt;

    private double[] lpwInt;
    private double[] eglInt;
    private double[] sabInt;

    private double[] radianceWavelenghts;
    private double[] radianceBandwidths;
    private int o2a;
    private int o2b;
    private double[] lpw;
    private double[] egl;
    private double[] sab;

    @Override
    public void initialize() throws OperatorException {
        // get source bands
        radianceBands = OpUtils.findBands(sourceProduct, "radiance");
        maskBands = OpUtils.findBands(sourceProduct, "mask");
        cloudProductBand = sourceProduct.getBand("cloud_product");

        if (cloudProductBand == null) {
            throw new OperatorException("band 'cloud_product' does not exists.");
        }
        // todo - further validation

        // get annotations
        final double vaa = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_OBSERVATION_AZIMUTH_ANGLE);
        final double saa = OpUtils.getAnnotationDouble(sourceProduct,
                                                       ChrisConstants.ATTR_NAME_SOLAR_AZIMUTH_ANGLE);
        vza = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_OBSERVATION_ZENITH_ANGLE);
        sza = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
        ada = OpUtils.getAzimuthalDifferenceAngle(vaa, saa);
        alt = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_TARGET_ALT);

        // create target product
        final Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                 ProgressMonitor pm) throws OperatorException {
        synchronized (this) {
            if (lut == null) {
                init24();
            }
        }

        computeTileStack24(targetTileMap, targetRectangle, pm);

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
        lutFilterMatrix = null;
        lut = null;

        cloudMaskImage = null;

        reflBands = null;
        maskBands = null;
        radianceBands = null;
    }

    private Product createTargetProduct() {
        final Product targetProduct = new Product("CHRIS_BOA_REFL", "CHRIS_BOA_REFL",
                                                  sourceProduct.getSceneRasterWidth(),
                                                  sourceProduct.getSceneRasterHeight());
        // set start and stop times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // add reflectance bands
        reflBands = new Band[radianceBands.length];
        for (int i = 0; i < radianceBands.length; ++i) {
            final Band radianceBand = radianceBands[i];
            final Band reflBand = new Band(radianceBand.getName().replaceAll("radiance", "refl"),
                                           ProductData.TYPE_INT16,
                                           radianceBand.getRasterWidth(),
                                           radianceBand.getRasterHeight());

            reflBand.setDescription(MessageFormat.format("Surface reflectance for spectral band {0}", i + 1));
            reflBand.setUnit("dl");
            reflBand.setScalingFactor(REFL_SCALING_FACTOR);
            reflBand.setValidPixelExpression(radianceBand.getValidPixelExpression());
            reflBand.setSpectralBandIndex(radianceBand.getSpectralBandIndex());
            reflBand.setSpectralWavelength(radianceBand.getSpectralWavelength());
            reflBand.setSpectralBandwidth(radianceBand.getSpectralBandwidth());
            reflBand.setNoDataValue(REFL_NO_DATA_VALUE);
            reflBand.setNoDataValueUsed(true);
            // todo - set solar flux ?

            targetProduct.addBand(reflBand);
            reflBands[i] = reflBand;
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

    private void init24() {
        try {
            lut = new ModtranLookupTableReader().readLookupTable();
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }
        // spectrum mask
        cloudMaskImage = CloudMaskOpImage.createImage(cloudProductBand, cloudProductThreshold);

        final int day = OpUtils.getAcquisitionDay(sourceProduct);
        toaRadianceMultiplier = 0.001 / OpUtils.getSolarIrradianceCorrectionFactor(day);

        radianceWavelenghts = OpUtils.getWavelenghts(radianceBands);
        radianceBandwidths = OpUtils.getBandwidths(radianceBands);
        // todo - properly initialize water vapour column
        cwv = wvIni;

        final double[][] rtmTable = lut.getTable(vza, sza, ada, alt, aot550, cwv);
        final Resampler resampler = new Resampler(lut.getWavelengths(), radianceWavelenghts, radianceBandwidths);

        lpwInt = new double[radianceWavelenghts.length];
        eglInt = new double[radianceWavelenghts.length];
        sabInt = new double[radianceWavelenghts.length];

        resampler.resample(rtmTable[ModtranLookupTable.LPW], lpwInt);
        resampler.resample(rtmTable[ModtranLookupTable.EGL], eglInt);
        resampler.resample(rtmTable[ModtranLookupTable.SAB], sabInt);

        o2a = OpUtils.findBandIndex(radianceBands, O2_A_WAVELENGTH, O2_A_BANDWIDTH);
        o2b = OpUtils.findBandIndex(radianceBands, O2_B_WAVELENGTH, O2_B_BANDWIDTH);
    }

    private void computeTileStack24(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
        final int tileWork = targetRectangle.height;

        try {
            // Inversion of Lambertian equation
            ac24(targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork * reflBands.length));
            // Polishing of spikes for O2-A and O2-B absorption features
            if (o2a != -1) {
                interpolateReflTile(o2a, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
            }
            if (o2b != -1) {
                interpolateReflTile(o2b, targetTileMap, targetRectangle, SubProgressMonitor.create(pm, tileWork));
            }
            if (performAdjacencyCorrection) {
                // todo - adjacency correction
            }
        } finally {
            pm.done();
        }
    }

    private void ac24(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) {
        try {
            pm.beginTask("Computing surface reflectances...", targetRectangle.height * reflBands.length);

            final Raster cloudMaskRaster = cloudMaskImage.getData(targetRectangle);

            for (int i = 0; i < reflBands.length; i++) {
                final Tile radianceTile = getSourceTile(radianceBands[i], targetRectangle, pm);
                final Tile reflTile = targetTileMap.get(reflBands[i]);

                for (final Tile.Pos pos : reflTile) {
                    if (pos.x == targetRectangle.x) {
                        checkForCancelation(pm);
                    }
                    final int cloudMask = cloudMaskRaster.getSample(pos.x, pos.y, 0);
                    if (cloudMask == 0 || cloudMask == 256) {
                        final double radiance = radianceTile.getSampleDouble(pos.x, pos.y);
                        final double term = PI * (radiance * toaRadianceMultiplier) / eglInt[i];
                        final double refl = term / (1.0 + term * sabInt[i]);

                        reflTile.setSample(pos.x, pos.y, refl);
                    } else {
                        reflTile.setSample(pos.x, pos.y, REFL_NO_DATA_VALUE);
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

    private void interpolateReflTile(int bandIndex, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                     ProgressMonitor pm) {
        try {
            pm.beginTask("Interpolating surface reflectances", targetRectangle.height);

            final Tile innerTile = targetTileMap.get(reflBands[bandIndex]);
            final Tile lowerTile = targetTileMap.get(reflBands[bandIndex - 1]);
            final Tile upperTile = targetTileMap.get(reflBands[bandIndex + 1]);

            final double innerWavelength = reflBands[bandIndex].getSpectralWavelength();
            final double lowerWavelength = reflBands[bandIndex - 1].getSpectralWavelength();
            final double upperWavelength = reflBands[bandIndex + 1].getSpectralWavelength();

            final double w = (innerWavelength - lowerWavelength) / (upperWavelength - lowerWavelength);

            for (final Tile.Pos pos : innerTile) {
                if (pos.x == targetRectangle.x) {
                    checkForCancelation(pm);
                }

                final double lowerSample = lowerTile.getSampleDouble(pos.x, pos.y);
                final double upperSample = upperTile.getSampleDouble(pos.x, pos.y);
                innerTile.setSample(pos.x, pos.y, lowerSample + (upperSample - lowerSample) * w);

                if (pos.x == targetRectangle.x + targetRectangle.width - 1) {
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeBoaReflectancesOp.class);
        }
    }
}
