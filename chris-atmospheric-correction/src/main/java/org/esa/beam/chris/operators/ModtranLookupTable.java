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

import org.esa.beam.util.math.Array;
import org.esa.beam.util.math.VectorLookupTable;

import static java.lang.Math.*;

/**
 * MODTRAN lookup table used for atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class ModtranLookupTable {
    private final int parameterCount1;
    private final int parameterCount2;

    private final VectorLookupTable lut1;
    private final VectorLookupTable lut2;

    private final double[] wavelengths;

    public ModtranLookupTable(int parameterCount1, int parameterCount2, VectorLookupTable lut1,
                              VectorLookupTable lut2, Array wavelengths) {
        this.parameterCount1 = parameterCount1;
        this.parameterCount2 = parameterCount2;
        this.lut1 = lut1;
        this.lut2 = lut2;

        this.wavelengths = new double[wavelengths.getLength()];
        wavelengths.copyTo(0, this.wavelengths, 0, wavelengths.getLength());
    }

    /**
     * Returns an interpolated matrix of output parameters for given input parameters.
     *
     * @param vza the view zenith angle (degree).
     * @param sza the solar zenith angle (degree).
     * @param ada the relative azimuth angle (degree).
     * @param alt the target altitude (km).
     * @param aot the aerosol optical thickness at 550 nm.
     * @param cwv the integrated water vapour column (g cm-2).
     *
     * @return the output parameter matrix. The number of rows is equal to {@code wavelenghtCount}
     *         and the number of columns matches {@code parameterCount}. The tabulated atmospheric
     *         parameters are the following:
     *         <table>
     *         <tr>
     *         <td>Column</td>
     *         <td>Parameter</td>
     *         </tr>
     *         <tr>
     *         <td>1</td>
     *         <td>Atmospheric path radiance</td>
     *         </tr>
     *         <tr>
     *         <td>2</td>
     *         <td>Directed flux</td>
     *         </tr>
     *         <tr>
     *         <td>3</td>
     *         <td>Diffuse flux</td>
     *         </tr>
     *         <tr>
     *         <td>4</td>
     *         <td>Atmospheric spherical albedo</td>
     *         </tr>
     *         <tr>
     *         <td>5</td>
     *         <td>Ratio of diffuse to direct upward transmittance</td>
     *         </tr>
     *         </table>
     */
    public final double[][] getValues(double vza, double sza, double ada, double alt, double aot, double cwv) {
        final double[] values1 = lut1.getValues(vza, sza, alt, aot, ada);
        final double[] values2 = lut2.getValues(vza, sza, alt, aot, cwv);

        final double[][] matrix = new double[wavelengths.length][parameterCount1 + parameterCount2];

        for (int i = 0; i < wavelengths.length; ++i) {
            System.arraycopy(values1, parameterCount1 * i, matrix[i], 0, parameterCount1);
            System.arraycopy(values2, parameterCount2 * i, matrix[i], parameterCount1, parameterCount2);
        }

        return matrix;
    }

    public final int getParameterCount() {
        return parameterCount1 + parameterCount2;
    }

    public final int getWavelengthCount() {
        return wavelengths.length;
    }

    public final double[] getWavelengths() {
        return wavelengths.clone();
    }

    final int getParameterCount1() {
        return parameterCount1;
    }

    final int getParameterCount2() {
        return parameterCount2;
    }

    final VectorLookupTable getLut1() {
        return lut1;
    }

    final VectorLookupTable getLut2() {
        return lut2;
    }
}
