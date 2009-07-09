package org.esa.beam.chris.operators;

import sun.net.www.protocol.ftp.FtpURLConnection;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Utility class for converting between several time systems.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-Box 1.1
 */
public class TimeConverter {

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
    /**
     * The epoch (days) for the Julian Date (JD) which
     * corresponds to 4713-01-01 12:00 BC.
     */
    public static final double EPOCH_JD = -2440587.5;
    /**
     * The epoch (days) for the Modified Julian Date (MJD) which
     * corresponds to 1858-11-17 00:00.
     */
    public static final double EPOCH_MJD = -40587.0;
    /**
     * The epoch (millis) for the Modified Julian Date (MJD) which
     * corresponds to 1858-11-17 00:00.
     */
    public static final long EPOCH_MJD_MILLIS = -3506716800000L;
    /**
     * The number of days between {@link #EPOCH_MJD} and {@link #EPOCH_JD}.
     */
    public static final double MJD_TO_JD_OFFSET = EPOCH_MJD - EPOCH_JD; // 2400000.5;
    /**
     * The number of milli-seconds per day.
     */
    public static final double MILLIS_PER_DAY = 86400000.0;
    /**
     * The number of seconds per day.
     */
    public static final double SECONDS_PER_DAY = 86400.0;

    private static volatile TimeConverter uniqueInstance;

    private TimeConverter() {
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
    public static TimeConverter getInstance() throws IOException {
        if (uniqueInstance == null) {
            synchronized (TimeConverter.class) {
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
                    MessageFormat.format("No TAI-UTC data available before {0}.", mjdToDate(tai.firstKey())));
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
                    MessageFormat.format("No UT1-UTC data available before {0}.", mjdToDate(ut1.firstKey())));
        }
        if (mjd > ut1.lastKey()) {
            throw new IllegalArgumentException(
                    MessageFormat.format("No UT1-UTC data available after {0}.", mjdToDate(ut1.lastKey())));
        }

        return interpolate(mjd, ut1.floorEntry(mjd), ut1.ceilingEntry(mjd));
    }

    public long lastModified() {
        final File file = getFile("finals.data");
        if (file != null) {
            return file.lastModified();
        }
        return 0L;
    }

    public void updateTimeTables() throws IOException {
        synchronized (this) {
            this.updateTAI("ftp://maia.usno.navy.mil/ser7/leapsec.dat");
            this.updateUT1("ftp://maia.usno.navy.mil/ser7/finals.data");
        }
    }

    private static TimeConverter createInstance() throws IOException {
        final TimeConverter timeConverter = new TimeConverter();

        readTAI("leapsec.dat", timeConverter.tai);
        readUT1("finals.data", timeConverter.ut1);

        return timeConverter;
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
     * Updates the internal TAI-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the TAI-UTC data from the input stream.
     */
    private void updateTAI(InputStream is) throws IOException {
        readTAI(is, tai, true);
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

    /**
     * Updates the internal UT1-UTC table with newer data read from an input stream.
     *
     * @param is the input stream.
     *
     * @throws IOException if an error occurred while reading the UT1-UTC data from the input stream.
     */
    private void updateUT1(InputStream is) throws IOException {
        readUT1(is, ut1, true);
    }

    private static void readTAI(String name, ConcurrentMap<Double, Double> map) throws IOException {
        final File finalsFile = getFile(name);
        if (finalsFile == null) {
            readTAI(TimeConverter.class.getResourceAsStream(name), map, false);
        } else {
            readTAI(new BufferedInputStream(new FileInputStream(finalsFile)), map, false);
        }
    }

    private static void readTAI(InputStream is, ConcurrentMap<Double, Double> map, boolean write) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        final ArrayList<String> lineList = new ArrayList<String>();
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                lineList.add(line);

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

        if (write) {
            writeFile("leapsec.dat", lineList);
        }
    }

    private static void readUT1(String name, ConcurrentMap<Double, Double> map) throws IOException {
        final File finalsFile = getFile(name);
        if (finalsFile == null) {
            readUT1(TimeConverter.class.getResourceAsStream(name), map, false);
        } else {
            readUT1(new BufferedInputStream(new FileInputStream(finalsFile)), map, false);
        }
    }

    private static void readUT1(InputStream is, ConcurrentMap<Double, Double> map, boolean write) throws IOException {
        final Scanner scanner = new Scanner(is, "US-ASCII");
        scanner.useLocale(Locale.US);

        final ArrayList<String> lineList = new ArrayList<String>();
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                lineList.add(line);

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

        if (write) {
            writeFile("finals.data", lineList);
        }
    }

    private static Writer getWriter(String fileName) {
        final File userDir = getUserDir(".beam", "chris-geometric-correction", "auxdata");
        if (userDir != null) {
            userDir.mkdirs();
            final File file = new File(userDir, fileName);
            try {
                final OutputStream os = new FileOutputStream(file);
                return new BufferedWriter(new OutputStreamWriter(os, "US-ASCII"));
            } catch (FileNotFoundException e) {
                // ignore
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }

        return null;
    }

    private static void writeFile(String fileName, ArrayList<String> lineList) throws IOException {
        final Writer writer = getWriter(fileName);

        if (writer != null) {
            try {
                for (final String line : lineList) {
                    writer.write(line);
                    writer.write("\n");
                }
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static File getFile(String fileName) {
        final File timeFileDir = getUserDir(".beam", "chris-geometric-correction", "auxdata");

        if (timeFileDir != null) {
            final File file = new File(timeFileDir, fileName);
            if (file.canRead()) {
                return file;
            }
        }

        return null;
    }

    private static File getUserDir(String... children) {
        File dir = new File(System.getProperty("user.home"));

        for (final String child : children) {
            dir = new File(dir, child);
        }

        return dir;
    }

    private static double interpolate(double mjd, Map.Entry<Double, Double> floor, Map.Entry<Double, Double> ceiling) {
        final double floorKey = floor.getKey();
        final double floorValue = floor.getValue();
        final double ceilingKey = ceiling.getKey();

        if (floorKey == ceilingKey) {
            return floorValue;
        }

        return floorValue + (ceiling.getValue() - floorValue) * ((mjd - floorKey) / (ceilingKey - floorKey));
    }

    /**
     * Returns the Julian Date (JD) corresponding to a date.
     *
     * @param date the date.
     *
     * @return the JD corresponding to the date.
     */
    public static double dateToJD(Date date) {
        return date.getTime() / MILLIS_PER_DAY - EPOCH_JD;
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a date.
     *
     * @param date the date.
     *
     * @return the MJD corresponding to the date.
     */
    public static double dateToMJD(Date date) {
        return date.getTime() / MILLIS_PER_DAY - EPOCH_MJD;
    }

    /**
     * Returns the date corresponding to a Modified Julian Date (MJD).
     *
     * @param mjd the MJD.
     *
     * @return the date corresponding to the MJD.
     */
    public static Date mjdToDate(double mjd) {
        return new Date(Math.round((EPOCH_MJD + mjd) * MILLIS_PER_DAY));
    }

    /**
     * Converts UT1 into Greenwich Mean Sidereal Time (GST, IAU 1982 model).
     * <p/>
     * Note that the unit of GST is radian (rad).
     *
     * @param mjd the UT1 expressed as Modified Julian Date (MJD).
     *
     * @return the GST corresponding to the MJD given.
     */
    public static double mjdToGST(double mjd) {
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
     * Returns the date corresponding to a Julian Date (JD).
     *
     * @param jd the JD.
     *
     * @return the date corresponding to the JD.
     */
    public static Date jdToDate(double jd) {
        return new Date(Math.round((EPOCH_JD + jd) * MILLIS_PER_DAY));
    }

    /**
     * Returns the Julian Date (JD) corresponding to a Modified Julian Date (JD).
     *
     * @param mjd the MJD.
     *
     * @return the JD corresponding to the MJD.
     */
    public static double mjdToJD(double mjd) {
        return mjd + MJD_TO_JD_OFFSET;
    }

    /**
     * Converts UT1 into Greenwich Mean Sidereal Time (GST, IAU 1982 model).
     * <p/>
     * Note that the unit of GST is radian (rad).
     *
     * @param jd the UT1 expressed as Julian Date (JD).
     *
     * @return the GST corresponding to the JD given.
     */
    public static double jdToGST(double jd) {
        return mjdToGST(jdToMJD(jd));
    }

    /**
     * Returns the Modified Julian Date (MJD) corresponding to a Julian Date (JD).
     *
     * @param jd the JD.
     *
     * @return the MJD corresponding to the JD.
     */
    public static double jdToMJD(double jd) {
        return jd - MJD_TO_JD_OFFSET;
    }

    /**
     * Returns the Julian Date (JD) for the given parameters.
     *
     * @param year       the year.
     * @param month      the month (zero-based, e.g. use 0 for January and 11 for December).
     * @param dayOfMonth the day-of-month.
     *
     * @return the Julian Date.
     */
    public static double julianDate(int year, int month, int dayOfMonth) {
        return julianDate(year, month, dayOfMonth, 0, 0, 0);
    }

    /**
     * Calculates the Julian Date (JD) from the given parameter.
     *
     * @param year       the year.
     * @param month      the month (zero-based, e.g. use 0 for January and 11 for December).
     * @param dayOfMonth the day-of-month.
     * @param hourOfDay  the hour-of-day.
     * @param minute     the minute.
     * @param second     the second.
     *
     * @return the Julian Date.
     */
    public static double julianDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
        final GregorianCalendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(year, month, dayOfMonth, hourOfDay, minute, second);
        utc.set(Calendar.MILLISECOND, 0);

        return utc.getTimeInMillis() / MILLIS_PER_DAY - EPOCH_JD;
    }
}
