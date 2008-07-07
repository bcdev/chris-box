package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CalculatorFactory {

    private final RtcTable rtcTable;
    private final double toaScaling;

    public CalculatorFactory(RtcTable rtcTable, double toaScaling) {
        this.rtcTable = rtcTable;
        this.toaScaling = toaScaling;
    }

    public Calculator createCalculator(double[] wavelengths, double[] bandwidths) {
        return createCalculator(wavelengths, bandwidths, 0.0);
    }

    public Calculator createCalculator(double[] wavelengths, double[] bandwidths, double shift) {
        return createCalculator(new Resampler(rtcTable.getWavelengths(), wavelengths, bandwidths, shift));
    }

    public Calculator createCalculator(Resampler resampler) {
        final double[] lpw = resampler.resample(rtcTable.getLpw());
        final double[] egl = resampler.resample(rtcTable.getEgl());
        final double[] sab = resampler.resample(rtcTable.getSab());

        return new Calculator(lpw, egl, sab, toaScaling);
    }
}
