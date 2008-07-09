package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;

/**
 * Band filter interface.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface BandFilter {

    /**
     * Returns {@code true} if the band supplied as argument has been accepted.
     *
     * @param band the band.
     *
     * @return {@code true} if the band supplied as argument has been accepted.
     */
    boolean accept(Band band);
}
