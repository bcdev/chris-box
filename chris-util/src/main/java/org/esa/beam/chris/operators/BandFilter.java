package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision: 1864 $ $Date: 2008-03-07 15:16:02 +0100 (Fri, 07 Mar 2008) $
 */
public interface BandFilter {

    boolean accept(Band band);
}
