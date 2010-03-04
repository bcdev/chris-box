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

/**
 * CHRIS Constants, some differ for each mode
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 */
class ModeCharacteristics {

    // frame transfer time (s) per line line
    private static final double FTT = 1.2096E-03;

    private static final ModeCharacteristics MODE_0 =
            new ModeCharacteristics(744, 1010, 11.4912E-03, Math.toRadians(1.28570));
    private static final ModeCharacteristics MODE_1 =
            new ModeCharacteristics(372, 374, 24.1899E-03, Math.toRadians(1.29261));
    private static final ModeCharacteristics MODE_234 =
            new ModeCharacteristics(744, 748, 11.4912E-03, Math.toRadians(1.28570));
    private static final ModeCharacteristics MODE_5 =
            new ModeCharacteristics(370, 748, 11.4912E-03, Math.toRadians(0.63939));
    private static final ModeCharacteristics MODE_20 =
            new ModeCharacteristics(744, 1024, 11.4912E-03, Math.toRadians(1.28570));

    private final int colCount;
    private final int rowCount;
    private final double integrationTimePerLine;

    static ModeCharacteristics get(int mode) {
        switch (mode) {
        case 0:
            return MODE_0;
        case 1:
            return MODE_1;
        case 5:
            return MODE_5;
        case 20:
            return MODE_20;
        default:
            return MODE_234;
        }
    }

    private final double fov;

    private ModeCharacteristics(int colCount, int rowCount, double integrationTimePerLine, double fov) {
        this.colCount = colCount;
        this.rowCount = rowCount;
        this.integrationTimePerLine = integrationTimePerLine;
        this.fov = fov;
    }

    int getColCount() {
        return colCount;
    }

    int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the integration time per Line (s).
     *
     * @return the integration time per Line (s).
     */
    double getIntegrationTimePerLine() {
        return integrationTimePerLine;
    }

    double getTotalTimePerLine() {
        return integrationTimePerLine + FTT;
    }

    double getTimePerImage() {
        return rowCount * getTotalTimePerLine();
    }

    /**
     * Returns the field of view (rad).
     *
     * @return the field of view (rad).
     */
    double getFov() {
        return fov;
    }

    /**
     * Returns the instantaneous field of view (rad).
     *
     * @return the instantaneous field of view (rad).
     */
    double getIfov() {
        return fov / colCount;
    }
}
