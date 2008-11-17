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

    public void testGetUtcTai() {
        try {
            timeCalculator.getUtcTai(TimeCalculator.toMJD(2441316.5));
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // 1972-JAN-01
        assertEquals(10.0, timeCalculator.getUtcTai(TimeCalculator.toMJD(2441317.5)), 0.0);

        // 1999-JAN-01
        assertEquals(31.0, timeCalculator.getUtcTai(TimeCalculator.toMJD(2451178.5)), 0.0);
        assertEquals(32.0, timeCalculator.getUtcTai(TimeCalculator.toMJD(2451179.5)), 0.0);

        // 2006-JAN-01
        assertEquals(32.0, timeCalculator.getUtcTai(TimeCalculator.toMJD(2453735.5)), 0.0);
        assertEquals(33.0, timeCalculator.getUtcTai(TimeCalculator.toMJD(2453736.5)), 0.0);
    }

    public void testGetUtcUt1() {
        try {
            timeCalculator.getUtcUt1(48621.0);
            fail();
        } catch (IllegalArgumentException expexted) {
        }

        // 1992-JAN-01
        assertEquals(-0.1251669, timeCalculator.getUtcUt1(48622.0), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5390396, timeCalculator.getUtcUt1(54783.0), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5400264, timeCalculator.getUtcUt1(54784.0), 0.0);

        // 2009-NOV-21
        assertEquals(0.1716805, timeCalculator.getUtcUt1(55156.0), 0.0);

        try {
            timeCalculator.getUtcUt1(55157.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Override
    protected void setUp() throws Exception {
        timeCalculator = TimeCalculator.getInstance();
    }
}
