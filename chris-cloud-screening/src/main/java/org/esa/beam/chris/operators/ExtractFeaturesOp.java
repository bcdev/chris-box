package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
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

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Operator for extracting features from TOA reflectances needed for
 * cloud screening.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ExtractFeatures",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Extracts features from TOA reflectances needed for cloud screening.")
public class ExtractFeaturesOp extends Operator {

    private static final double VIS_MIN = 400.0;
    private static final double VIS_MAX = 700.0;

    private static final double INVERSE_SCALING_FACTOR = 10000.0;

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private String targetProductName;

    private transient Band totBr;
    private transient Band totWh;
    private transient Band visBr;
    private transient Band visWh;
    private transient Band nirBr;
    private transient Band nirWh;

    private transient Band[] absorptionFreeBands;
    private transient Band[] visBands;
    private transient Band[] nirBands;

    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        categorizeRadianceBands();

        final String type = sourceProduct.getProductType().replace("_REFL", "_FEAT");
        targetProduct = new Product(targetProductName, type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        totBr = targetProduct.addBand("brightness", ProductData.TYPE_INT16);
        totBr.setDescription("Brightness for visual and NIR bands");
        totBr.setUnit("dl");
        totBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visBr = targetProduct.addBand("brightness_vis", ProductData.TYPE_INT16);
        visBr.setDescription("Brightness for visual bands");
        visBr.setUnit("dl");
        visBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirBr = targetProduct.addBand("brightness_nir", ProductData.TYPE_INT16);
        nirBr.setDescription("Brightness for NIR bands");
        nirBr.setUnit("dl");
        nirBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        totWh = targetProduct.addBand("whiteness", ProductData.TYPE_INT16);
        totWh.setDescription("Whiteness for visual and NIR bands");
        totWh.setUnit("dl");
        totWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visWh = targetProduct.addBand("whiteness_vis", ProductData.TYPE_INT16);
        visWh.setDescription("Whiteness for visual bands");
        visWh.setUnit("dl");
        visBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirWh = targetProduct.addBand("whiteness_nir", ProductData.TYPE_INT16);
        nirWh.setDescription("Whiteness for NIR bands");
        nirWh.setUnit("dl");
        nirWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        pm.beginTask("computing bands...", 6);
        try {
            computeBrightnessAndWhiteness(totBr, totWh, targetTileMap, targetRectangle, absorptionFreeBands,
                                          SubProgressMonitor.create(pm, 2));
            computeBrightnessAndWhiteness(visBr, visWh, targetTileMap, targetRectangle, visBands,
                                          SubProgressMonitor.create(pm, 2));
            computeBrightnessAndWhiteness(nirBr, nirWh, targetTileMap, targetRectangle, nirBands,
                                          SubProgressMonitor.create(pm, 2));
            // todo - atmospheric features
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        visBr = null;
        visWh = null;
        nirBr = null;
        nirWh = null;

        visBands = null;
        nirBands = null;
    }

    private void categorizeRadianceBands() {
        final List<Band> absorptionFreeBandList = new ArrayList<Band>();
        final List<Band> visBandList = new ArrayList<Band>();
        final List<Band> nirBandList = new ArrayList<Band>();

        categorization:
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("reflectance")) {
                final double wavelength = sourceBand.getSpectralWavelength();

                for (final AbsorptionBands absorptionBand : AbsorptionBands.values()) {
                    if (absorptionBand.contains(wavelength)) {
                        continue categorization;
                    }
                }
                absorptionFreeBandList.add(sourceBand);

                if (wavelength > VIS_MIN && wavelength < VIS_MAX) {
                    visBandList.add(sourceBand);
                } else {
                    nirBandList.add(sourceBand);
                }
            }
        }

        if (absorptionFreeBandList.isEmpty()) {
            throw new OperatorException("no absorption-free bands found");
        }
        if (visBandList.isEmpty()) {
            throw new OperatorException("no absorption-free visual bands found");
        }
        if (nirBandList.isEmpty()) {
            throw new OperatorException("no absorption-free NIR bands found");
        }

        absorptionFreeBands = absorptionFreeBandList.toArray(new Band[absorptionFreeBandList.size()]);
        visBands = visBandList.toArray(new Band[visBandList.size()]);
        nirBands = nirBandList.toArray(new Band[nirBandList.size()]);

        final BandComparator comparator = new BandComparator();
        Arrays.sort(absorptionFreeBands, comparator);
        Arrays.sort(visBands, comparator);
        Arrays.sort(nirBands, comparator);
    }

    void computeBrightnessAndWhiteness(Band targetBand1, Band targetBand2, Map<Band, Tile> targetTileMap,
                                       Rectangle targetRectangle, Band[] sourceBands, ProgressMonitor pm) {
        pm.beginTask("computing brightness and whiteness...", targetRectangle.height);
        try {
            final short[][] sourceSamples = new short[sourceBands.length][];

            final int[] sourceOffsets = new int[sourceBands.length];
            final int[] sourceStrides = new int[sourceBands.length];
            final int[] sourceIndexes = new int[sourceBands.length];

            final double[] wavelengths = new double[sourceBands.length];

            for (int i = 0; i < sourceBands.length; ++i) {
                final Tile sourceTile = getSourceTile(sourceBands[i], targetRectangle, pm);

                sourceSamples[i] = sourceTile.getDataBufferShort();
                sourceOffsets[i] = sourceTile.getScanlineOffset();
                sourceStrides[i] = sourceTile.getScanlineStride();

                wavelengths[i] = sourceBands[i].getSpectralWavelength();
            }

            final Tile targetTile1 = targetTileMap.get(targetBand1);
            final Tile targetTile2 = targetTileMap.get(targetBand2);

            final short[] targetSamples1 = targetTile1.getDataBufferShort();
            final short[] targetSamples2 = targetTile2.getDataBufferShort();

            int targetOffset1 = targetTile1.getScanlineOffset();
            int targetOffset2 = targetTile2.getScanlineOffset();

            final int targetStride1 = targetTile1.getScanlineStride();
            final int targetStride2 = targetTile2.getScanlineStride();

            for (int y = 0; y < targetRectangle.height; ++y) {
                checkForCancelation(pm);

                System.arraycopy(sourceOffsets, 0, sourceIndexes, 0, sourceBands.length);
                int targetIndex1 = targetOffset1;
                int targetIndex2 = targetOffset2;
                for (int x = 0; x < targetRectangle.width; ++x) {
                    final double b = brightness(sourceSamples, sourceIndexes, wavelengths);
                    final double w = whiteness(sourceSamples, sourceIndexes, wavelengths, b);

                    targetSamples1[targetIndex1] = (short) (b + 0.5);
                    targetSamples2[targetIndex2] = (short) (w + 0.5);

                    for (int i = 0; i < sourceBands.length; i++) {
                        ++sourceIndexes[i];
                    }
                    ++targetIndex1;
                    ++targetIndex2;
                }
                for (int i = 0; i < sourceBands.length; i++) {
                    sourceOffsets[i] += sourceStrides[i];
                }
                targetOffset1 += targetStride1;
                targetOffset2 += targetStride2;

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static double brightness(short[][] samples, int[] indexes, double[] wavelengths) {
        double sum = 0.0;

        double value1 = samples[0][indexes[0]];
        for (int i = 1; i < samples.length; ++i) {
            final double value2 = samples[i][indexes[i]];

            sum += 0.5 * (value2 + value1) * (wavelengths[i] - wavelengths[i - 1]);
            value1 = value2;
        }

        return sum / (wavelengths[wavelengths.length - 1] - wavelengths[0]);
    }

    private static double whiteness(short[][] samples, int[] indexes, double[] wavelengths, double brightness) {
        double sum = 0.0;

        double value1 = Math.abs(samples[0][indexes[0]] - brightness);
        for (int i = 1; i < samples.length; ++i) {
            final double value2 = Math.abs(samples[i][indexes[i]] - brightness);

            sum += 0.5 * (value2 + value1) * (wavelengths[i] - wavelengths[i - 1]);
            value1 = value2;
        }

        return sum / (wavelengths[wavelengths.length - 1] - wavelengths[0]);
    }

    static void assertValidity(Product product) {
        if (!product.getProductType().matches("CHRIS_M[1-5]A?_REFL")) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not of appropriate type", product.getName()));
        }
    }

    // todo - move
    static double[][] readTransmittanceTable() throws OperatorException {
        final ImageInputStream iis = getResourceAsImageInputStream("toa-nir-transmittance.img");

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
     * @return the image input stream.
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
     */
    private static ImageInputStream getResourceAsImageInputStream(String name) throws OperatorException {
        final InputStream is = ExtractFeaturesOp.class.getResourceAsStream(name);

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


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractFeaturesOp.class);
        }
    }
}
