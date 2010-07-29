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

import java.io.InputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class IctReaderTest extends TestCase {

    public void testReadImageCenterTimes() throws IOException {
        final InputStream is = IctReaderTest.class.getResourceAsStream(
                "Pass2049.Barrax_13350_CHRIS_center_times_20030512_65534");
        
        IctDataRecord.IctDataReader reader = new IctDataRecord.IctDataReader(is);
        String[] lastIctRecord = reader.getLastIctRecord();
        assertEquals("2003.132.11.26.05.341", lastIctRecord[0]);
        assertEquals("294944", lastIctRecord[1]);
        assertEquals("+G:294944", lastIctRecord[2]);
        assertEquals("106572018.95829985", lastIctRecord[3]);
        assertEquals("+G:294944", lastIctRecord[4]);
        assertEquals("106571922.30086310", lastIctRecord[5]);
        assertEquals("+G:294944", lastIctRecord[6]);
        assertEquals("106571971.53328174", lastIctRecord[7]);
        assertEquals("+G:294944", lastIctRecord[8]);
        assertEquals("106572020.76570037", lastIctRecord[9]);
        assertEquals("+G:294944", lastIctRecord[10]);
        assertEquals("106572069.99811901", lastIctRecord[11]);
        assertEquals("+G:294944", lastIctRecord[12]);
        assertEquals("106572119.23053764", lastIctRecord[13]);
        
        double[] ictValues = reader.getLastIctValues();
        IctDataRecord ictData = IctDataRecord.create(ictValues, 13);

        assertNotNull(ictData);
        assertEquals(getUT1(2452771.9711724636), ictData.ict1, 1.0E-10);
        assertEquals(getUT1(2452771.9717422836), ictData.ict2, 1.0E-10);
        assertEquals(getUT1(2452771.9723121030), ictData.ict3, 1.0E-10);
        assertEquals(getUT1(2452771.9728819225), ictData.ict4, 1.0E-10);
        assertEquals(getUT1(2452771.9734517424), ictData.ict5, 1.0E-10);
    }

    private static double getUT1(double jd) throws IOException {
        return jd + TimeConverter.getInstance().deltaUT1(TimeConverter.jdToMJD(jd)) / TimeConverter.SECONDS_PER_DAY;
    }

}
