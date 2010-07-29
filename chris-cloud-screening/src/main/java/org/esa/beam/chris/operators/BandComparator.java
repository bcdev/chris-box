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

import org.esa.beam.framework.datamodel.Band;

import java.util.Comparator;

/**
 * Helper class needed for sorting spectral {@link Band}s according to the
 * central wavelength.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class BandComparator implements Comparator<Band> {

    /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     * <p/>
     * Note: this comparator imposes orderings that are inconsistent with equals.
     *
     * @param b1 the first {@link Band} to be compared.
     * @param b2 the second {@link Band} to be compared.
     *
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    @Override
    public int compare(Band b1, Band b2) {
        return Float.compare(b1.getSpectralWavelength(), b2.getSpectralWavelength());
    }
}
