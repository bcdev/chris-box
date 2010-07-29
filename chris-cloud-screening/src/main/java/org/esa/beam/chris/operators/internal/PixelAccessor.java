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

package org.esa.beam.chris.operators.internal;

/**
 * Interface for accessing the samples of a set of countably
 * many pixels.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface PixelAccessor {

    /**
     * Returns the sum of a given array of samples and the
     * samples of the ith pixel.
     *
     * @param i       the pixel index.
     * @param samples the array of samples. On return contains
     *                the sum of  the original samples and the
     *                samples of the ith pixel.
     *
     * @return the sum of the original samples and the samples
     *         of the ith pixel.
     */
    double[] addSamples(int i, double[] samples);

    /**
     * Returns the samples of the ith pixel.
     *
     * @param i       the pixel index.
     * @param samples the samples of the ith pixel.
     *
     * @return the samples of the ith pixel.
     */
    double[] getSamples(int i, double[] samples);

    /**
     * Returns the number of accessible pixels.
     *
     * @return the number of accessible pixels.
     */
    int getPixelCount();

    /**
     * Returns the number of samples per pixel.
     *
     * @return the number of samples per pixel.
     */
    int getSampleCount();
}
