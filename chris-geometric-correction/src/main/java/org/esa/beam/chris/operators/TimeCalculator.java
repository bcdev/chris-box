package org.esa.beam.chris.operators;

import sun.net.www.protocol.ftp.FtpURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Calendar;
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
    /**
     * The epoch (millis) for the Modified Julian Date (MJD) which
     * corresponds to 1858-11-17 00:00.
     */
    private static final long EPOCH_MILLIS = -3506716800000L;

    private static final AtomicReference<TimeCalculator> instance = new AtomicReference<TimeCalculator>();

    /**
     * The TAI-UTC table.
     * <p/>
     * Note that since 1972-JAN-01 the difference between TAI and UTC
     * consists of leap-seconds only.
     */
    private final ConcurrentNavigableMap<Double, Double> tuTable;
    /**
     * The UT1-UTC table.
     */
    private final ConcurrentNavigableMap<Double, Double> uuTable;

    private TimeCalculator() {
        tuTable = new ConcurrentSkipListMap<Double, Double>();
        uuTable = new ConcurrentSkipListMap<Double, Double>();
    }

    /**
     * Returns a reference to the single instance of this class.
     * <p/>
     * When this method is called for the first time, a new instance
     * of this class is created.
     *
     * @return a reference to the single instance of this class.
     *
     * @throws IOException if an error occurred.
     */
    public static TimeCalculator getInstance() throws IOException {
        if (instance.get() == null) {
            instance.compareAndSet(null, createInstance());
        }

        return instance.get();
    }

    /**
     * Returns the number of seconds GPS time runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds GPS time runs ahead of UTC.
     */
    public final double getDeltaGPS(double mjd) {
        return getDeltaTAI(mjd) - 19.0;
    }

    /**
     * Returns the number of seconds International Atomic Time (TAI) runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds TAI runs ahead of UTC.
     */
    public final double getDeltaTAI(double mjd) {
        if (mjd < tuTable.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("TAI-UTC for MJD before {0} is not available", tuTable.firstKey()));
        }

        return tuTable.floorEntry(mjd).getValue();
    }

    /**
     * Returns the number of seconds UT1 runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds UT1 runs ahead of UTC. When UT1 lags
     *         behind UTC the sign of the number returned is negative.
     */
    public final double getDeltaUT1(double mjd) {
        if (mjd < uuTable.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("UT1-UTC for MJD before {0} is not available", uuTable.firstKey()));
        }
        if (mjd > uuTable.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("UT1-UTC for MJD after {0} is not available", uuTable.lastKey()));
        }

        return uuTable.floorEntry(mjd).getValue();
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a calendar.
     *
     * @param calendar the calendar.
     *
     * @return the MJD corresponding to the calendar.
     */
    public static double toMJD(Calendar calendar) {
        return (calendar.getTimeInMillis() - EPOCH_MILLIS) / (double) (24 * 3600 * 1000);
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a Julian Date (JD).
     *
     * @param jd the JD.
     *
     * @return the MJD corresponding to the JD.
     */
    static double toMJD(double jd) {
        return jd - 2400000.5;
    }

    /**
     * Updates the internal TAI-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the TAI-UTC data from the input stream.
     */
    private void updateTuTable(InputStream is) throws IOException {
        readTuTable(is, tuTable);
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the UT1-UTC data from the input stream.
     */
    private void updateUuTable(InputStream is) throws IOException {
        readUuTable(is, uuTable);
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param spec the string to parse a a URL.
     *
     * @throws IOException if an error occurred.
     */
    private void updateTuTable(String spec) throws IOException {
        final URL url = new URL(spec);
        final URLConnection connection = new FtpURLConnection(url);

        updateTuTable(connection.getInputStream());
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param spec the string to parse a a URL.
     *
     * @throws IOException if an error occurred.
     */
    private void updateUuTable(String spec) throws IOException {
        final URL url = new URL(spec);
        final URLConnection connection = new FtpURLConnection(url);

        updateUuTable(connection.getInputStream());
    }

    private static TimeCalculator createInstance() throws IOException {
        final TimeCalculator timeCalculator = new TimeCalculator();
        readTuTable(TimeCalculator.class.getResourceAsStream("leapsec.dat"), timeCalculator.tuTable);
        readUuTable(TimeCalculator.class.getResourceAsStream("finals.data"), timeCalculator.uuTable);

        try {
            timeCalculator.updateTuTable("ftp://maia.usno.navy.mil/ser7/leapsec.dat");
            timeCalculator.updateUuTable("ftp://maia.usno.navy.mil/ser7/finals.data");
        } catch (IOException ignored) {
            // todo - warning
        }

        return timeCalculator;
    }

    private static void readTuTable(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                final int datePos = line.indexOf("=JD");
                final int timePos = line.indexOf("TAI-UTC=");
                final int stopPos = line.indexOf(" S ");

                if (datePos == -1 || timePos == -1 || stopPos == -1) {
                    continue; // try next line
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

    private static void readUuTable(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                final String mjdString = line.substring(7, 15);
                final String utdString = line.substring(58, 68);

                if (mjdString.trim().isEmpty() || utdString.trim().isEmpty()) {
                    continue; // try next line
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
