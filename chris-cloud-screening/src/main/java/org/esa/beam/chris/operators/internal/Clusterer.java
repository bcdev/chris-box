/*
 * Copyright (C) 2002-2007 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators.internal;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization cluster algorithm.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class Clusterer {

    private final int clusterCount;
    private final int pointCount;
    private final int dimensionCount;

    private final MultinormalDistribution[] dists;
    private final double[][] points;
    private final double[][] h;
    private final double[] mixtureCoefficients;

    private double[][] means;
    private double[][][] covariances;

    private final IndexedDouble[] sortedMixtureCoefficients;

    /**
     * Constructs a new instance of this class.
     *
     * @param points         the points being analyzed.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of iterations being performed.
     */
    public Clusterer(double[][] points, int clusterCount, int iterationCount) {
        this(points, points.length, points[0].length, clusterCount, iterationCount, 5489);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points         the points being analyzed.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of iterations being performed.
     * @param seed           the seed used for the random initialization of clusters.
     */
    public Clusterer(double[][] points, int clusterCount, int iterationCount, int seed) {
        this(points, points.length, points[0].length, clusterCount, iterationCount, seed);
    }

    private Clusterer(double[][] points, int pointCount, int dimensionCount, int clusterCount,
                      int iterationCount, int seed) {
        this.points = points;
        this.pointCount = pointCount;
        this.dimensionCount = dimensionCount;
        this.clusterCount = clusterCount;

        dists = new MultinormalDistribution[clusterCount];
        h = new double[clusterCount][pointCount];
        mixtureCoefficients = new double[clusterCount];

        initialize(seed, 7);

        do {
            System.out.println("iterationCount = " + iterationCount);
            for (int k = 0; k < clusterCount; ++k) {
                System.out.println("class probability of cluster " + k + ": " + mixtureCoefficients[k]);
            }

            estimate();
            maximize();
        } while (iterationCount-- > 0);

        sortedMixtureCoefficients = IndexedDouble.createArray(mixtureCoefficients);
        Arrays.sort(sortedMixtureCoefficients, new Lt<IndexedDouble>());

        for (int k = 0; k < clusterCount; ++k) {
            System.out.println("class probability of cluster " + k + ": " + getMixtureCoefficient(k));
        }
    }

    /**
     * Returns the kth cluster distribution.
     *
     * @param k the cluster index.
     *
     * @return the kth cluster distribution.
     */
    public MultinormalDistribution getClusterDistribution(int k) {
        return dists[actualIndex(k)];
    }

    /**
     * Returns the posterior (or membership) probabilities for the kth cluster.
     *
     * @param k the cluster index.
     *
     * @return the posterior probabilities for the kth cluster.
     */
    public double[] getPosteriorProbabilities(int k) {
        return h[actualIndex(k)];
    }

    /**
     * Returns the mixture coefficient of the kth cluster.
     *
     * @param k the cluster index.
     *
     * @return the mixture coefficient of the kth cluster.
     */
    public double getMixtureCoefficient(int k) {
        return mixtureCoefficients[actualIndex(k)];
    }

    /**
     * Creates a membership mask.
     *
     * @return the membership mask created.
     */
    public int[] createMembershipMask() {
        final int[] mask = new int[pointCount];

        for (int i = 0; i < pointCount; ++i) {
            final double[] d = new double[clusterCount];
            for (int k = 0; k < clusterCount; ++k) {
                d[k] = h[k][i];
            }
            mask[i] = actualIndex(indexMax(d));
        }

        return mask;
    }

    /**
     * Returns the actual index of for a given cluster index.
     *
     * @param k the cluster index.
     *
     * @return the actual index.
     */
    private int actualIndex(int k) {
        return sortedMixtureCoefficients[k].index;
    }

    /**
     * Random initialization with the k-means method.
     *
     * @param seed           the seed value used for initializing the random number generator.
     * @param iterationCount the number of k-means iterations carried out.
     */
    private void initialize(int seed, int iterationCount) {
        final Random random = new Random(seed);

        // k-means initialization
        means = new double[clusterCount][dimensionCount];
        for (int k = 0; k < clusterCount; ++k) {
            boolean duplicated = false;
            do {
                means[k] = points[random.nextInt(pointCount)];
                for (int i = 0; i < k; ++i) {
                    duplicated = means[k] == means[i];
                    if (duplicated) {
                        break;
                    }
                }
            } while (duplicated);
        }
        // k-means iteration
        final int[] mask = new int[pointCount];
        do {
            // create membership mask
            for (int i = 0; i < pointCount; ++i) {
                final double[] d = new double[clusterCount];
                for (int k = 0; k < clusterCount; ++k) {
                    d[k] = euclideanDistanceSquared(points[i], means[k]);
                }
                mask[i] = indexMin(d);
            }
            // calculate mean
            means = new double[clusterCount][dimensionCount];
            for (int k = 0; k < clusterCount; ++k) {
                int count = 0;
                for (int i = 0; i < pointCount; ++i) {
                    if (mask[i] == k) {
                        for (int l = 0; l < dimensionCount; ++l) {
                            means[k][l] += points[i][l];
                        }
                        ++count;
                    }
                }
                for (int l = 0; l < dimensionCount; ++l) {
                    means[k][l] /= count;
                }
            }
        } while (iterationCount-- > 0);
        // calculate covariances
        covariances = new double[clusterCount][dimensionCount][dimensionCount];
        for (int k = 0; k < clusterCount; ++k) {
            int count = 0;
            for (int i = 0; i < pointCount; ++i) {
                if (mask[i] == k) {
                    for (int l = 0; l < dimensionCount; ++l) {
                        for (int m = l; m < dimensionCount; ++m) {
                            covariances[k][l][m] += (points[i][l] - means[k][l]) * (points[i][m] - means[k][m]);
                        }
                    }
                    ++count;
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                for (int m = 0; m < dimensionCount; ++m) {
                    covariances[k][l][m] /= count;
                }
            }
        }
        // initialize prior probabilities
        for (int k = 0; k < clusterCount; ++k) {
            Arrays.fill(h[k], 1.0);
        }
    }

    /**
     * E-step.
     */
    private void estimate() {
        // update cluster distributions
        for (int k = 0; k < clusterCount; ++k) {
            dists[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
        // calculate posterior probabilities
        final double[] sums = new double[pointCount];
        for (int k = 0; k < clusterCount; ++k) {
            for (int i = 0; i < pointCount; ++i) {
                h[k][i] *= dists[k].probabilityDensity(points[i]);
                sums[i] += h[k][i];
            }
        }
        for (int i = 0; i < pointCount; ++i) {
            if (sums[i] > 0.0) {
                for (int k = 0; k < clusterCount; ++k) {
                    h[k][i] /= sums[i];
                }
            }
        }
    }

    /**
     * M-step.
     */
    private void maximize() {
        // calculate sums
        final double[] sums = new double[clusterCount];
        for (int k = 0; k < clusterCount; ++k) {
            for (int i = 0; i < pointCount; ++i) {
                sums[k] += h[k][i];
            }
        }
        // calculate mean
        means = new double[clusterCount][dimensionCount];
        for (int k = 0; k < clusterCount; ++k) {
            for (int i = 0; i < pointCount; ++i) {
                for (int l = 0; l < dimensionCount; ++l) {
                    means[k][l] += h[k][i] * points[i][l];
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                means[k][l] /= sums[k];
            }
        }
        // calculate covariances
        covariances = new double[clusterCount][dimensionCount][dimensionCount];
        for (int k = 0; k < clusterCount; ++k) {
            for (int i = 0; i < pointCount; ++i) {
                for (int l = 0; l < dimensionCount; ++l) {
                    for (int m = l; m < dimensionCount; ++m) {
                        covariances[k][l][m] += h[k][i] * (points[i][l] - means[k][l]) * (points[i][m] - means[k][m]);
                    }
                }
            }
            for (int l = 0; l < dimensionCount; ++l) {
                for (int m = 0; m < dimensionCount; ++m) {
                    covariances[k][l][m] /= sums[k];
                }
            }
        }
        // calculate mixture coefficients
        for (int k = 0; k < clusterCount; ++k) {
            mixtureCoefficients[k] = sums[k] / pointCount;
        }
    }

    /**
     * Returns the square of Euclidean distance between two points.
     *
     * @param x a point.
     * @param y a point.
     *
     * @return the square of the Euclidean distance between x an y.
     */
    private static double euclideanDistanceSquared(double[] x, double[] y) {
        double d = 0.0;
        for (int i = 0; i < x.length; ++i) {
            d += sqr(x[i] - y[i]);
        }

        return d;
    }

    /**
     * Returns the square of a number.
     *
     * @param x the number.
     *
     * @return the square of x.
     */
    private static double sqr(double x) {
        return x != 0.0 ? x * x : 0.0;
    }

    /**
     * Returns the index of the maximum value for an array of values.
     *
     * @param values the values.
     *
     * @return the index of the maximum value.
     */
    private static int indexMax(double[] values) {
        int index = 0;
        for (int i = 1; i < values.length; ++i) {
            if (values[i] > values[index]) {
                index = i;
            }
        }

        return index;
    }

    /**
     * Returns the index of the minimum value for an array of values.
     *
     * @param values the values.
     *
     * @return the index of the minimum value.
     */
    private static int indexMin(double[] values) {
        int index = 0;
        for (int i = 1; i < values.length; ++i) {
            if (values[i] < values[index]) {
                index = i;
            }
        }

        return index;
    }

    /**
     * Helper class for sorting numeric values along with an index.
     */
    private static class IndexedDouble implements Comparable<IndexedDouble> {
        /**
         * The index.
         */
        public int index;
        /**
         * The value.
         */
        public double value;

        private IndexedDouble(int index, double value) {
            this.index = index;
            this.value = value;
        }

        /**
         * Note: this class has a natural ordering that is inconsistent with equals.
         */
        public final int compareTo(IndexedDouble o) {
            return Double.compare(this.value, o.value);
        }

        /**
         * Creates an array of indexed doubles from an array of double values.
         *
         * @param values the values.
         *
         * @return the array created.
         */
        public static IndexedDouble[] createArray(double[] values) {
            final IndexedDouble[] indexedValues = new IndexedDouble[values.length];

            for (int i = 0; i < indexedValues.length; i++) {
                indexedValues[i] = new IndexedDouble(i, values[i]);

            }

            return indexedValues;
        }
    }

    /**
     * Generic <em>less than</em> comparator.
     */
    private static class Lt<T extends Comparable<T>> implements Comparator<T> {

        public final int compare(T o1, T o2) {
            return o2.compareTo(o1);
        }
    }
}
