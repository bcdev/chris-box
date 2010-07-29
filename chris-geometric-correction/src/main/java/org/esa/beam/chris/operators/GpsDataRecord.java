/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.operators;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles GPS time reading and calculation.
 *
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class GpsDataRecord {

    private static final double GPS_JD_OFFSET = TimeConverter.julianDate(1980, 0, 6);

    final double posX;
    final double posY;
    final double posZ;
    final double velX;
    final double velY;
    final double velZ;
    final double secs;
    final double jd;

    GpsDataRecord(double posX, double posY, double posZ, double velX, double velY, double velZ, double secs,
                  double jd) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.secs = secs;
        this.jd = jd;
    }

    static List<GpsDataRecord> create(List<String[]> records, double dTgps, double delay) throws IOException {
        List<GpsDataRecord> gpsDataRecords = new ArrayList<GpsDataRecord>(records.size());
        for (String[] record : records) {
            double posX = getDouble(record, GPS.POSX);
            double posY = getDouble(record, GPS.POSY);
            double posZ = getDouble(record, GPS.POSZ);
            double velX = getDouble(record, GPS.VELOX);
            double velY = getDouble(record, GPS.VELOY);
            double velZ = getDouble(record, GPS.VELOZ);

            double gpsSec = getDouble(record, GPS.SECONDS) - delay - dTgps;
            int gpsWeek = getInt(record, GPS.WEEK);
            double jd = gpsTimeToJD(gpsWeek, gpsSec);

            GpsDataRecord gpstime = new GpsDataRecord(posX, posY, posZ, velX, velY, velZ, gpsSec, jd);
            gpsDataRecords.add(gpstime);
        }
        return gpsDataRecords;

    }

    private static double gpsTimeToJD(int gpsWeek, double gpsSec) throws IOException {
        double gpsDays = gpsSec / TimeConverter.SECONDS_PER_DAY;
        final double utcJD = GPS_JD_OFFSET + gpsWeek * 7 + gpsDays;
        return getUT1(utcJD);
    }

    private static double getDouble(String[] record, GPS index) {
        return Double.parseDouble(record[index.index]);
    }

    private static int getInt(String[] record, GPS index) {
        return Integer.parseInt(record[index.index]);
    }

    static class GpsDataReader {

        private final List<String[]> records;

        GpsDataReader(InputStream is) {
            Reader reader = new InputStreamReader(is);
            char[] separators = new char[]{'\t'};
            CsvReader csvReader = new CsvReader(reader, separators, true, "TIME");
            List<String[]> readRecords = null;
            try {
                readRecords = readRecords(csvReader);
            } catch (IOException e) {
                try {
                    csvReader.close();
                } catch (IOException e1) {
                    // ignore
                }
            }
            records = readRecords;
        }

        private List<String[]> readRecords(CsvReader csvReader) throws IOException {
            List<String[]> readStringRecords = csvReader.readStringRecords();
            List<String[]> cleanedRecords = new ArrayList<String[]>(readStringRecords.size());
            String lastSecondsEntry = "";
            for (String[] strings : readStringRecords) {
                String secondsEntry = strings[GPS.SECONDS.index];
                if (!secondsEntry.equals(lastSecondsEntry)) {
                    cleanedRecords.add(strings);
                    lastSecondsEntry = secondsEntry;
                }
            }
            return cleanedRecords;
        }

        List<String[]> getReadRecords() {
            return records;
        }
    }

    private enum GPS {

        TIME(0),
        PKT(1),
        POSX_PKT(2),
        POSX(3),
        POSY_PKT(4),
        POSY(5),
        POSZ_PKT(6),
        POSZ(7),
        WEEK_PKT(8),
        WEEK(9),
        SECONDS_PKT(10),
        SECONDS(11),
        VELOX_PKT(12),
        VELOX(13),
        VELOY_PKT(14),
        VELOY(15),
        VELOZ_PKT(16),
        VELOZ(17);

        final int index;

        private GPS(int index) {
            this.index = index;
        }
    }

    private static double getUT1(double jd) throws IOException {
        return jd + TimeConverter.getInstance().deltaUT1(TimeConverter.jdToMJD(jd)) / TimeConverter.SECONDS_PER_DAY;
    }

}
