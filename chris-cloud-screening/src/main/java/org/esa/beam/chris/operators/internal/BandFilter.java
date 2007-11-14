package org.esa.beam.chris.operators.internal;

import org.esa.beam.framework.datamodel.Band;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface BandFilter {

    boolean accept(Band band);
}
