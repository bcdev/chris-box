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

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.dataio.chris.ChrisConstants;
import org.esa.beam.framework.datamodel.Product;

/**
 * Acquisition information.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class AcquisitionInfo {

    private static final int[] CHRONOLOGICAL_IMAGE_NUMBERS = new int[]{2, 1, 3, 0, 4};

    private final int mode;
    private final double targetAlt;
    private final double targetLat;
    private final double targetLon;
    private final int chronologicalImageNumber;

    public static AcquisitionInfo create(Product product) {
        final String modeString = OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
        final int mode;
        if ("3A".equals(modeString)) {
            mode = 3;
        } else {
            mode = Integer.parseInt(modeString);
        }
        final double alt = OpUtils.getAnnotationDouble(product, ChrisConstants.ATTR_NAME_TARGET_ALT) / 1000.0;
        final double lat = OpUtils.getAnnotationDouble(product, ChrisConstants.ATTR_NAME_TARGET_LAT);
        final double lon = OpUtils.getAnnotationDouble(product, ChrisConstants.ATTR_NAME_TARGET_LON);
        final int imageNumber = OpUtils.getAnnotationInt(product, ChrisConstants.ATTR_NAME_IMAGE_NUMBER, 0, 1);

        return new AcquisitionInfo(mode, lon, lat, alt, CHRONOLOGICAL_IMAGE_NUMBERS[imageNumber - 1]);
    }

    AcquisitionInfo(int mode, double targetLon, double targetLat, double targetAlt,
                    int chronologicalImageNumber) {
        this.mode = mode;
        this.targetAlt = targetAlt;
        this.targetLat = targetLat;
        this.targetLon = targetLon;
        this.chronologicalImageNumber = chronologicalImageNumber;
    }

    final int getMode() {
        return mode;
    }

    final double getTargetAlt() {
        return targetAlt;
    }

    final double getTargetLat() {
        return targetLat;
    }

    final double getTargetLon() {
        return targetLon;
    }

    final int getChronologicalImageNumber() {
        return chronologicalImageNumber;
    }

    final boolean isBackscanning() {
        return chronologicalImageNumber % 2 != 0;
    }
}
