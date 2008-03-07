package org.esa.beam.chris.operators.internal;

import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision: 1411 $ $Date$
 */
public class ExclusiveBandFilter implements BandFilter {

    private final double minWavelength;
    private final double maxWavelength;

    public ExclusiveBandFilter(double minWavelength, double maxWavelength) {
        this.minWavelength = minWavelength;
        this.maxWavelength = maxWavelength;
    }

    public boolean accept(Band band) {
        final double wavelength = band.getSpectralWavelength();

        return wavelength > minWavelength && wavelength < maxWavelength;
    }
}
