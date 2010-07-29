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

package org.esa.beam.chris.operators.internal;

/**
 * Table storing radiative transfer calculations.
 *
 * @author Ralf Quast
 * @version $Revision: 2703 $ $Date: 2008-07-15 11:42:48 +0200 (Di, 15 Jul 2008) $
 * @since BEAM 4.2
 */
public class RtcTable {
    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double[] rat;

    public RtcTable(double[] lpw, double[] egl, double[] sab, double[] rat) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.rat = rat;
    }

    public final double[] getLpw() {
        return lpw;
    }

    public final double[] getEgl() {
        return egl;
    }

    public final double[] getSab() {
        return sab;
    }

    public double[] getRat() {
        return rat;
    }

    public final double getLpw(int i) {
        return lpw[i];
    }

    public final double getEgl(int i) {
        return egl[i];
    }

    public final double getSab(int i) {
        return sab[i];
    }

    public final double getRat(int i) {
        return rat[i];
    }
}
