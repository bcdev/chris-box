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
