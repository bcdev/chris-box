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

import org.esa.beam.util.math.*;

/**
 * MODTRAN lookup table used for atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class ModtranLookupTable {

    public static final int VZA = 0;
    public static final int SZA = 1;
    public static final int ADA = 2;
    public static final int ALT = 3;
    public static final int AOT = 4;
    public static final int CWV = 5;

    public static final int LPW = 0;
    public static final int EGL = 5;
    public static final int SAB = 3;

    private final double[] wavelengths;
    private final VectorLookupTable lutA;
    private final MatrixLookupTable lutB;

    public ModtranLookupTable(Array wavelengths, VectorLookupTable lutA, VectorLookupTable lutB) {
        this.wavelengths = new double[wavelengths.getLength()];

        this.lutA = lutA;
        this.lutB = new MatrixLookupTable(4, wavelengths.getLength(), new ColumnMajorMatrixFactory(), lutB);

        wavelengths.copyTo(0, this.wavelengths, 0, wavelengths.getLength());
    }

    /**
     * Returns the table of radiative transfer calculations for the input parameters
     * supplied as arguments.
     *
     * @param vza the view zenith angle (degree).
     * @param sza the solar zenith angle (degree).
     * @param ada the relative azimuth angle (degree).
     * @param alt the target altitude (km).
     * @param aot the aerosol optical thickness at 550 nm.
     * @param cwv the integrated water vapour column (g cm-2).
     *
     * @return the table of radiative transfer calculations. The number of rows in the table
     *         is equal to the number of output parameters defined in the table below, while
     *         the number of columns matches the number of spectral wavelenghts.
     *         <p/>
     *         <table>
     *         <tr>
     *         <td>Row</td>
     *         <td>Parameter</td>
     *         </tr>
     *         <tr>
     *         <td>1</td>
     *         <td>Atmospheric path radiance</td>
     *         </tr>
     *         <tr>
     *         <td>2</td>
     *         <td>Directed flux at ground</td>
     *         </tr>
     *         <tr>
     *         <td>3</td>
     *         <td>Diffuse flux at ground</td>
     *         </tr>
     *         <tr>
     *         <td>4</td>
     *         <td>Atmospheric spherical albedo</td>
     *         </tr>
     *         <tr>
     *         <td>5</td>
     *         <td>Ratio of diffuse to direct upward transmittance</td>
     *         </tr>
     *         <tr>
     *         <td>6</td>
     *         <td>Global flux at ground</td>
     *         </tr>
     *         </table>
     */
    public final double[][] getRtmTable(double vza, double sza, double ada, double alt, double aot, double cwv) {
        final double[] valuesA = lutA.getValues(vza, sza, alt, aot, ada);
        final double[][] valuesB = lutB.getValues(vza, sza, alt, aot, cwv);

        final int m = 1;
        final int n = valuesB.length;

        final double[][] table = new double[m + n + 1][wavelengths.length];

        table[0] = valuesA;
        table[1] = valuesB[0];
        table[2] = valuesB[1];
        table[3] = valuesB[2];
        table[4] = valuesB[3];

        // compute global flux at ground
        final double szc = Math.cos(Math.toRadians(sza));
        for (int j = 0; j < wavelengths.length; ++j) {
            table[5][j] = table[1][j] * szc + table[2][j];
        }

        return table;
    }
 
    /**
     * Returns the number of wavelengths.
     *
     * @return the number of wavelengths.
     */
    public int getWavelengthCount() {
        return wavelengths.length;
    }

    /**
     * Returns the wavelengths.
     *
     * @return the wavelengths.
     */
    public final double[] getWavelengths() {
        return wavelengths.clone();
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return 6;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     *
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(int i) {
        switch (i) {
            case VZA:
                return lutB.getDimension(0);
            case SZA:
                return lutB.getDimension(1);
            case ADA:
                return lutA.getDimension(4);
            case ALT:
                return lutB.getDimension(2);
            case AOT:
                return lutB.getDimension(3);
            case CWV:
                return lutB.getDimension(4);
            default:
                throw new IllegalArgumentException("illegal dimension index number");
        }
    }

    final VectorLookupTable getLutA() {
        return lutA;
    }

    final MatrixLookupTable getLutB() {
        return lutB;
    }
}
