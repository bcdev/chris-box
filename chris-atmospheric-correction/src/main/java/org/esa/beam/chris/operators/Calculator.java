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

/**
 * Calculates BOA reflectances from TOA radiances.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Calculator {

    private final double[] lpw;
    private final double[] egl;
    private final double[] sab;
    private final double[] rat;

    private final double toaScaling;

    Calculator(double[] lpw, double[] egl, double[] sab, double[] rat, double toaScaling) {
        this.lpw = lpw;
        this.egl = egl;
        this.sab = sab;
        this.rat = rat;
        this.toaScaling = toaScaling;
    }

    public void calculateBoaReflectances(double[] toa, double[] rho) {
        calculateBoaReflectances(toa, rho, 0, toa.length);
    }

    public void calculateBoaReflectances(double[] toa, double[] rho, int from, int to) {
        for (int i = from; i < to; ++i) {
            rho[i] = getBoaReflectance(i, toa[i]);
        }
    }

    public double getBoaReflectance(int i, double toa) {
        final double a = Math.PI * (toa * toaScaling - lpw[i]) / egl[i];

        return a / (1.0 + a * sab[i]);
    }

    public void calculateToaRadiances(double[] rho, double[] toa) {
        calculateToaRadiances(rho, toa, 0, rho.length);
    }

    public void calculateToaRadiances(double[] rho, double[] toa, int from, int to) {
        for (int i = from; i < to; ++i) {
            toa[i] = getToaRadiance(i, rho[i]);
        }
    }

    public double getToaRadiance(int i, double rho) {
        return (lpw[i] + rho * (egl[i] / (Math.PI * (1.0 - sab[i] * rho)))) / toaScaling;
    }

    public double getAdjacencyCorrection(int i, double rho, double ave) {
        return (rho - ave) * rat[i];
    }
}
