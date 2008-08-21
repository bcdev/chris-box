/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.internal.ModtranLookupTable;
import org.esa.beam.chris.operators.internal.RtcTable;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class RtcTableFactoryAot {
    private final double[] aot;
    private final double[][] lpw;

    public RtcTableFactoryAot(ModtranLookupTable modtranLookupTable, Resampler resampler, double vza,
                              double sza, double ada, double alt, double cwv) {
        aot = modtranLookupTable.getDimension(ModtranLookupTable.AOT);

        lpw = new double[aot.length][];

        for (int i = 0; i < aot.length; ++i) {
            final RtcTable table = modtranLookupTable.getRtcTable(vza, sza, ada, alt, aot[i], cwv);

            lpw[i] = resampler.resample(table.getLpw());
        }
    }

    public RtcTable createRtcTable(double aot) {
        final FI fracIndex = toFracIndex(aot);

        return createRtcTable(fracIndex.i, fracIndex.f);
    }

    public final double getMinAot() {
        return aot[0];
    }

    public final double getMaxAot() {
        return aot[aot.length - 1];
    }

    private RtcTable createRtcTable(final int i, final double f) {
        final int wavelengthCount = lpw[i].length;

        final double[] interpolatedLpw = new double[wavelengthCount];

        for (int k = 0; k < wavelengthCount; ++k) {
            interpolatedLpw[k] = (1.0 - f) * lpw[i][k] + f * lpw[i + 1][k];
        }

        return new RtcTable(interpolatedLpw, null, null, null);
    }

    private FI toFracIndex(final double coordinate) {
        int lo = 0;
        int hi = aot.length - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < aot[m]) {
                hi = m;
            } else {
                lo = m;
            }
        }

        return new FI(lo, (coordinate - aot[lo]) / (aot[hi] - aot[lo]));
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
