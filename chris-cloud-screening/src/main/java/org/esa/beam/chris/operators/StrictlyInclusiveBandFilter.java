package org.esa.beam.chris.operators;

import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class StrictlyInclusiveBandFilter implements BandFilter {

    private final double minWavelength;
    private final double maxWavelength;

    StrictlyInclusiveBandFilter(double minWavelength, double maxWavelength) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
    }

    @Override
    public boolean accept(Band band) {
        final double lowerBound = band.getSpectralWavelength() - 0.5 * band.getSpectralBandwidth();
        final double upperBound = band.getSpectralWavelength() + 0.5 * band.getSpectralBandwidth();

        return lowerBound > minWavelength && upperBound < maxWavelength;
    }
}
