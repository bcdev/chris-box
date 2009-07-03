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

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ConversionsTest {

    @Test
    public void dateToJD() {
        final Date date = Conversions.mjdToDate(41317.0);
        assertEquals(41317.0, Conversions.dateToMJD(date), 0.0);

        final double jd = Conversions.dateToJD(date);
        assertEquals(41317.0, Conversions.jdToMJD(jd), 0.0);
    }

    @Test
    public void dateToMJD() {
        final GregorianCalendar epoch1858 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch1858.clear();
        epoch1858.set(1858, 10, 17, 0, 0, 0);

        assertEquals(0.0, Conversions.dateToMJD(epoch1858.getTime()), 0.0);

        final GregorianCalendar epoch2000 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        epoch2000.clear();
        epoch2000.set(2000, 0, 1, 0, 0, 0);

        assertEquals(51544.0, Conversions.dateToMJD(epoch2000.getTime()), 0.0);
    }

    @Test
    public void mjdToDate() {
        final Date date = Conversions.mjdToDate(41317.0);
        assertEquals(41317.0, Conversions.dateToMJD(date), 0.0);
    }

    @Test
    public void mjdToGST() {
        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2008, 10, 19, 15, 0, 0);

        final double mjd = Conversions.dateToMJD(calendar.getTime());
        final double gst = Conversions.mjdToGST(mjd);

        // expected result taken from Luis Alonso
        assertEquals(4.9569015, gst, 1.0E-7);
    }

    @Test
    public void julianDate() {
        double julDay0 = Conversions.julianDate(1999, 11, 26);
        assertEquals(2451538.5, julDay0, 0.0);
    }

}
