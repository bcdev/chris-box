package org.esa.beam.chris.operators;

/**
 * Resampler.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Resampler {

    private final int sourceWavelengthCount;
    private final int targetWavelengthCount;
    private final double[][] weights;

    public Resampler(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths) {
        sourceWavelengthCount = sourceWavelengths.length;
        targetWavelengthCount = targetWavelengths.length;

        weights = calculateResamplingWeights(sourceWavelengths, targetWavelengths, targetBandwidths,
                                             new double[targetWavelengthCount][sourceWavelengthCount]);
    }

    /**
     * Resamples the given spectral values.
     *
     * @param sourceValues the spectral values.
     *
     * @return the resampled values.
     */
    public double[] resample(double[] sourceValues) {
        return multiply(sourceValues, weights, new double[targetWavelengthCount]);
    }

    /**
     * Resamples the given spectral values.
     *
     * @param sourceValues the spectral values.
     * @param targetValues the resampled values (overwritten on output).
     *
     * @return the resampled values.
     */
    public double[] resample(double[] sourceValues, double[] targetValues) {
        if (sourceValues.length != sourceWavelengthCount) {
            throw new IllegalArgumentException("sourceValues.length != sourceWavelengthCount");
        }
        if (targetValues.length != targetWavelengthCount) {
            throw new IllegalArgumentException("targetValues.length != targetWavelengthCount");
        }

        return multiply(sourceValues, weights, targetValues);
    }

    /**
     * Resamples the given spectral values.
     *
     * @param sourceValues the spectral values.
     *
     * @return the resampled values.
     */
    public double[][] resample(double[][] sourceValues) {
        return resample(sourceValues, new double[targetWavelengthCount][sourceValues[0].length]);
    }

    /**
     * Resamples the given spectral values.
     *
     * @param sourceValues the spectral values.
     * @param targetValues the resampled values (overwritten on output).
     *
     * @return the resampled values.
     */
    public double[][] resample(double[][] sourceValues, double[][] targetValues) {
        if (sourceValues.length != sourceWavelengthCount) {
            throw new IllegalArgumentException("sourceValues.lenght != sourceWavelengthCount");
        }
        if (targetValues.length != targetWavelengthCount) {
            throw new IllegalArgumentException("targetValues.lenght != targetWavelengthCount");
        }

        return multiply(sourceValues, weights, targetValues);
    }

    /**
     * Multiplies the columns of a matrix A by the rows of a matrix B.  The matrix B must
     * have the same number of columns as A has rows. The resulting matrix C has the same
     * number of columns as A and the same number of rows as B.
     *
     * @param a the matrix A.
     * @param b the matrix B.
     * @param c the resulting matrix (overwritten on output).
     *
     * @return the resutlting matrix.
     */
    private static double[][] multiply(double[][] a, double[][] b, double[][] c) {
        for (int i = 0; i < b.length; ++i) {
            for (int k = 0; k < a[0].length; ++k) {
                double sum = 0.0;
                for (int l = 0; l < a.length; ++l) {
                    sum += a[l][k] * b[i][l];
                }
                c[i][k] = sum;
            }
        }

        return c;
    }

    /**
     * Multiplies a vector by the rows of a matrix. The resulting vector has the same
     * number of components as the matrix has rows.
     *
     * @param a the vector.
     * @param b the matrix.
     * @param c the resulting matrix (overwritten on output).
     *
     * @return the resutlting matrix.
     */
    private static double[] multiply(double[] a, double[][] b, double[] c) {
        for (int i = 0; i < b.length; ++i) {
            double sum = 0.0;
            for (int k = 0; k < a.length; ++k) {
                sum += a[k] * b[i][k];
            }
            c[i] = sum;
        }

        return c;
    }

    /**
     * Calculates the weights for resampling spectral quantities computed at
     * wavelengths {@code sourceWavelenghts}  to spectral bands with central
     * wavelenghts {@code targetWavelengths} and bandwidths
     * {@code targetBandwidths}.
     *
     * @param sourceWavelengths the source wavelenghts.
     * @param targetWavelengths the targetWavelengths.
     * @param targetBandwidths  the target bandwidths.
     * @param weights           the resampling weights (overwritten on output).
     *
     * @return the resampling weights.
     */
    private static double[][] calculateResamplingWeights(double[] sourceWavelengths, double[] targetWavelengths,
                                                         double[] targetBandwidths, double[][] weights) {
        final int sourceWavelengthCount = sourceWavelengths.length;
        final int targetWavelengthCount = targetWavelengths.length;

        final double max = 6.0;
        final double min = 2.0;

        final double[] e = new double[targetWavelengthCount];
        final double[] c = new double[targetWavelengthCount];

        for (int i = 0; i < targetWavelengthCount; ++i) {
            e[i] = max + ((min - max) * i) / (targetWavelengthCount - 1);
            c[i] = Math.pow(1.0 / (Math.pow(2.0, e[i]) * Math.log(2.0)), 1.0 / e[i]);
        }

        for (int i = 0; i < targetWavelengthCount; ++i) {
            for (int k = 0; k < sourceWavelengthCount; ++k) {
                final double delta = Math.abs(targetWavelengths[i] - sourceWavelengths[k]);
                if (delta <= 2.0 * targetBandwidths[i]) {
                    weights[i][k] = 1.0 / Math.exp(Math.pow(delta / (targetBandwidths[i] * c[i]), e[i]));
                }
            }
            // normalize weights
            double sum = 0.0;
            for (int j = 0; j < sourceWavelengthCount; ++j) {
                sum += weights[i][j];
            }
            if (sum > 0.0) {
                for (int j = 0; j < sourceWavelengthCount; ++j) {
                    weights[i][j] /= sum;
                }
            }
        }

        return weights;
    }
}
