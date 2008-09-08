package org.esa.beam.chris.operators;

/**
 * Interface for accessing a set of countably many pixels.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
interface PixelAccessor {

    void getPixel(int index, double[] samples);

    int getPixelCount();

    int getSampleCount();
}
