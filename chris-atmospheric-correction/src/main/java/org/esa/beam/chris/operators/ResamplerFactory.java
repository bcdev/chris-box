package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ResamplerFactory {

    private final double[] sourceWavelengths;
    private final double[] targetWavelengths;
    private final double[] targetBandwidths;

    public ResamplerFactory(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths) {
        this.sourceWavelengths = sourceWavelengths;
        this.targetWavelengths = targetWavelengths;
        this.targetBandwidths = targetBandwidths;
    }

    public Resampler createResampler(double wavlengthShift) {
        return new Resampler(sourceWavelengths, targetWavelengths, targetBandwidths, wavlengthShift);
    }
}
