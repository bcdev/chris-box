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

import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class InclusiveMultiBandFilter implements BandFilter {

    private final double[][] wavelengthIntervals;

    InclusiveMultiBandFilter(double[]... wavelengthIntervals) {
        this.wavelengthIntervals = wavelengthIntervals;
    }

    @Override
    public boolean accept(Band band) {
        final float wavelength = band.getSpectralWavelength();

        for (final double[] interval : wavelengthIntervals) {
            if (wavelength > interval[0] && wavelength < interval[1]) {
                return true;
            }
        }
        return false;
    }
}
