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
    private static final long EPOCH_MJD = -3506716800000L;
    private static final AtomicReference<TimeCalculator> instance = new AtomicReference<TimeCalculator>();

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

    private volatile double timeStamp;

    private TimeCalculator() {
        tai = new ConcurrentSkipListMap<Double, Double>();
        ut1 = new ConcurrentSkipListMap<Double, Double>();
        timeStamp = toMJD(new Date());
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

    static double toGST(double mjd) {
        final double jdn = toJDN(mjd);
        final double TU = (jdn - 2451545.0) / 36525.0;
        final double gst0 = 24110.54841 + TU * (8640184.812866D + TU * (0.093104 - TU * 6.2 - 6.0));
        final double dt = mjd - jdn;
        final double gst = (gst0 + 1.00273790934 * dt) % 86400;

        return (gst * Math.PI / 43200.0) % (2.0 * Math.PI);
    }

    public static double toJD(double mjd) {
        return mjd + 2400000.5;
    }

    public static double toJDN(double mjd) {
        return Math.floor(toJD(mjd));
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

        try {
            if (timeCalculator.timeStamp + 182.0 > timeCalculator.ut1.lastKey()) {
                timeCalculator.updateTAI("ftp://maia.usno.navy.mil/ser7/leapsec.dat");
                timeCalculator.updateUT1("ftp://maia.usno.navy.mil/ser7/finals.data");
            }
        } catch (IOException ignored) {
            // todo - warning
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
