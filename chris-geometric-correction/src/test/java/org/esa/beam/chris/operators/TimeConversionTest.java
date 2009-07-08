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

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Tests for class {@link TimeConversion}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeConversionTest {

    private static TimeConversion timeConversion;

    @Test
    public void getInstance() throws IOException {
        assertNotNull(timeConversion);
        assertSame(timeConversion, TimeConversion.getInstance());
    }

    @Test
    public void deltaGPS() {
        // 1999-JAN-01
        assertEquals(13.0, timeConversion.deltaGPS(51179.0), 0.0);
        // 2006-JAN-01
        assertEquals(14.0, timeConversion.deltaGPS(53736.0), 0.0);
        // 2009-JAN-01
        assertEquals(15.0, timeConversion.deltaGPS(54832.0), 0.0);
    }

    @Test
    public void deltaTAI() {
        try {
            timeConversion.deltaTAI(41316.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // 1972-JAN-01
        assertEquals(10.0, timeConversion.deltaTAI(41317.0), 0.0);

        // 1999-JAN-01
        assertEquals(31.0, timeConversion.deltaTAI(51178.0), 0.0);
        assertEquals(32.0, timeConversion.deltaTAI(51179.0), 0.0);

        // 2006-JAN-01
        assertEquals(32.0, timeConversion.deltaTAI(53735.0), 0.0);
        assertEquals(33.0, timeConversion.deltaTAI(53736.0), 0.0);
    }

    @Test
    public void deltaUT1() {
        try {
            timeConversion.deltaUT1(48621.0);
            fail();
        } catch (IllegalArgumentException expexted) {
        }

        // 1992-JAN-01
        assertEquals(-0.1251669, timeConversion.deltaUT1(48622.0), 0.0);

        // 2008-NOV-13
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        assertEquals(-0.5391980, timeConversion.deltaUT1(54783.0), 0.0);

        // 2008-NOV-13
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        assertEquals(-0.5403143, timeConversion.deltaUT1(54784.0), 0.0);

        // 2009-NOV-21
        // NOTE: Failure of this tests usually occurs because newer time data are used for calculation
        assertEquals(0.1470921, timeConversion.deltaUT1(55156.0), 0.0);
    }

    @Test
    public void dateToJD() {
        final Date date = TimeConversion.mjdToDate(41317.0);
        assertEquals(41317.0, TimeConversion.dateToMJD(date), 0.0);

        final double jd = TimeConversion.dateToJD(date);
        assertEquals(41317.0, TimeConversion.jdToMJD(jd), 0.0);
    }

    @Test
    public void dateToMJD() {
        final GregorianCalendar epoch1858 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch1858.clear();
        epoch1858.set(1858, 10, 17, 0, 0, 0);

        assertEquals(0.0, TimeConversion.dateToMJD(epoch1858.getTime()), 0.0);

        final GregorianCalendar epoch2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch2000.clear();
        epoch2000.set(2000, 0, 1, 0, 0, 0);

        assertEquals(51544.0, TimeConversion.dateToMJD(epoch2000.getTime()), 0.0);
    }

    @Test
    public void mjdToDate() {
        final Date date = TimeConversion.mjdToDate(41317.0);
        assertEquals(41317.0, TimeConversion.dateToMJD(date), 0.0);
    }

    @Test
    public void mjdToGST() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2008, 10, 19, 15, 0, 0);

        final double mjd = TimeConversion.dateToMJD(calendar.getTime());
        final double gst = TimeConversion.mjdToGST(mjd);

        // expected result taken from Luis Alonso
        assertEquals(4.9569015, gst, 1.0E-7);
    }

    @Test
    public void julianDate() {
        double jd = TimeConversion.julianDate(1999, 11, 26);
        assertEquals(2451538.5, jd, 0.0);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        timeConversion = TimeConversion.getInstance();
    }
}
