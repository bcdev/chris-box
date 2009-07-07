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

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class AuxDataFinder {

    static class TelemetryFiles {
        final File gpsFile;
        final File telemetryFile;
        
        TelemetryFiles(File gpsFile, File telemetryFile) {
            super();
            this.gpsFile = gpsFile;
            this.telemetryFile = telemetryFile;
        }
        
        
    }

    public static TelemetryFiles findTelemetryFiles(Product chrisProduct, File telemetryArchiveBase, File gpsArchiveBase) {
        String imageDateWithDashes = OpUtils.getAnnotationString(chrisProduct, ChrisConstants.ATTR_NAME_IMAGE_DATE);
        String imageDate = imageDateWithDashes.replace("-", "");
        String targetName = OpUtils.getAnnotationString(chrisProduct, ChrisConstants.ATTR_NAME_TARGET_NAME);
        
        Pattern telemetryPattern = Pattern.compile(".*\\."+targetName+"_(\\d+)_CHRIS_center_times_"+imageDate+"_.*");
        File[] telemetryFiles = telemetryArchiveBase.listFiles(new PatternFilenameFilter(telemetryPattern));
        if (telemetryFiles.length != 1) {
            return null;
        }
        
        String reference = getGroup(telemetryPattern, telemetryFiles[0].getName());
        
        Pattern gpsPattern = Pattern.compile("CHRIS_"+reference+"_\\d+_PROBA1_GPS_Data");
        File[] gpsFiles = gpsArchiveBase.listFiles(new PatternFilenameFilter(gpsPattern));
        if (gpsFiles.length != 1) {
            return new TelemetryFiles(null, telemetryFiles[0]);
        }
        return new TelemetryFiles(gpsFiles[0], telemetryFiles[0]);
    }
    
    private static String getGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
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
        
    }
}
