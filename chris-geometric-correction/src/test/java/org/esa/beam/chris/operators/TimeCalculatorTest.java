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

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Tests for class {@link TimeCalculator}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeCalculatorTest extends TestCase {

    private TimeCalculator timeCalculator;

    public void testGetInstance() throws IOException {
        assertNotNull(timeCalculator);
        assertSame(timeCalculator, TimeCalculator.getInstance());
    }

    public void testToDate() {
        final Date date = TimeCalculator.toDate(41317.0);
        assertEquals(41317.0, TimeCalculator.toMJD(date), 0.0);
    }

    public void testToGST() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2008, 10, 19, 15, 0, 0);

        final double gst = TimeCalculator.toGST(TimeCalculator.toMJD(calendar.getTime()));
        assertEquals(4.9569015, gst, 1.0E-7);
    }

    public void testToMJD() {
        final GregorianCalendar epoch1858 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch1858.clear();
        epoch1858.set(1858, 10, 17, 0, 0, 0);

        assertEquals(0.0, TimeCalculator.toMJD(epoch1858.getTime()), 0.0);

        final GregorianCalendar epoch2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch2000.clear();
        epoch2000.set(2000, 0, 1, 0, 0, 0);

        assertEquals(51544.0, TimeCalculator.toMJD(epoch2000.getTime()), 0.0);
    }

    public void testDeltaGPS() {
        // 1999-JAN-01
        assertEquals(32.0 - 19.0, timeCalculator.deltaGPS(51179.0), 0.0);
        // 2006-JAN-01
        assertEquals(33.0 - 19.0, timeCalculator.deltaGPS(53736.0), 0.0);
    }

    public void testDeltaTAI() {
        try {
            timeCalculator.deltaTAI(41316.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // 1972-JAN-01
        assertEquals(10.0, timeCalculator.deltaTAI(41317.0), 0.0);

        // 1999-JAN-01
        assertEquals(31.0, timeCalculator.deltaTAI(51178.0), 0.0);
        assertEquals(32.0, timeCalculator.deltaTAI(51179.0), 0.0);

        // 2006-JAN-01
        assertEquals(32.0, timeCalculator.deltaTAI(53735.0), 0.0);
        assertEquals(33.0, timeCalculator.deltaTAI(53736.0), 0.0);
    }

    public void testDeltaUT1() {
        try {
            timeCalculator.deltaUT1(48621.0);
            fail();
        } catch (IllegalArgumentException expexted) {
        }

        // 1992-JAN-01
        assertEquals(-0.1251669, timeCalculator.deltaUT1(48622.0), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5390396, timeCalculator.deltaUT1(54783.0), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5400264, timeCalculator.deltaUT1(54784.0), 0.0);

        // 2009-NOV-21
        assertEquals(0.1716805, timeCalculator.deltaUT1(55156.0), 0.0);

        try {
            timeCalculator.deltaUT1(55157.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Override
    protected void setUp() throws Exception {
        timeCalculator = TimeCalculator.getInstance();
    }
}
