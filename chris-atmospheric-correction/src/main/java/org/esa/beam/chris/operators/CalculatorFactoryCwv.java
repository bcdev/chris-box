package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.internal.ModtranLookupTable;
import org.esa.beam.chris.operators.internal.RtcTable;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CalculatorFactoryCwv {

    private final double[] cwv;

    private final double[][] lpw;
    private final double[][] egl;
    private final double[][] sab;
    private final double[][] rat;

    private final double toaScaling;

    public CalculatorFactoryCwv(ModtranLookupTable modtranLookupTable, Resampler resampler, double vza, double sza,
                                double ada, double alt, double aot, double toaScaling) {
        cwv = modtranLookupTable.getDimension(ModtranLookupTable.CWV).getSequence();

        lpw = new double[cwv.length][];
        egl = new double[cwv.length][];
        sab = new double[cwv.length][];
        rat = new double[cwv.length][];

        for (int i = 0; i < cwv.length; ++i) {
            final RtcTable table = modtranLookupTable.getRtcTable(vza, sza, ada, alt, aot, cwv[i]);

            lpw[i] = resampler.resample(table.getLpw());
            egl[i] = resampler.resample(table.getEgl());
            sab[i] = resampler.resample(table.getSab());
            rat[i] = resampler.resample(table.getRat());
        }

        this.toaScaling = toaScaling;
    }

    public Calculator createCalculator(double cwv) {
        final FI fracIndex = toFracIndex(cwv);

        return createCalculator(fracIndex.i, fracIndex.f);
    }

    public final double getCwvMin() {
        return cwv[0];
    }

    public final double getCwvMax() {
        return cwv[cwv.length - 1];
    }

    private Calculator createCalculator(final int i, final double f) {
        final int wavelengthCount = lpw[i].length;

        final double[] interpolatedLpw = new double[wavelengthCount];
        final double[] interpolatedEgl = new double[wavelengthCount];
        final double[] interpolatedSab = new double[wavelengthCount];
        final double[] interpolatedRat = new double[wavelengthCount];

        for (int k = 0; k < wavelengthCount; ++k) {
            interpolatedLpw[k] = (1.0 - f) * lpw[i][k] + f * lpw[i + 1][k];
            interpolatedEgl[k] = (1.0 - f) * egl[i][k] + f * egl[i + 1][k];
            interpolatedSab[k] = (1.0 - f) * sab[i][k] + f * sab[i + 1][k];
            interpolatedRat[k] = (1.0 - f) * rat[i][k] + f * rat[i + 1][k];
        }

        return new Calculator(interpolatedLpw, interpolatedEgl, interpolatedSab, interpolatedRat, toaScaling);
    }

    private FI toFracIndex(final double coordinate) {
        int lo = 0;
        int hi = cwv.length - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < cwv[m]) {
                hi = m;
            } else {
                lo = m;
            }
        }

        return new FI(lo, Math.log(coordinate / cwv[lo]) / Math.log(cwv[hi] / cwv[lo]));
    }

    /**
     * Index with integral and fractional components.
     */
    private static class FI {
        /**
         * The integral component.
         */
        public int i;
        /**
         * The fractional component.
         */
        public double f;

        /**
         * Constructs a new instance of this class.
         *
         * @param i the integral component.
         * @param f the fractional component.  Note that the fractional component of the
         *          created instance is set to {@code 0.0} if {@code f < 0.0} and set to
         *          {@code 1.0} if {@code f > 1.0}.
         */
        public FI(int i, double f) {
            this.i = i;

            if (f < 0.0) {
                f = 0.0;
            } else if (f > 1.0) {
                f = 1.0;
            }

            this.f = f;
        }
    }
}
