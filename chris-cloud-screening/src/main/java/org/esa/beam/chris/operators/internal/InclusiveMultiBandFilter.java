package org.esa.beam.chris.operators.internal;

import org.esa.beam.chris.operators.BandFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class InclusiveMultiBandFilter implements BandFilter {

    private final double[][] wavelengthIntervals;

    public InclusiveMultiBandFilter(double[]... wavelengthIntervals) {
        this.wavelengthIntervals = wavelengthIntervals;
    }

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
