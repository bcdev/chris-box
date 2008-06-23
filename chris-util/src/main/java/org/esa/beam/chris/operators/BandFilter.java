package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;

/**
 * Band filter interface.
 *
 * @author Ralf Quast
 * @version $Revision: 1864 $ $Date: 2008-03-07 15:16:02 +0100 (Fri, 07 Mar 2008) $
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
