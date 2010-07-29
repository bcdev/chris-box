/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
