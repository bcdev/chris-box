/* Copyright (C) 2002-2008 by Brockmann Consult
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

import org.esa.beam.cluster.Distribution;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.ProbabilityCalculator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization (EM) cluster algorithm.
 *
 * @author Ralf Quast
 * @version $Revision: 2221 $ $Date: 2008-06-16 11:19:52 +0200 (Mo, 16 Jun 2008) $
 */
public class Clusterer {

    private final int clusterCount;
    private final PixelAccessor pixelAccessor;

    // prior cluster probabilities
    private final double[] priors;
    // cluster means
    private final double[][] means;
    // cluster covariances
    private final double[][][] covariances;
    // cluster distributions
    private final Distribution[] distributions;

    // strategy for calculating posterior cluster probabilities
    private final ProbabilityCalculator calculator;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param pixelAccessor  the pixel accessor.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     * @param seed           the seed used to initialize the cluster algorithm
     *
     * @return the cluster decomposition.
     */
    public static EMCluster[] findClusters(PixelAccessor pixelAccessor, int clusterCount, int iterationCount, int seed) {
        return new Clusterer(pixelAccessor, clusterCount, seed).findClusters(iterationCount);
    }

    /**
     * Creates a {@link ProbabilityCalculator} for calculating posterior cluster
     * probabilities for the clusters provided as arguments.
     *
     * @param clusters the clusters.
     *
     * @return the probability calculator.
     */
    public static ProbabilityCalculator createProbabilityCalculator(EMCluster[] clusters) {
        final Distribution[] distributions = new Distribution[clusters.length];
        final double[] priors = new double[clusters.length];

        for (int i = 0; i < clusters.length; i++) {
            distributions[i] = new MultinormalDistribution(clusters[i].getMean(), clusters[i].getCovariances());
            priors[i] = clusters[i].getPriorProbability();
        }

        return new ProbabilityCalculator(distributions, priors);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param pixelAccessor the pixel accessor.
     * @param clusterCount  the number of clusters.
     * @param seed          the seed used to initialize the cluster algorithm.
     */
    public Clusterer(PixelAccessor pixelAccessor, int clusterCount, long seed) {
        final int sampleCount = pixelAccessor.getSampleCount();

        this.pixelAccessor = pixelAccessor;
        this.clusterCount = clusterCount;

        priors = new double[clusterCount];

        means = new double[clusterCount][sampleCount];
        covariances = new double[clusterCount][sampleCount][sampleCount];
        distributions = new Distribution[clusterCount];
        calculator = new ProbabilityCalculator(distributions, priors);

        initialize(new Random(seed));
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private EMCluster[] findClusters(int iterationCount) {
        for (int i = 0; i < iterationCount; ++i) {
            iterate();
            // todo - logging
        }

        return getClusters();
    }

    /**
     * Returns the clusters found, sorted according to their prior
     * probability.
     *
     * @return the clusters found.
     */
    public EMCluster[] getClusters() {
        return getClusters(new PriorProbabilityClusterComparator());
    }

    /**
     * Returns the clusters found, sorted according to a comparator.
     *
     * @param comparator a comparator.
     *
     * @return the clusters found.
     */
    public EMCluster[] getClusters(Comparator<EMCluster> comparator) {
        return getClusters(comparator, new EMCluster[clusterCount]);
    }

    /**
     * Returns the clusters found, sorted according to a comparator.
     *
     * @param comparator a comparator.
     * @param clusters   a preallocated array of clusters, on return holding the
     *                   clusters found.
     *
     * @return the clusters found.
     */
    public EMCluster[] getClusters(Comparator<EMCluster> comparator, EMCluster[] clusters) {
        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new EMCluster(means[k], covariances[k], priors[k]);
        }
        Arrays.sort(clusters, comparator);

        return clusters;
    }

    /**
     * Randomly initializes the clusters.
     *
     * @param random the random number generator used for initialization.
     */
    void initialize(Random random) {
        final int pixelCount = pixelAccessor.getPixelCount();
        final int sampleCount = pixelAccessor.getSampleCount();

        final double[] samples = new double[sampleCount];

        for (int k = 0; k < clusterCount; ++k) {
            pixelAccessor.getSamples(random.nextInt(pixelCount), samples);
            System.arraycopy(samples, 0, means[k], 0, sampleCount);
        }

        for (int k = 0; k < clusterCount; ++k) {
            priors[k] = 1.0; // same prior probability for all clusters

            for (int l = 0; l < sampleCount; ++l) {
                // initialization of diagonal elements with unity comes close
                // to an initial run with the k-means algorithm
                covariances[k][l][l] = 1.0;
            }

            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Carries out a single EM iteration.
     */
    public final void iterate() {
        iterate(false);
    }

    private void iterate(boolean updateCovariances) {
        final int pixelCount = pixelAccessor.getPixelCount();
        final int sampleCount = pixelAccessor.getSampleCount();

        final double[] sums = new double[clusterCount];
        final double[] posteriors = new double[clusterCount];
        final double[] samples = new double[sampleCount];

        for (int i = 0; i < pixelCount; ++i) {
            pixelAccessor.getSamples(i, samples);
            calculator.calculate(samples, posteriors);

            // calculate cluster means and covariances in a single pass
            // D. H. D. West (1979, Communications of the ACM, 22, 532)
            if (i == 0) {
                for (int k = 0; k < clusterCount; ++k) {
                    for (int l = 0; l < sampleCount; ++l) {
                        means[k][l] = samples[l];
                        covariances[k][l][l] = 0.0;
                        if (updateCovariances) {
                            for (int m = l + 1; m < sampleCount; ++m) {
                                covariances[k][l][m] = 0.0;
                            }
                        }
                    }
                    sums[k] = posteriors[k];
                }
            } else {
                for (int k = 0; k < clusterCount; ++k) {
                    final double temp = posteriors[k] + sums[k];

                    for (int l = 0; l < sampleCount; ++l) {
                        final double dist = samples[l] - means[k][l];

                        covariances[k][l][l] += sums[k] * posteriors[k] * dist * dist / temp;
                        if (updateCovariances) {
                            for (int m = l + 1; m < sampleCount; ++m) {
                                covariances[k][l][m] += sums[k] * posteriors[k] * dist * (samples[m] - means[k][m]) / temp;
                            }
                        }
                        means[k][l] += posteriors[k] * dist / temp;
                    }

                    sums[k] = temp;
                }
            }
        }

        for (int k = 0; k < clusterCount; ++k) {
            for (int l = 0; l < sampleCount; ++l) {
                covariances[k][l][l] /= sums[k];
                if (updateCovariances) {
                    for (int m = l + 1; m < sampleCount; ++m) {
                        covariances[k][l][m] /= sums[k];
                        covariances[k][m][l] = covariances[k][l][m];
                    }
                }
            }

            priors[k] = sums[k] / pixelCount;
            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
    }

    /**
     * Cluster comparator.
     * <p/>
     * Compares two clusters according to their prior probability.
     */
    public static class PriorProbabilityClusterComparator implements Comparator<EMCluster> {
        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }

    /**
     * Multinormal distribution with vanishing covariances.
     */
    private static class MultinormalDistribution implements Distribution {
        private final double[] means;
        private final double[] variances;
        private final double logNormFactor;

        private MultinormalDistribution(double[] means, double[][] covariances) {
            this.means = means.clone();
            this.variances = new double[means.length];

            double det = variances[0] = covariances[0][0];
            for (int i = 1; i < variances.length; ++i) {
                variances[i] = covariances[i][i];
                det *= variances[i];
            }
            if (det == 0) {
                throw new ArithmeticException("covariance matrix is singular.");
            }
            logNormFactor = -0.5 * (means.length * Math.log(2.0 * Math.PI) + Math.log(det));
        }

        @Override
        public double probabilityDensity(double[] y) {
            return Math.exp(logProbabilityDensity(y));
        }

        @Override
        public double logProbabilityDensity(double[] y) {
            if (y.length != means.length) {
                throw new IllegalArgumentException("y.length != mean.length");
            }
            return logNormFactor - 0.5 * mahalanobisSquaredDistance(y);
        }

        private double mahalanobisSquaredDistance(double[] y) {
            double sum = 0.0;

            for (int i = 0; i < means.length; ++i) {
                final double dist = y[i] - means[i];
                sum += (dist * dist) / variances[i];
            }

            return sum;
        }
    }
}
