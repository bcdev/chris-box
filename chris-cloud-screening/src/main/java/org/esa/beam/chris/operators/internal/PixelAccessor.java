package org.esa.beam.chris.operators.internal;

/**
 * Interface for accessing the samples of a set of countably
 * many pixels.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public interface PixelAccessor {

    /**
     * Returns the sum of a given array of samples and the
     * samples of the ith pixel.
     *
     * @param i       the pixel index.
     * @param samples the array of samples. On return contains
     *                the sum of  the original samples and the
     *                samples of the ith pixel.
     *
     * @return the sum of the original samples and the samples
     *         of the ith pixel.
     */
    double[] addSamples(int i, double[] samples);

    /**
     * Returns the samples of the ith pixel.
     *
     * @param i       the pixel index.
     * @param samples the samples of the ith pixel.
     *
     * @return the samples of the ith pixel.
     */
    double[] getSamples(int i, double[] samples);

    /**
     * Returns the number of accessible pixels.
     *
     * @return the number of accessible pixels.
     */
    int getPixelCount();

    /**
     * Returns the number of samples per pixel.
     *
     * @return the number of samples per pixel.
     */
    int getSampleCount();
}
