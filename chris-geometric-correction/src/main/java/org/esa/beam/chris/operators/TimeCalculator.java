package org.esa.beam.chris.operators;

import sun.net.www.protocol.ftp.FtpURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Utility class for time conversion.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeCalculator {

    private static final String PROPERTY_KEY_FETCH_NEWEST_TIME_DATA = "org.esa.beam.chris.fetchNewestTimeData";

    private static volatile TimeCalculator uniqueInstance;

    /**
     * Internal TAI-UTC table.
     * <p/>
     * Since 1972-JAN-01 the difference between TAI and UTC
     * consists of leap-seconds only.
     */
    private final ConcurrentNavigableMap<Double, Double> tai;

    /**
     * Internal UT1-UTC table.
     */
    private final ConcurrentNavigableMap<Double, Double> ut1;

    private TimeCalculator() {
        tai = new ConcurrentSkipListMap<Double, Double>();
        ut1 = new ConcurrentSkipListMap<Double, Double>();
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
        if (uniqueInstance == null) {
            synchronized (TimeCalculator.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = createInstance();
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * Returns the number of seconds GPS time runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds GPS time runs ahead of UTC.
     */
    public final double deltaGPS(double mjd) {
        return deltaTAI(mjd) - 19.0;
    }

    /**
     * Returns the number of seconds International Atomic Time (TAI) runs ahead of UTC
     * for a Modified Julian Date (MJD) of interest.
     *
     * @param mjd the MJD.
     *
     * @return the number of seconds TAI runs ahead of UTC.
     */
    public final double deltaTAI(double mjd) {
        if (mjd < tai.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No TAI-UTC data available before {0}.", Conversions.mjdToDate(tai.firstKey())));
        }
        // todo - interpolate? (rq-20090623)
        return tai.floorEntry(mjd).getValue();
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
    public final double deltaUT1(double mjd) {
        if (mjd < ut1.firstKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available before {0}.", Conversions.mjdToDate(ut1.firstKey())));
        }
        if (mjd > ut1.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available after {0}.", Conversions.mjdToDate(ut1.lastKey())));
        }
        // todo - interpolate? (rq-20090623)
        return ut1.floorEntry(mjd).getValue();
    }

    /**
     * Updates the internal TAI-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the TAI-UTC data from the input stream.
     */
    private void updateTAI(InputStream is) throws IOException {
        readTAI(is, tai);
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the UT1-UTC data from the input stream.
     */
    private void updateUT1(InputStream is) throws IOException {
        readUT1(is, ut1);
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param spec the string to parse a a URL.
     *
     * @throws IOException if an error occurred.
     */
    private void updateTAI(String spec) throws IOException {
        final URL url = new URL(spec);
        final URLConnection connection = new FtpURLConnection(url);

        updateTAI(connection.getInputStream());
    }

    /**
     * Updates the internal UT1-UTC table with newer data read from a URL.
     *
     * @param spec the string to parse a a URL.
     *
     * @throws IOException if an error occurred.
     */
    private void updateUT1(String spec) throws IOException {
        final URL url = new URL(spec);
        final URLConnection connection = new FtpURLConnection(url);

        updateUT1(connection.getInputStream());
    }

    private static TimeCalculator createInstance() throws IOException {
        final TimeCalculator timeCalculator = new TimeCalculator();
        readTAI(TimeCalculator.class.getResourceAsStream("leapsec.dat"), timeCalculator.tai);
        readUT1(TimeCalculator.class.getResourceAsStream("finals.data"), timeCalculator.ut1);

        if ("true".equalsIgnoreCase(System.getProperty(PROPERTY_KEY_FETCH_NEWEST_TIME_DATA))) {
            try {
                timeCalculator.updateTAI("ftp://maia.usno.navy.mil/ser7/leapsec.dat");
                timeCalculator.updateUT1("ftp://maia.usno.navy.mil/ser7/finals.data");
            } catch (IOException ignored) {
                // todo - warning
            }
        }

        return timeCalculator;
    }

    private static void readTAI(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
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
                    throw new IOException("An error occurred while parsing the TAI-UTC data.", e);
                }

                map.putIfAbsent(jd - 2400000.5, ls);
            }
        } finally {
            scanner.close();
        }
    }

    private static void readUT1(InputStream is, ConcurrentMap<Double, Double> map) throws IOException {
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
                    throw new IOException("An error occurred while parsing the UT1-UTC data.", e);
                }

                map.put(mjd, utd);
            }
        } finally {
            scanner.close();
        }
    }
}
