package org.esa.beam.chris.operators;

import org.esa.beam.chris.math.LocalRegressionSmoother;
import org.esa.beam.chris.math.LowessRegressionWeightCalculator;
import org.esa.beam.chris.operators.internal.Min;
import org.esa.beam.chris.operators.internal.UnivariateFunction;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.*;
import java.util.Vector;

/**
 * Calculates column-wise wavelengths shifts due to the CHRIS smile effect.
 * <p/>
 * Based on Guanter et al. (2006, Appl. Opt. 45, 2360).
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class SmileOpImage extends OpImage {
    private static final int MAX_ITER = 1000;

    private static final double O2_LOWER_BOUND = 749.0;
    private static final double O2_UPPER_BOUND = 779.0;

    private final CalculatorFactory calculatorFactory;
    private final int lowerO2;
    private final int upperO2;

    private final LocalRegressionSmoother smoother;

    /**
     * Creates a new image from the radiance bands of a CHRIS product and the
     * corresponding mask images.
     *
     * @param radianceBands     the radiance bands.
     * @param hyperMaskImage    the hyper-spectral quality mask image.
     * @param cloudMaskImage    the cloud mask image.
     * @param calculatorFactory the factory for creating the strategy for calculating BOA reflectances
     *                          from TOA radiances.
     *
     * @return the column-wise wavelength shifts.
     */
    public static OpImage createImage(Band[] radianceBands, RenderedImage hyperMaskImage, RenderedImage cloudMaskImage,
                                      CalculatorFactory calculatorFactory) {
        final Vector<RenderedImage> sourceImageVector = new Vector<RenderedImage>();

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

        int lowerO2 = -1;
        int upperO2 = -1;
        for (int i = 0; i < radianceBands.length; ++i) {
            if (radianceBands[i].getSpectralWavelength() >= O2_LOWER_BOUND) {
                lowerO2 = i;
                break;
            }
        }
        for (int i = lowerO2; i < radianceBands.length; ++i) {
            if (radianceBands[i].getSpectralWavelength() <= O2_UPPER_BOUND) {
                upperO2 = i;
            } else {
                break;
            }
        }

        return new SmileOpImage(imageLayout, sourceImageVector, lowerO2, upperO2, calculatorFactory);
    }

    private SmileOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector, int lowerO2, int upperO2,
                         CalculatorFactory calculatorFactory) {
        super(sourceImageVector, imageLayout, null, true);

        this.lowerO2 = lowerO2;
        this.upperO2 = upperO2;
        this.calculatorFactory = calculatorFactory;

        smoother = new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 0, 9, 1);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final double[][] meanToaSpectra = new double[rectangle.width][sources.length - 2];
        final double[][] trueBoaSpectra = new double[rectangle.width][sources.length - 2];

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
            final double[] meanBoaSpectrum = new double[sources.length - 2];

            final UnivariateFunction function = new UnivariateFunction() {
                @Override
                public double value(double shift) {
                    // todo - ask Luis Guanter why not just the O2 bands are used here - would improve speed (rq)
                    final Calculator calculator = calculatorFactory.createCalculator(shift);
                    calculator.calculateBoaReflectances(meanToaSpectrum, meanBoaSpectrum, lowerO2, upperO2 + 1);

                    double sum = 0.0;
                    for (int i = lowerO2; i < upperO2 + 1; ++i) {
                        final double d = trueBoaSpectrum[i] - meanBoaSpectrum[i];
                        sum += d * d;
                    }

                    return sum;
                }
            };

            Min.brack(function, -6.0, 6.0, bracket);
            Min.brent(function, bracket, 1.0E-5, 1.0E-5, MAX_ITER);
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
        final Calculator calculator = calculatorFactory.createCalculator();

        for (int x = 0; x < meanToaSpectra.length; ++x) {
            final double[] meanToaSpectrum = meanToaSpectra[x];
            final double[] meanBoaSpectrum = new double[meanToaSpectrum.length];

            calculator.calculateBoaReflectances(meanToaSpectrum, meanBoaSpectrum);
            smoother.smooth(meanBoaSpectrum, trueBoaSpectra[x]);

//            linear interpolation between lower and upper O2 absorption bands
//            todo - ask Luis Guanter if this was necessary due to non-robust boxcar smoothing in the prototype (rq)
//            final double w = nominalWavelengths[upperO2] - nominalWavelengths[lowerO2];
//            for (int i = lowerO2 + 1; i < upperO2; ++i) {
//                final double t = (nominalWavelengths[i] - nominalWavelengths[lowerO2]) / w;
//
//                trueBoaSpectra[x][i] = t * trueBoaSpectra[x][upperO2] + (1.0 - t) * trueBoaSpectra[x][lowerO2];
//            }
        }
    }
}
