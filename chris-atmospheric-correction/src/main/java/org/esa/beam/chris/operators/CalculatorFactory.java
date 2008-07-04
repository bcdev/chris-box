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
        final Resampler resampler = new Resampler(rtcTable.getWavelengths(), wavelengths, bandwidths, shift);

        final double[] lpw = new double[wavelengths.length];
        final double[] egl = new double[wavelengths.length];
        final double[] sab = new double[wavelengths.length];

        resampler.resample(rtcTable.getLpw(), lpw);
        resampler.resample(rtcTable.getEgl(), egl);
        resampler.resample(rtcTable.getSab(), sab);

        return new Calculator(lpw, egl, sab, toaScaling);
    }

    public Calculator createCalculator(double[] wavelengths, double[] bandwidths, double[] lpwCor) {
        final Resampler resampler = new Resampler(rtcTable.getWavelengths(), wavelengths, bandwidths);

        final double[] lpw = new double[wavelengths.length];
        final double[] egl = new double[wavelengths.length];
        final double[] sab = new double[wavelengths.length];

        resampler.resample(rtcTable.getLpw(), lpw);
        resampler.resample(rtcTable.getEgl(), egl);
        resampler.resample(rtcTable.getSab(), sab);

        for (int i = 0; i < lpw.length; i++) {
            lpw[i] -= lpwCor[i];
        }

        return new Calculator(lpw, egl, sab, toaScaling);
    }
}
