package org.esa.beam.chris.operators;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CalculatorFactory {

    private final RtcTable table;
    private final double toaScaling;

    public CalculatorFactory(RtcTable table, double toaScaling) {
        this.table = table;
        this.toaScaling = toaScaling;
    }

    public Calculator createCalculator(Resampler resampler) {
        final double[] lpw = resampler.resample(table.getLpw());
        final double[] egl = resampler.resample(table.getEgl());
        final double[] sab = resampler.resample(table.getSab());

        return new Calculator(lpw, egl, sab, toaScaling);
    }
}
