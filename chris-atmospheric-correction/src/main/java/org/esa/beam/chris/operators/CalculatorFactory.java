package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.internal.RtcTable;

/**
 * Creates a {@link Calculator}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CalculatorFactory {

    private final RtcTable table;
    private final double[] corrections;
    private final double toaScaling;

    public CalculatorFactory(RtcTable table, double[] corrections, double toaScaling) {
        this.table = table;
        this.corrections = corrections;
        this.toaScaling = toaScaling;
    }

    public Calculator createCalculator(Resampler resampler) {
        final double[] lpw = resampler.resample(table.getLpw());
        final double[] egl = resampler.resample(table.getEgl());
        final double[] sab = resampler.resample(table.getSab());
        final double[] rat = resampler.resample(table.getRat());

        for (int i = 0; i < lpw.length; i++) {
            lpw[i] -= corrections[i];
        }

        return new Calculator(lpw, egl, sab, rat, toaScaling);
    }
}
