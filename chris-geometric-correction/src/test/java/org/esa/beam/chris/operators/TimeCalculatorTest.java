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

import java.util.concurrent.atomic.AtomicReference;
import java.util.Scanner;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.InputStream;

/**
 * Tests.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.0
 */
public class TimeCalculatorTest extends TestCase {
    private TimeConverter timeConverter;

    public void testGetInstance() {
        assertNotNull(timeConverter);
        assertSame(timeConverter, TimeConverter.getInstance());
    }

    public void testGetLeapSeconds() {
        // 1999-JAN-01
        assertEquals(31, timeConverter.getLeapSeconds(TimeConverter.toMJD(2451178.5)));
        assertEquals(32, timeConverter.getLeapSeconds(TimeConverter.toMJD(2451179.5)));

        // 2006-JAN-01
        assertEquals(32, timeConverter.getLeapSeconds(TimeConverter.toMJD(2453735.5)));
        assertEquals(33, timeConverter.getLeapSeconds(TimeConverter.toMJD(2453736.5)));
    }

    @Override
    protected void setUp() throws Exception {
        timeConverter = TimeConverter.getInstance();
    }

    public static class TimeConverter {
        private static final AtomicReference<TimeConverter> instance = new AtomicReference<TimeConverter>();

        private TimeConverter() {
            final InputStream is = TimeConverter.class.getResourceAsStream("leapsec.dat");

            final Scanner scanner = new Scanner(is, "US-ASCII");
            scanner.useLocale(Locale.US);

            final List<Double> x = new ArrayList<Double>();
            final List<Double> y = new ArrayList<Double>();

            Pattern.compile("\\d\\d\\d\\d\\s\\w\\w\\w\\s\\s\\d\\s=JD 2453736.5  TAI-UTC=  33.0       S + (MJD - 41317.) X 0.0      S");
            try {
                while (scanner.hasNextLine()) {
                    scanner.next("2006 JAN  1 =JD 2453736.5  TAI-UTC=  33.0       S + (MJD - 41317.) X 0.0      S");
                    x.add(scanner.nextDouble());
                    y.add(scanner.nextDouble());
                }
            } finally {
                scanner.close();
            }
        }

        public static TimeConverter getInstance() {
            instance.compareAndSet(null, new TimeConverter());
            return instance.get();
        }

        /**
         * Converts a Julian Date (JD) into a Modified Julian Date (MJD).
         *
         * @param jd the Julian Day (JD).
         *
         * @return the Modified Julian Date (MJD.
         */
        public static double toMJD(double jd) {
            return jd - 2400000.5;
        }

        public int getLeapSeconds(double mjd) {
            return 0;
        }
    }
}
