package org.esa.beam.chris.operators.internal;

import org.esa.beam.chris.operators.BandFilter;
import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class InclusiveBandFilter implements BandFilter {

    private final double minWavelength;
    private final double maxWavelength;

    public InclusiveBandFilter(double minWavelength, double maxWavelength) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
    }

    public boolean accept(Band band) {
        final double wavelength = band.getSpectralWavelength();

        return wavelength > minWavelength && wavelength < maxWavelength;
    }
}
