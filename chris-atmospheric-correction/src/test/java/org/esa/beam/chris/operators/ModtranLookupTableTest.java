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

import junit.framework.TestCase;
import org.esa.beam.util.math.MatrixFactory;
import org.esa.beam.util.math.RowMajorMatrixFactory;

/**
 * Tests for {@link ModtranLookupTable}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ModtranLookupTableTest extends TestCase {
    private ModtranLookupTable lut;

    @Override
    protected void setUp() throws Exception {
//        lut = new ModtranLookupTableReader().readLookupTable(); // too expensive for unit-testing
    }

    @Override
    protected void tearDown() throws Exception {
        lut = null;
    }

    public void testLookupTableIntegrity() {
        if (lut != null) {
            checkLookupTable1();
            checkLookupTable2();
            checkModtranLookupTable();
        }
    }

    private void checkLookupTable1() {
        double[] values;
        // vza  = 20.0
        // sza  = 35.0
        // elev = 0.3   target elevation
        // aot  = 0.2   AOT at 550nm
        // raa  = 145.0 relative azimuth angle
        values = lut.getLut1().getValues(20.0, 35.0, 0.3, 0.2, 145.0);

        assertEquals(lut.getWavelengthCount(), values.length);
        assertEquals(0.002960650, values[104], 0.5E-8);
        assertEquals(0.000294274, values[472], 0.5E-9);

        // vza  = 40.0
        // sza  = 55.0
        // elev = 0.1  target elevation
        // aot  = 0.3  AOT at 550nm
        // raa  = 45.0 relative azimuth angle
        values = lut.getLut1().getValues(40.0, 55.0, 0.1, 0.3, 45.0);

        assertEquals(lut.getWavelengthCount(), values.length);
        assertEquals(0.004093020, values[136], 0.5E-8);
        assertEquals(0.000631324, values[446], 0.5E-9);
    }

    private void checkLookupTable2() {
        final MatrixFactory matrixFactory = new RowMajorMatrixFactory();

        double[][] values;
        // vza  = 20.0
        // sza  = 35.0
        // elev = 0.3  target elevation
        // aot  = 0.2  AOT at 550nm
        // cwv  = 2.0  integrated water vapour
        values = matrixFactory.createMatrix(lut.getWavelengthCount(), lut.getParameterCount2(),
                                            lut.getLut2().getValues(20.0, 35.0, 0.3, 0.2, 2.0));

        assertEquals(0.1084700, values[110][0], 0.5E-6);
        assertEquals(0.0333388, values[110][1], 0.5E-7);
        assertEquals(0.1479490, values[110][2], 0.5E-6);
        assertEquals(0.3042250, values[110][3], 0.5E-6);

        assertEquals(0.05969390, values[627][0], 0.5E-7);
        assertEquals(0.00439437, values[627][1], 0.5E-8);
        assertEquals(0.03657480, values[627][2], 0.5E-7);
        assertEquals(0.07540260, values[627][3], 0.5E-7);

        // vza  = 40.0
        // sza  = 55.0
        // elev = 0.1  target elevation
        // aot  = 0.3  AOT at 550nm
        // cwv  = 3.0  integrated water vapour
        values = matrixFactory.createMatrix(lut.getWavelengthCount(), lut.getParameterCount2(),
                                            lut.getLut2().getValues(40.0, 55.0, 0.1, 0.3, 3.0));

        assertEquals(0.0756223, values[222][0], 0.5E-7);
        assertEquals(0.0227272, values[222][1], 0.5E-7);
        assertEquals(0.1133030, values[222][2], 0.5E-6);
        assertEquals(0.4021710, values[222][3], 0.5E-6);

        assertEquals(0.0662339, values[462][0], 0.5E-7);
        assertEquals(0.0101405, values[462][1], 0.5E-7);
        assertEquals(0.0646544, values[462][2], 0.5E-7);
        assertEquals(0.2110600, values[462][3], 0.5E-6);
    }

    private void checkModtranLookupTable() {
        double[][] matrix;
        matrix = lut.getValues(20.0, 35.0, 145.0, 0.3, 0.2, 2.0);
        assertEquals(lut.getWavelengthCount(), matrix.length);

        double[] parameters;
        parameters = matrix[70];
        assertEquals(lut.getParameterCount(), parameters.length);

        assertEquals(0.00423624, parameters[0], 0.5E-8);
        assertEquals(0.10402400, parameters[1], 0.5E-6);
        assertEquals(0.03887840, parameters[2], 0.5E-7);
        assertEquals(0.17851200, parameters[3], 0.5E-6);
        assertEquals(0.36571800, parameters[4], 0.5E-6);

        matrix = lut.getValues(40.0, 55.0, 45.0, 0.1, 0.3, 3.0);
        assertEquals(lut.getWavelengthCount(), matrix.length);

        parameters = matrix[17];
        assertEquals(lut.getParameterCount(), parameters.length);

        assertEquals(0.00809511, parameters[0], 0.5E-8);
        assertEquals(0.03731340, parameters[1], 0.5E-7);
        assertEquals(0.03066440, parameters[2], 0.5E-7);
        assertEquals(0.25710700, parameters[3], 0.5E-6);
        assertEquals(1.00742000, parameters[4], 0.5E-5);
    }
}
