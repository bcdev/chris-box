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

/**
 * Creates a {@link Resampler}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class ResamplerFactory {

    private final double[] sourceWavelengths;
    private final double[] targetWavelengths;
    private final double[] targetBandwidths;

    ResamplerFactory(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths) {
        this.sourceWavelengths = sourceWavelengths;
        this.targetWavelengths = targetWavelengths;
        this.targetBandwidths = targetBandwidths;
    }

    Resampler createResampler(double wavlengthShift) {
        return new Resampler(sourceWavelengths, targetWavelengths, targetBandwidths, wavlengthShift);
    }
}
