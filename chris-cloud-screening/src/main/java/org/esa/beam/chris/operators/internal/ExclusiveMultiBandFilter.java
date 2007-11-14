package org.esa.beam.chris.operators.internal;

import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ExclusiveMultiBandFilter implements BandFilter {

    private final double[][] wavelengthIntervals;

    public ExclusiveMultiBandFilter(double[][] wavelengthIntervals) {
        this.wavelengthIntervals = wavelengthIntervals;
    }

    public boolean accept(Band band) {
        final double lowerBound = band.getSpectralWavelength() - 0.5 * band.getSpectralBandwidth();
        final double upperBound = band.getSpectralWavelength() + 0.5 * band.getSpectralBandwidth();

        for (final double[] interval : wavelengthIntervals) {
            if (lowerBound > interval[0] && upperBound < interval[1]) {
                return true;
            }
        }
        return false;
    }
}
