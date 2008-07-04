package org.esa.beam.chris.operators;

import org.esa.beam.util.math.IntervalPartition;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CwvRtcTableFactory {

    private IntervalPartition dimension;
    private final RtcTable[] rtcTables;

    public CwvRtcTableFactory(RtcTableFactory factory, double vza, double sza, double ada, double alt, double aot) {
        dimension = factory.getDimension(RtcTableFactory.CWV);
        rtcTables = new RtcTable[dimension.getCardinal()];

        for (int i = 0; i < dimension.getCardinal(); ++i) {
            rtcTables[i] = factory.createRtcTable(vza, sza, ada, alt, aot, dimension.get(i));
        }
    }

    public RtcTable createRtcTable(double cwv) {
        final FracIndex fracIndex = computeFracIndexLog(dimension, cwv);

        return getRtcTable(fracIndex.i, fracIndex.f);
    }

    private RtcTable getRtcTable(final int i, final double f) {
        final double[] wavelengths = rtcTables[0].getWavelengths();

        final double[] lpw = new double[wavelengths.length];
        final double[] egl = new double[wavelengths.length];
        final double[] sab = new double[wavelengths.length];

        for (int k = 0; k < wavelengths.length; ++k) {
            lpw[k] = (1.0 - f) * rtcTables[i].getLpw(k) + f * rtcTables[i + 1].getLpw(k);
            egl[k] = (1.0 - f) * rtcTables[i].getEgl(k) + f * rtcTables[i + 1].getEgl(k);
            sab[k] = (1.0 - f) * rtcTables[i].getSab(k) + f * rtcTables[i + 1].getSab(k);
        }

        return new RtcTable(wavelengths, lpw, egl, sab);
    }

    private static FracIndex computeFracIndexLog(final IntervalPartition partition, final double coordinate) {
        int lo = 0;
        int hi = partition.getCardinal() - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < partition.get(m)) {
                hi = m;
            } else {
                lo = m;
            }
        }

        return new FracIndex(lo, Math.log(coordinate / partition.get(lo)) / Math.log(partition.get(hi) / partition.get(lo)));
    }

    private static class FracIndex {
        /**
         * The integral component.
         */
        public int i;

        /**
         * The fractional component.
         */
        public double f;

        public FracIndex(int i, double f) {
            this.i = i;

            // truncate the fractional component if less than zero or greater than unity
            if (f < 0.0) {
                f = 0.0;
            } else if (f > 1.0) {
                f = 1.0;
            }

            this.f = f;
        }
    }
}
