package org.esa.beam.chris.operators.internal;

import org.esa.beam.chris.operators.BandFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class StrictlyInclusiveBandFilter implements BandFilter {

    private final double minWavelength;
    private final double maxWavelength;

    public StrictlyInclusiveBandFilter(double minWavelength, double maxWavelength) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
    }

    public boolean accept(Band band) {
        final double lowerBound = band.getSpectralWavelength() - 0.5 * band.getSpectralBandwidth();
        final double upperBound = band.getSpectralWavelength() + 0.5 * band.getSpectralBandwidth();

        return lowerBound > minWavelength && upperBound < maxWavelength;
    }
}
