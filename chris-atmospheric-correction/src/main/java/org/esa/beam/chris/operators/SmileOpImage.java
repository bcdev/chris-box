package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.internal.Min;
import org.esa.beam.chris.operators.internal.Pow;
import org.esa.beam.chris.operators.internal.UnivariateFunction;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;

/**
 * Image with column-wise wavelengths shifts due to the smile effect.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class SmileOpImage extends OpImage {
    private static final double O2_LOWER_BOUND = 749.0;
    private static final double O2_UPPER_BOUND = 779.0;

    private final double[] nominalWavelengths;
    private final double[] nominalBandwidths;
    private final BoaReflectanceCalculator boaReflectanceCalculator;

    private final int lowerO2;
    private final int upperO2;

    private final LocalRegressionSmoother smoother;

    public static OpImage createImage(Band[] radianceBands, Band[] maskBands, Band cloudProductBand,
                                      double cloudProductThreshold, BoaReflectanceCalculator boaReflectanceCalculator) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

        final RenderedImage hyperMaskImage = HyperMaskOpImage.createImage(maskBands);
        final RenderedImage cloudMaskImage = CloudMaskOpImage.createImage(cloudProductBand, cloudProductThreshold);
        sourceImageVector.add(hyperMaskImage);
        sourceImageVector.add(cloudMaskImage);

        for (final Band band : radianceBands) {
            RenderedImage image = band.getImage();
            if (image == null) {
                image = new RasterDataNodeOpImage(band);
                band.setImage(image);
            }
            sourceImageVector.add(image);
        }

        int w = hyperMaskImage.getWidth();
        int h = 1;

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_DOUBLE, w, h, 1, w,
                                                                    new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        final double[] sourceWavelengths = OpUtils.getCentralWavelenghts(radianceBands);
        final double[] sourceBandwidths = OpUtils.getBandwidths(radianceBands);

        return new SmileOpImage(imageLayout, sourceImageVector, sourceWavelengths, sourceBandwidths,
                                boaReflectanceCalculator);
    }

    private SmileOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector, double[] nominalWavelengths,
                         double[] nominalBandwidths, BoaReflectanceCalculator boaReflectanceCalculator) {
        super(sourceImageVector, imageLayout, null, true);

        this.nominalWavelengths = nominalWavelengths;
        this.nominalBandwidths = nominalBandwidths;
        this.boaReflectanceCalculator = boaReflectanceCalculator;

        int lowerO2 = -1;
        int upperO2 = -1;
        for (int i = 0; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] >= O2_LOWER_BOUND) {
                lowerO2 = i;
                break;
            }
        }
        for (int i = lowerO2; i < nominalWavelengths.length; ++i) {
            if (nominalWavelengths[i] <= O2_UPPER_BOUND) {
                upperO2 = i;
            } else {
                break;
            }
        }
        this.lowerO2 = lowerO2;
        this.upperO2 = upperO2;

        smoother = new LocalRegressionSmoother(new BoxcarWeightCalculator(), 0, 10);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final double[] resampledLpw = new double[nominalWavelengths.length];
        final double[] resampledEgl = new double[nominalWavelengths.length];
        final double[] resampledSab = new double[nominalWavelengths.length];

        final double[][] meanToaSpectra = new double[rectangle.width][nominalWavelengths.length];
        final double[][] trueBoaSpectra = new double[rectangle.width][nominalWavelengths.length];

        final Min.Bracket bracket = new Min.Bracket();

        computeMeanToaSpectra(sources, meanToaSpectra, mapDestRect(rectangle, 0));
        computeTrueBoaSpectra(meanToaSpectra, trueBoaSpectra);

        final PixelAccessor targetAccessor;
        final UnpackedImageData targetData;
        final double[] targetPixels;

        targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_DOUBLE, true);
        targetPixels = targetData.getDoubleData(0);

        int targetColumnOffset = targetData.bandOffsets[0];

        for (int x = 0; x < rectangle.width; ++x) {
            final double[] meanToaSpectrum = meanToaSpectra[x];
            final double[] trueBoaSpectrum = trueBoaSpectra[x];
            final double[] meanBoaSpectrum = new double[nominalWavelengths.length];

            final UnivariateFunction function = new UnivariateFunction() {
                @Override
                public double value(double shift) {
                    final Resampler resampler = boaReflectanceCalculator.createResampler(nominalWavelengths,
                                                                                         nominalBandwidths, shift);

                    boaReflectanceCalculator.calculateBoaReflectances(resampler, meanToaSpectrum, meanBoaSpectrum);
                    double sum = 0.0;
                    for (int i = lowerO2; i < upperO2 + 1; ++i) {
                        sum += Pow.pow2(trueBoaSpectrum[i] - meanBoaSpectrum[i]);
                    }

                    return sum;
                }
            };

            Min.brack(function, -6.0, 6.0, bracket);
            Min.brent(function, bracket, 1.0E-5, 1.0E-5, 1000);
            targetPixels[targetColumnOffset] = bracket.minimumX;

            targetColumnOffset += targetData.pixelStride;
        }

        targetAccessor.setPixels(targetData);
    }

    @Override
    public Rectangle mapSourceRect(Rectangle rectangle, int i) {
        return new Rectangle(rectangle.x, 0, rectangle.width, 1);
    }

    @Override
    public Rectangle mapDestRect(Rectangle rectangle, int i) {
        return new Rectangle(rectangle.x, 0, rectangle.width, getSourceImage(i).getHeight());
    }

    private void computeMeanToaSpectra(Raster[] sources, double[][] meanToaSpectra, Rectangle rectangle) {
        final PixelAccessor hyperMaskAccessor;
        final PixelAccessor cloudMaskAccessor;

        final UnpackedImageData hyperMaskData;
        final UnpackedImageData cloudMaskData;

        final byte[] hyperMaskPixels;
        final byte[] cloudMaskPixels;

        hyperMaskAccessor = new PixelAccessor(getSourceImage(0));
        cloudMaskAccessor = new PixelAccessor(getSourceImage(1));

        hyperMaskData = hyperMaskAccessor.getPixels(sources[0], rectangle, DataBuffer.TYPE_BYTE, false);
        cloudMaskData = cloudMaskAccessor.getPixels(sources[1], rectangle, DataBuffer.TYPE_BYTE, false);

        hyperMaskPixels = hyperMaskData.getByteData(0);
        cloudMaskPixels = cloudMaskData.getByteData(0);

        for (int i = 2; i < sources.length; ++i) {
            final PixelAccessor radianceAccessor;
            final UnpackedImageData radianceData;
            final int[] radiancePixels;

            radianceAccessor = new PixelAccessor(getSourceImage(i));
            radianceData = radianceAccessor.getPixels(sources[i], rectangle, DataBuffer.TYPE_INT, false);
            radiancePixels = radianceData.getIntData(0);

            int sourceColumnOffset = radianceData.bandOffsets[0];

            for (int x = 0; x < rectangle.width; ++x) {
                int sourcePixelOffset = sourceColumnOffset;
                int count = 0;

                for (int y = 0; y < rectangle.height; ++y) {
                    if (hyperMaskPixels[sourcePixelOffset] == 0 && cloudMaskPixels[sourcePixelOffset] == 0) {
                        final int radiance = radiancePixels[sourcePixelOffset];

                        if (radiance > 0) {
                            meanToaSpectra[x][i - 2] += radiance;
                            ++count;
                        }
                    }
                    sourcePixelOffset += radianceData.lineStride;
                }
                if (count > 0) {
                    meanToaSpectra[x][i - 2] /= count;
                }

                sourceColumnOffset += radianceData.pixelStride;
            }
        }
    }

    private void computeTrueBoaSpectra(double[][] meanToaSpectra, double[][] trueBoaSpectra) {
        final Resampler resampler = boaReflectanceCalculator.createResampler(nominalWavelengths, nominalBandwidths);

        for (int x = 0; x < trueBoaSpectra.length; ++x) {
            final double[] meanBoaSpectrum = new double[nominalWavelengths.length];
            boaReflectanceCalculator.calculateBoaReflectances(resampler, meanToaSpectra[x], meanBoaSpectrum);

            smoother.smooth(meanBoaSpectrum, trueBoaSpectra[x]);

            // linear interpolation between lower and upper O2 absorption bands
            // todo - ask Luis Guanter if this is necessary due to non-robust boxcar smoothing
            final double w = nominalWavelengths[upperO2] - nominalWavelengths[lowerO2];
            for (int i = lowerO2 + 1; i < upperO2; ++i) {
                final double t = (nominalWavelengths[i] - nominalWavelengths[lowerO2]) / w;

                trueBoaSpectra[x][i] = t * trueBoaSpectra[x][upperO2] + (1.0 - t) * trueBoaSpectra[x][lowerO2];
            }
        }
    }
}
