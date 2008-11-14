package org.esa.beam.chris.operators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for time conversion.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeConverter {
    private static final AtomicReference<TimeConverter> instance = new AtomicReference<TimeConverter>();

    private final ConcurrentNavigableMap<Double, Double> lsMap;

    private TimeConverter() {
        lsMap = new ConcurrentSkipListMap<Double, Double>();
    }

    public static TimeConverter getInstance() throws IOException {
        instance.compareAndSet(null, createInstance());
        return instance.get();
    }

    /**
     * Returns the leap seconds for a Modified Julian Date (MJD).
     *
     * @param mjd the MJD.
     *
     * @return the leap seconds for the MJD requested.
     */
    public final double getLeapSeconds(double mjd) {
        if (mjd >= lsMap.firstKey()) {
            return lsMap.floorEntry(mjd).getValue();
        }

        return 0;
    }

    /**
     * Converts a Julian Date (JD) into a Modified Julian Date (MJD).
     *
     * @param jd the Julian Date (JD).
     *
     * @return the Modified Julian Date (MJD.
     */
    public static double toMJD(double jd) {
        return jd - 2400000.5;
    }

    /**
     * Updates the internal leap seconds table with newer leap seconds data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the leap seconds data from the input stream.
     */
    public void updateLeapSecondsTable(InputStream is) throws IOException {
        readLeapSecondsTable(is, lsMap);
    }

    private static TimeConverter createInstance() throws IOException {
        final TimeConverter converter = new TimeConverter();
        readLeapSecondsTable(TimeConverter.class.getResourceAsStream("leapsec.dat"), converter.lsMap);

        return converter;
    }

    private static void readLeapSecondsTable(InputStream is,
                                             ConcurrentNavigableMap<Double, Double> map) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                final int datePos = line.indexOf("=JD");
                final int timePos = line.indexOf("TAI-UTC=");
                final int stopPos = line.indexOf("S");

                if (datePos == -1 || timePos == -1 || stopPos == -1) {
                    throw new IOException("An error occurred while reading the leap seconds table.");
                }

                final double jd;
                final double ls;

                try {
                    jd = Double.parseDouble(line.substring(datePos + 3, timePos));
                    ls = Double.parseDouble(line.substring(timePos + 8, stopPos));
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while reading the leap seconds table.", e);
                }

                map.putIfAbsent(toMJD(jd), ls);
            }
        } finally {
            scanner.close();
        }
    }
}
