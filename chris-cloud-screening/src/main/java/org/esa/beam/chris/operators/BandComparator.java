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
