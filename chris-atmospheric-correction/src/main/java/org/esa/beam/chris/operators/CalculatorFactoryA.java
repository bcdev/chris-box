package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CalculatorFactoryA {

    private final RtcTable rtcTable;
    private final double[] wavelengths;
    private final double[] bandwidths;
    private final double toaScaling;

    public CalculatorFactoryA(RtcTable rtcTable, double[] wavelengths, double[] bandwidths, double toaScaling) {
        this.rtcTable = rtcTable;
        this.wavelengths = wavelengths;
        this.bandwidths = bandwidths;
        this.toaScaling = toaScaling;
    }

    public Calculator createCalculator() {
        return createCalculator(0.0);
    }

    public Calculator createCalculator(double shift) {
        return createCalculator(new Resampler(rtcTable.getWavelengths(), wavelengths, bandwidths, shift));
    }

    private Calculator createCalculator(Resampler resampler) {
        final double[] lpw = resampler.resample(rtcTable.getLpw());
        final double[] egl = resampler.resample(rtcTable.getEgl());
        final double[] sab = resampler.resample(rtcTable.getSab());

        return new Calculator(lpw, egl, sab, toaScaling);
    }
}
