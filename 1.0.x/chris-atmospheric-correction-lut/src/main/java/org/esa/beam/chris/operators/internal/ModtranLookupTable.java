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
package org.esa.beam.chris.operators.internal;

import org.esa.beam.util.math.Array;
import org.esa.beam.util.math.ColumnMajorMatrixFactory;
import org.esa.beam.util.math.MatrixLookupTable;
import org.esa.beam.util.math.VectorLookupTable;

/**
 * MODTRAN lookup table.
 *
 * @author Ralf Quast
 * @version $Revision: 2703 $ $Date: 2008-07-15 11:42:48 +0200 (Di, 15 Jul 2008) $
 */
public class ModtranLookupTable {

    public static final int VZA = 0;
    public static final int SZA = 1;
    public static final int ADA = 2;
    public static final int ALT = 3;
    public static final int AOT = 4;
    public static final int CWV = 5;

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
     * Creates the table of radiative transfer calculations for the input parameters
     * supplied as arguments.
     *
     * @param vza the view zenith angle (degree).
     * @param sza the solar zenith angle (degree).
     * @param ada the relative azimuth angle (degree).
     * @param alt the target altitude (km).
     * @param aot the aerosol optical thickness at 550 nm.
     * @param cwv the integrated water vapour column (g cm-2).
     *
     * @return the table of radiative transfer calculations.
     */
    public final RtcTable getRtcTable(double vza, double sza, double ada, double alt, double aot, double cwv) {
        final double[] valuesA = lutA.getValues(vza, sza, alt, aot, ada);
        final double[][] valuesB = lutB.getValues(vza, sza, alt, aot, cwv);

        // compute global fluxes at ground
        final double szc = Math.cos(Math.toRadians(sza));
        for (int j = 0; j < wavelengths.length; ++j) {
            valuesB[0][j] = valuesB[0][j] * szc + valuesB[1][j];
        }

        return new RtcTable(valuesA, valuesB[0], valuesB[2], valuesB[3]);
    }

    /**
     * Returns the wavelenghts.
     *
     * @return the wavelengths.
     */
    public final double[] getWavelengths() {
        return wavelengths;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     *
     * @return the ith dimension.
     */
    public final double[] getDimension(int i) {
        switch (i) {
            case VZA:
                return lutB.getDimension(0).getSequence();
            case SZA:
                return lutB.getDimension(1).getSequence();
            case ADA:
                return lutA.getDimension(4).getSequence();
            case ALT:
                return lutB.getDimension(2).getSequence();
            case AOT:
                return lutB.getDimension(3).getSequence();
            case CWV:
                return lutB.getDimension(4).getSequence();
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
