/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;


public class GpsDataReaderTest extends TestCase {

    public void testReadGPSTimes() throws IOException {
        final InputStream is = GpsDataReaderTest.class.getResourceAsStream(
                "CHRIS_13350_13354_PROBA1_GPS_Data");
        
        GpsDataRecord.GpsDataReader reader = new GpsDataRecord.GpsDataReader(is);
        List<String[]> readRecords = reader.getReadRecords();
        assertNotNull(readRecords);
        assertEquals(183, readRecords.size());
        
        List<GpsDataRecord> gpsDataRecordList = GpsDataRecord.create(readRecords, 0, 0);
        assertNotNull(gpsDataRecordList);
        assertEquals(183, gpsDataRecordList.size());
        
        GpsDataRecord gpsDataRecord = gpsDataRecordList.get(0);
        assertNotNull(gpsDataRecord);
        assertEquals(getUT1(2452771.965436449), gpsDataRecord.jd, 0.000001);
        
    }

    private static double getUT1(double jd) throws IOException {
        return jd + TimeConverter.getInstance().deltaUT1(TimeConverter.jdToMJD(jd)) / TimeConverter.SECONDS_PER_DAY;
    }

}
