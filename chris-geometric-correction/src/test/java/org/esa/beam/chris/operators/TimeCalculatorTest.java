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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Tests for class {@link TimeCalculator}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeCalculatorTest {

    private TimeCalculator timeCalculator;

    @Test
    public void getInstance() throws IOException {
        assertNotNull(timeCalculator);
        assertSame(timeCalculator, TimeCalculator.getInstance());
    }

    @Test
    public void deltaGPS() {
        // 1999-JAN-01
        assertEquals(13.0, timeCalculator.deltaGPS(51179.0), 0.0);
        // 2006-JAN-01
        assertEquals(14.0, timeCalculator.deltaGPS(53736.0), 0.0);
        // 2009-JAN-01
        assertEquals(15.0, timeCalculator.deltaGPS(54832.0), 0.0);
    }

    @Test
    public void deltaTAI() {
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

    @Test
    public void deltaUT1() {
        try {
            timeCalculator.deltaUT1(48621.0);
            fail();
        } catch (IllegalArgumentException expexted) {
        }

        // 1992-JAN-01
        assertEquals(-0.1251669, timeCalculator.deltaUT1(48622.0), 0.0);

        // 2008-NOV-13
        // TODO - update time auxiliary data
        assertEquals(-0.5391982, timeCalculator.deltaUT1(54783.0), 0.0);

        // 2008-NOV-13
        assertEquals(-0.5403142, timeCalculator.deltaUT1(54784.0), 0.0);

        // 2009-NOV-21
        assertEquals(0.1493375, timeCalculator.deltaUT1(55156.0), 0.0);

        try {
            final double lastPrediction = 55373.0;
            timeCalculator.deltaUT1(lastPrediction + 1.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Before
    public void before() throws Exception {
        timeCalculator = TimeCalculator.getInstance();
    }
}
