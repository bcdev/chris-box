/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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

import com.bc.ceres.core.Assert;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class TelemetryFinder {

    static class Telemetry {

        private final File gpsFile;
        private final File ictFile;

        Telemetry(File gpsFile, File ictFile) {
            super();
            this.gpsFile = gpsFile;
            this.ictFile = ictFile;
        }

        public File getGpsFile() {
            return gpsFile;
        }

        public File getIctFile() {
            return ictFile;
        }
    }

    public static Telemetry findTelemetry(Product chrisProduct, File telemetryRepository) throws IOException {
        Assert.notNull(chrisProduct);

        final String imageDateWithDashes = OpUtils.getAnnotationString(chrisProduct,
                                                                       ChrisConstants.ATTR_NAME_IMAGE_DATE);
        final String imageDate = imageDateWithDashes.replace("-", "");
        final String targetName = OpUtils.getAnnotationString(chrisProduct, ChrisConstants.ATTR_NAME_TARGET_NAME);

        final File productDir = chrisProduct.getFileLocation().getParentFile();
        final Pattern ictPattern = createIctPattern(imageDate, targetName);
        final File ictFile = findTelemetryFile(productDir, telemetryRepository, new PatternFilenameFilter(ictPattern));
        final String reference = getReferenceString(ictPattern, ictFile.getName());

        final Pattern gpsPattern = createGpsPattern(reference);
        final File gpsFile = findTelemetryFile(productDir, telemetryRepository, new PatternFilenameFilter(gpsPattern));

        return new Telemetry(gpsFile, ictFile);
    }

    private static Pattern createGpsPattern(String reference) {
        return Pattern.compile("CHRIS_" + reference + "_\\d+_PROBA1_GPS_Data");
    }

    private static Pattern createIctPattern(String imageDate, String targetName) {
        return Pattern.compile(".*\\." + targetName + "_(\\d+)_CHRIS_center_times_" + imageDate + "_.*");
    }

    private static File findTelemetryFile(File productDir, File repositoryDir,
                                          PatternFilenameFilter filter) throws IOException {
        File[] files = new File[0];
        if (productDir != null) {
            files = productDir.listFiles(filter);
        }
        if (files.length == 0) {
            if (repositoryDir != null) {
                if (!repositoryDir.exists()) {
                    throw new IOException("Directory '" + repositoryDir.getPath() + "' does not exist.");
                }
                if (!repositoryDir.canRead()) {
                    throw new IOException("Cannot read from directory '" + repositoryDir.getPath() + "'.");
                }
                files = findTelemetryFiles(repositoryDir, filter);
            }
        }
        if (files.length == 0) {
            throw new FileNotFoundException(MessageFormat.format(
                    "Cannot find telemetry file matching pattern ''{0}''.", filter.getPattern()));
        }
        return files[0];
    }

    private static File[] findTelemetryFiles(File dir, FilenameFilter filter) {
        File[] telemetryFiles = dir.listFiles(filter);

        if (telemetryFiles.length != 0) {
            return telemetryFiles;
        }
        for (final File file : dir.listFiles()) {
            if (file.isDirectory() && file.canRead()) {
                telemetryFiles = findTelemetryFiles(file, filter);
                if (telemetryFiles.length != 0) {
                    return telemetryFiles;
                }
            }
        }
        
        return telemetryFiles;
    }

    private static String getReferenceString(Pattern ictPattern, String ictFileName) {
        final Matcher matcher = ictPattern.matcher(ictFileName);
        matcher.matches();

        return matcher.group(1);
    }

    private static class PatternFilenameFilter implements FilenameFilter {

        private final Pattern pattern;

        public PatternFilenameFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean accept(File dir, String name) {
            return pattern.matcher(name).matches();
        }

        public String getPattern() {
            return pattern.pattern();
        }
    }

}
