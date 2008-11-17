package org.esa.beam.chris.operators;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ConcurrentMap;
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
public class TimeCalculator {
    private static final AtomicReference<TimeCalculator> instance = new AtomicReference<TimeCalculator>();

    /**
     * The TAI-UTC table.
     * <p/>
     * Note that since 1972-JAN-01 the difference between TAI and UTC
     * is consisting of leap-seconds only.
     */
    private final ConcurrentNavigableMap<Double, Double> lsMap;
    /**
     * The UT1-UTC table.
     */
    private final ConcurrentNavigableMap<Double, Double> utMap;

    private TimeCalculator() {
        lsMap = new ConcurrentSkipListMap<Double, Double>();
        utMap = new ConcurrentSkipListMap<Double, Double>();
    }

    public static TimeCalculator getInstance() throws IOException {
        instance.compareAndSet(null, createInstance());
        return instance.get();
    }

    /**
     * Returns the number of seconds GPS time runs ahead of UT1
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds GPS time runs ahead of UT1.
     */
    public final double getUt1Gps(double mjd) {
        return getUtcGps(mjd) - getUtcUt1(mjd);
    }

    /**
     * Returns the number of seconds GPS time runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds GPS time runs ahead of UTC.
     */
    public final double getUtcGps(double mjd) {
        return getUtcTai(mjd) - 19.0;
    }

    /**
     * Returns the number of seconds UT1 runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds UT1 runs ahead of UTC.
     *         The sign of the number returned is negative,
     *         when UT1 lags behind UTC.
     */
    public final double getUtcUt1(double mjd) {
        if (mjd < utMap.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("UT1-UTC for MJD before {0} is not available", utMap.firstKey()));
        }
        if (mjd > utMap.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("UT1-UTC for MJD after {0} is not available", utMap.lastKey()));
        }
        return utMap.floorEntry(mjd).getValue();
    }

    /**
     * Returns the number of seconds International Atomic Time (TAI) runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds TAI runs ahead of UTC.
     */
    public final double getUtcTai(double mjd) {
        if (mjd < lsMap.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("TAI-UTC for MJD before {0} is not available", lsMap.firstKey()));
        }
        return lsMap.floorEntry(mjd).getValue();
    }

    /**
     * Converts a Julian Date (JD) into a Modified Julian Date (MJD).
     *
     * @param jd the Julian Date (JD).
     *
     * @return the Modified Julian Date (MJD).
     */
    static double toMJD(double jd) {
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
        readLsTable(is, lsMap);
    }

    private static TimeCalculator createInstance() throws IOException {
        final TimeCalculator calculator = new TimeCalculator();
        readLsTable(TimeCalculator.class.getResourceAsStream("leapsec.dat"), calculator.lsMap);
        readUtTable(TimeCalculator.class.getResourceAsStream("finals.data"), calculator.utMap);

        return calculator;
    }

    private static void readLsTable(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                final int datePos = line.indexOf("=JD");
                final int timePos = line.indexOf("TAI-UTC=");
                final int stopPos = line.indexOf(" S ");

                if (datePos == -1 || timePos == -1 || stopPos == -1) {
                    continue; // next line
                }

                final double jd;
                final double ls;

                try {
                    jd = Double.parseDouble(line.substring(datePos + 3, timePos));
                    ls = Double.parseDouble(line.substring(timePos + 8, stopPos));
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while reading the TAI-UTC table.", e);
                }

                map.putIfAbsent(toMJD(jd), ls);
            }
        } finally {
            scanner.close();
        }
    }

    private static void readUtTable(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                final String mjdString = line.substring(7, 15);
                final String utdString = line.substring(58, 68);

                if (mjdString.trim().isEmpty() || utdString.trim().isEmpty()) {
                    continue; // next line
                }

                final double mjd;
                final double utd;

                try {
                    mjd = Double.parseDouble(mjdString);
                    utd = Double.parseDouble(utdString);
                } catch (NumberFormatException e) {
                    throw new IOException("An error occurred while reading the UT1-UTC table.", e);
                }

                map.put(mjd, utd);
            }
        } finally {
            scanner.close();
        }
    }
}
