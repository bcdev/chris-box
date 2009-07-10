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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests for class {@link ModtranLookupTable}.
 * <p/>
 * Note that running all tests requires about 140 MB of heap space.
 *
 * @author Ralf Quast
 * @version $Revision: 2864 $ $Date: 2008-08-06 11:34:55 +0200 (Mi, 06 Aug 2008) $
 */
public class ModtranLookupTableTest {

    // unit conversion constant
    private static final double DEKA_KILO = 1.0E4;

    private ModtranLookupTable modtranLookupTable;

    // @Before
    public void before() throws IOException {
        modtranLookupTable = new ModtranLookupTableReader().readModtranLookupTable();
    }

    @Test
    public void placebo() {
        // JUnit expects that at least a single test is declared
    }

    // @Test
    public void lookupTablePresence() {
        assertNotNull(getClass().getResourceAsStream(ModtranLookupTableReader.LUT_FILE_NAME));
    }

    // @Test
    public void lookupTableIntegrity() {
        checkLookupTableA();
        checkLookupTableB();
        checkModtranLookupTable();
        checkWaterVapourDimension();
    }

    private void checkLookupTableA() {
        double[] values;
        // vza = 20.0
        // sza = 35.0
        // alt = 0.3   target elevation
        // aot = 0.2   AOT at 550nm
        // ada = 145.0 relative azimuth angle
        values = modtranLookupTable.getLutA().getValues(20.0, 35.0, 0.3, 0.2, 145.0);

        assertEquals(0.002960650 * DEKA_KILO, values[104], 0.5E-8 * DEKA_KILO);
        assertEquals(0.000294274 * DEKA_KILO, values[472], 0.5E-9 * DEKA_KILO);

        // vza = 40.0
        // sza = 55.0
        // alt = 0.1  target elevation
        // aot = 0.3  AOT at 550nm
        // ada = 45.0 relative azimuth angle
        values = modtranLookupTable.getLutA().getValues(40.0, 55.0, 0.1, 0.3, 45.0);

        assertEquals(0.004093020 * DEKA_KILO, values[136], 0.5E-8 * DEKA_KILO);
        assertEquals(0.000631324 * DEKA_KILO, values[446], 0.5E-9 * DEKA_KILO);
    }

    private void checkLookupTableB() {
        double[][] values;
        // vza = 20.0
        // sza = 35.0
        // alt = 0.3  target elevation
        // aot = 0.2  AOT at 550nm
        // cwv = 2.0  integrated water vapour
        values = modtranLookupTable.getLutB().getValues(20.0, 35.0, 0.3, 0.2, 2.0);

        assertEquals(0.1084700 * DEKA_KILO, values[0][110], 0.5E-5 * DEKA_KILO);
        assertEquals(0.0333388 * DEKA_KILO, values[1][110], 0.5E-7 * DEKA_KILO);
        assertEquals(0.1479490, values[2][110], 0.5E-6);
        assertEquals(0.3042250, values[3][110], 0.5E-6);

        assertEquals(0.05969390 * DEKA_KILO, values[0][627], 0.5E-7 * DEKA_KILO);
        assertEquals(0.00439437 * DEKA_KILO, values[1][627], 0.5E-8 * DEKA_KILO);
        assertEquals(0.03657480, values[2][627], 0.5E-7);
        assertEquals(0.07540260, values[3][627], 0.5E-7);

        // vza = 40.0
        // sza = 55.0
        // alt = 0.1  target elevation
        // aot = 0.3  AOT at 550nm
        // cwv = 3.0  integrated water vapour
        values = modtranLookupTable.getLutB().getValues(40.0, 55.0, 0.1, 0.3, 3.0);

        assertEquals(0.0756223 * DEKA_KILO, values[0][222], 0.5E-7 * DEKA_KILO);
        assertEquals(0.0227272 * DEKA_KILO, values[1][222], 0.5E-7 * DEKA_KILO);
        assertEquals(0.1133030, values[2][222], 0.5E-6);
        assertEquals(0.4021710, values[3][222], 0.5E-6);

        assertEquals(0.0662339 * DEKA_KILO, values[0][462], 0.5E-7 * DEKA_KILO);
        assertEquals(0.0101405 * DEKA_KILO, values[1][462], 0.5E-4 * DEKA_KILO);
        assertEquals(0.0646544, values[2][462], 0.5E-7);
        assertEquals(0.2110600, values[3][462], 0.5E-6);
    }

    private void checkModtranLookupTable() {
        RtcTable table;

        // vza = 20.0
        // sza = 35.0
        // ada = 145.0
        // alt = 0.3  target elevation
        // aot = 0.2  AOT at 550nm
        // cwv = 2.0  integrated water vapour
        table = modtranLookupTable.getRtcTable(20.0, 35.0, 145.0, 0.3, 0.2, 2.0);

        assertEquals(0.00423624 * DEKA_KILO, table.getLpw(70), 0.5E-8 * DEKA_KILO);
        assertEquals(0.12408900 * DEKA_KILO, table.getEgl(70), 0.5E-6 * DEKA_KILO);
        assertEquals(0.17851200, table.getSab(70), 0.5E-6);
        assertEquals(0.36571800, table.getRat(70), 0.5E-6);

        // vza = 40.0
        // sza = 55.0
        // ada = 45.0
        // alt = 0.1  target elevation
        // aot = 0.3  AOT at 550nm
        // cwv = 3.0  integrated water vapour
        table = modtranLookupTable.getRtcTable(40.0, 55.0, 45.0, 0.1, 0.3, 3.0);

        assertEquals(0.00809511 * DEKA_KILO, table.getLpw(17), 0.5E-8 * DEKA_KILO);
        assertEquals(0.05206649 * DEKA_KILO, table.getEgl(17), 0.5E-7 * DEKA_KILO);
        assertEquals(0.25710700, table.getSab(17), 0.5E-6);
        assertEquals(1.00742000, table.getRat(17), 0.5E-5);

        // vza = 20.0
        // sza = 20.0
        // ada = 20.0
        // alt = 0.40  target elevation
        // aot = 0.23  AOT at 550nm
        // cwv = 1.80  integrated water vapour
        table = modtranLookupTable.getRtcTable(20.0, 20.0, 20.0, 0.40, 0.23, 1.80);

        assertEquals(0.00839986 * DEKA_KILO, table.getLpw(0), 0.5E-8 * DEKA_KILO);
        assertEquals(0.10621177 * DEKA_KILO, table.getEgl(0), 0.5E-5 * DEKA_KILO);
        assertEquals(0.26435800, table.getSab(0), 0.5E-6);
        assertEquals(0.62973800, table.getRat(0), 0.5E-6);
    }

    private void checkWaterVapourDimension() {
        final double[] dimension = modtranLookupTable.getDimension(ModtranLookupTable.CWV);

        assertEquals(7, dimension.length);
        assertEquals(0.3, dimension[0], 1.0E-6);
        assertEquals(5.0, dimension[dimension.length - 1], 1.0E-6);
    }
}
