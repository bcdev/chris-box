package org.esa.beam.chris.operators;

/**
 * Creates a {@link Resampler}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ResamplerFactory {

    private final double[] sourceWavelengths;
    private final double[] targetWavelengths;
    private final double[] targetBandwidths;

    ResamplerFactory(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths) {
        this.sourceWavelengths = sourceWavelengths;
        this.targetWavelengths = targetWavelengths;
        this.targetBandwidths = targetBandwidths;
    }

    Resampler createResampler(double wavlengthShift) {
        return new Resampler(sourceWavelengths, targetWavelengths, targetBandwidths, wavlengthShift);
    }
}
