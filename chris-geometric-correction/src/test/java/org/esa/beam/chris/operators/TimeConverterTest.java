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
 * Tests for class {@link TimeConverter}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeConverterTest extends TestCase {
    private TimeConverter timeConverter;

    public void testGetInstance() throws IOException {
        assertNotNull(timeConverter);
        assertSame(timeConverter, TimeConverter.getInstance());
    }

    public void testGetLeapSeconds() {
        // 1999-JAN-01
        assertEquals(31.0, timeConverter.getLeapSeconds(TimeConverter.toMJD(2451178.5)), 0.0);
        assertEquals(32.0, timeConverter.getLeapSeconds(TimeConverter.toMJD(2451179.5)), 0.0);

        // 2006-JAN-01
        assertEquals(32.0, timeConverter.getLeapSeconds(TimeConverter.toMJD(2453735.5)), 0.0);
        assertEquals(33.0, timeConverter.getLeapSeconds(TimeConverter.toMJD(2453736.5)), 0.0);
    }

    @Override
    protected void setUp() throws Exception {
        timeConverter = TimeConverter.getInstance();
    }
}
