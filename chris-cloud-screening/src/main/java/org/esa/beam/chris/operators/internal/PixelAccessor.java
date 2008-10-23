package org.esa.beam.chris.operators.internal;

/**
 * Interface for accessing a set of countably many pixels.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface PixelAccessor {

    void getPixel(int index, double[] samples);

    int getPixelCount();

    int getSampleCount();
}
