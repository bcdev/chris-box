package org.esa.beam.chris.util;

import org.esa.beam.framework.datamodel.Band;

/**
 * Band filter interface.
 *
 * @author Ralf Quast
 * @version $Revision: 2530 $ $Date: 2008-07-09 13:10:39 +0200 (Wed, 09 Jul 2008) $
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
