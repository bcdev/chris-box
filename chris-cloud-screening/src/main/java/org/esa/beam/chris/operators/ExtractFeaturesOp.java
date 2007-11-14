package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.chris.operators.internal.BandComparator;
import org.esa.beam.chris.operators.internal.BandFilter;
import org.esa.beam.chris.operators.internal.InclusiveBandFilter;
import org.esa.beam.chris.operators.internal.InclusiveMultiBandFilter;
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
import static java.lang.Math.abs;
import static java.lang.Math.pow;
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

    private static final double INVERSE_SCALING_FACTOR = 10000.0;

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private String targetProductName;

    private transient Band surBr;
    private transient Band surWh;
    private transient Band visBr;
    private transient Band visWh;
    private transient Band nirBr;
    private transient Band nirWh;
    private transient Band o2;
    private transient Band wv;

    private transient Band[] surBands;
    private transient Band[] visBands;
    private transient Band[] nirBands;

    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);
        categorizeReflectanceBands();

        final String type = sourceProduct.getProductType().replace("_REFL", "_FEAT");
        targetProduct = new Product(targetProductName, type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        surBr = targetProduct.addBand("brightness", ProductData.TYPE_INT16);
        surBr.setDescription("Brightness for visual and NIR bands");
        surBr.setUnit("dl");
        surBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visBr = targetProduct.addBand("brightness_vis", ProductData.TYPE_INT16);
        visBr.setDescription("Brightness for visual bands");
        visBr.setUnit("dl");
        visBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirBr = targetProduct.addBand("brightness_nir", ProductData.TYPE_INT16);
        nirBr.setDescription("Brightness for NIR bands");
        nirBr.setUnit("dl");
        nirBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        surWh = targetProduct.addBand("whiteness", ProductData.TYPE_INT16);
        surWh.setDescription("Whiteness for visual and NIR bands");
        surWh.setUnit("dl");
        surWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visWh = targetProduct.addBand("whiteness_vis", ProductData.TYPE_INT16);
        visWh.setDescription("Whiteness for visual bands");
        visWh.setUnit("dl");
        visBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirWh = targetProduct.addBand("whiteness_nir", ProductData.TYPE_INT16);
        nirWh.setDescription("Whiteness for NIR bands");
        nirWh.setUnit("dl");
        nirWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        o2 = targetProduct.addBand("o2", ProductData.TYPE_INT16);
        o2.setDescription("Oxygen-A absorption");
        o2.setUnit("dl");
        o2.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        wv = targetProduct.addBand("wv", ProductData.TYPE_INT16);
        wv.setDescription("Water vapour absorption");
        wv.setUnit("dl");
        wv.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        pm.beginTask("computing bands...", 6);
        try {
            computeBrightnessAndWhiteness(surBr, surWh, targetTileMap, targetRectangle, surBands,
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
        surBr = null;
        surWh = null;
        visBr = null;
        visWh = null;
        nirBr = null;
        nirWh = null;
        o2 = null;
        wv = null;

        surBands = null;
        visBands = null;
        nirBands = null;
    }

    private void categorizeReflectanceBands() {
        final List<Band> surBandList = new ArrayList<Band>();
        final List<Band> visBandList = new ArrayList<Band>();
        final List<Band> nirBandList = new ArrayList<Band>();

        final BandFilter visBandFilter = new InclusiveBandFilter(400.0, 700.0);
        final BandFilter absBandFilter = new InclusiveMultiBandFilter(new double[][]{
                {400.0, 440.0},
                {590.0, 600.0},
                {630.0, 636.0},
                {648.0, 658.0},
                {686.0, 709.0},
                {792.0, 799.0},
                {756.0, 775.0},
                {808.0, 840.0},
                {885.0, 985.0},
                {985.0, 1010.0}});

        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("reflectance")) {
                if (absBandFilter.accept(sourceBand)) {
                    continue;
                }
                surBandList.add(sourceBand);
                if (visBandFilter.accept(sourceBand)) {
                    visBandList.add(sourceBand);
                } else {
                    nirBandList.add(sourceBand);
                }
            }
        }

        if (surBandList.isEmpty()) {
            throw new OperatorException("no absorption-free bands found");
        }
        if (visBandList.isEmpty()) {
            throw new OperatorException("no absorption-free visual bands found");
        }
        if (nirBandList.isEmpty()) {
            throw new OperatorException("no absorption-free NIR bands found");
        }

        surBands = surBandList.toArray(new Band[surBandList.size()]);
        visBands = visBandList.toArray(new Band[visBandList.size()]);
        nirBands = nirBandList.toArray(new Band[nirBandList.size()]);

        final BandComparator comparator = new BandComparator();
        Arrays.sort(surBands, comparator);
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

    private void computeOpticalPath(Band targetBand, Map<Band, Tile> targetTileMap, Rectangle targetRectangle) {
/*
 %Atmospheric absorptions
 % m=1/mu=1/cos(illum)+1/cos(obs): Optical mass
 if exist('ObservationZenithAngle'), mu=1/(1/cos(SolarZenithAngle/180*pi)+1/cos(ObservationZenithAngle/180*pi));
 else, mu=1/(1/cos(SolarZenithAngle/180*pi)); end
 %O2 atmospheric absorption
 W_out_inf=[738 755]; W_in=[755 770]; W_out_sup=[770 788];  W_max=[760.625]; %O2
 OP_O2=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);
 %H2O atmospheric absorption
 W_out_inf=[865 890]; W_in=[895 960]; W_out_sup=[985 1100]; W_max=[944.376]; %H2O
 OP_H2O=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);
 */

/*
function OP=optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max)
% Estimation of the optical path from an atmospheric absorption band.
% Note: contribution of illumination and observation geometries is not normalized (OP=mu*OP)
% Inputs:
%   - X: CHRIS image values
%   - WlMid,BWidth: wavelength and bandwidth of CHRIS channels
%   - W_out_inf,W_in,W_out_sup,W_max: spectral channels located at the absorption band
%                                     (outside and inside the absorption)

b_out_inf = find((WlMid-BWidth/2)>=W_out_inf(1) & (WlMid+BWidth/2)<=W_out_inf(2));
b_in      = find((WlMid-BWidth/2)>W_in(1) & (WlMid+BWidth/2)<W_in(2));
b_out_sup = find((WlMid-BWidth/2)>=W_out_sup(1) & (WlMid+BWidth/2)<=W_out_sup(2));
if ~isempty(b_in)
  b_in=find( abs(WlMid-W_max) == min(abs(WlMid(b_in)-W_max)) );

  %Effective atmospheric vertical transmittance, exp(-tau) estimated from a high resolution curve
  [ATM_w_hr,ATM_trans_hr]=textread('TOA_trans_NIR_hi.txt','%f %f','headerlines',1);
  ATM_trans=conv_spectral_channels(ATM_w_hr,ATM_trans_hr,WlMid(b_in),BWidth(b_in));

  %Interpolated spectrum at the absorption band is estimated from nearby channels
  if ~isempty(b_out_inf) & ~isempty(b_out_sup)
   L_out_inf=mean(X(:,:,b_out_inf),3); w_out_inf=mean(WlMid(b_out_inf));
   L_out_sup=mean(X(:,:,b_out_sup),3); w_out_sup=mean(WlMid(b_out_sup));
   L0=L_out_inf+((WlMid(b_in)-w_out_inf)/(w_out_sup-w_out_inf))*(L_out_sup-L_out_inf);
  elseif ~isempty(b_out_inf)
    L0=mean(X(:,:,b_out_inf),3);
  elseif ~isempty(b_out_sup)
    L0=mean(X(:,:,b_out_sup),3);
  end
  %Estimation of the optical path from an atmospheric absorption band.
  %note: contribution of illumination and observation geometries is not normalized
  OP=1/log(ATM_trans)*log(X(:,:,b_in)./L0);
else
  OP=zeros(size(X(:,:,1)));
end
*/
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
        final ImageInputStream iis = getResourceAsImageInputStream("nir-transmittance.img");

        try {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("could not read NIR transmittance table", e);
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
