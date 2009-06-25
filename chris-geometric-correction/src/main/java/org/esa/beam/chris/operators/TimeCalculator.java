package org.esa.beam.chris.operators;

import sun.net.www.protocol.ftp.FtpURLConnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Date;
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

    /**
     * The epoch (millis) for the Modified Julian Date (MJD) which
     * corresponds to 1858-11-17 00:00.
     */
    private static final long EPOCH_MJD = -3506716800000L;

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
     * Converts UT1 into Greenwich Mean Sidereal Time (GST, IAU 1982 model).
     *
     * @param mjd the UT1 expressed as Modified Julian Date (MJD).
     *
     * @return the GST corresponding to the MJD given.
     */
    public static double toGST(double mjd) {
        // radians per sidereal second
        final double secRad = 7.272205216643039903848712E-5;

        // seconds per day, days per Julian century
        final double daySec = 86400.0;
        final double cenDay = 36525.0;

        // reference epoch (J2000)
        final double mjd0 = 51544.5;

        // coefficients of IAU 1982 GMST-UT1 model
        final double a = 24110.54841;
        final double b = 8640184.812866;
        final double c = 0.093104;
        final double d = 6.2E-6;

        final double mjd1 = Math.floor(mjd);
        final double mjd2 = mjd - mjd1;

        // Julian centuries since epoch
        final double t = (mjd2 + (mjd1 - mjd0)) / cenDay;
        // fractional part of MJD(UT1) in seconds
        final double f = daySec * mjd2;

        final double twoPi = 2.0 * Math.PI;
        final double gst = (secRad * ((a + (b + (c - d * t) * t) * t) + f)) % twoPi;

        if (gst < 0.0) {
            return gst + twoPi;
        }

        return gst;
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a date.
     *
     * @param date the date.
     *
     * @return the MJD corresponding to the date.
     */
    public static double toMJD(Date date) {
        return (date.getTime() - EPOCH_MJD) / 86400000.0;
    }

    /**
     * Returns the date corresponding to a Modified Julian Date (MJD).
     *
     * @param mjd the MJD.
     *
     * @return the date corresponding to the MJD.
     */
    static Date toDate(double mjd) {
        return new Date(EPOCH_MJD + (long) (mjd * 86400000.0));
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
                    MessageFormat.format("No TAI-UTC data available before {0}.", toDate(tai.firstKey())));
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
                    MessageFormat.format("No UT1-UTC data available before {0}.", toDate(ut1.firstKey())));
        }
        if (mjd > ut1.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available after {0}.", toDate(ut1.lastKey())));
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
