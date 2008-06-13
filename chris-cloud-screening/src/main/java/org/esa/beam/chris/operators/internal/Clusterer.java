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

import static java.lang.Math.exp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Expectation maximization (EM) cluster algorithm.
 * <p/>
 * todo - observer notifications
 * todo - make algorithm use tiles
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class Clusterer {

    private final int m;
    private final int n;
    private final double[][] points;
    private final int clusterCount;

    private final double[] p;
    private final double[][] h;
    private final double[][] means;
    private final double[][][] covariances;
    private final Distribution[] distributions;

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    public static Cluster[] findClusters(double[][] points, int clusterCount, int iterationCount) {
        return new Clusterer(points, clusterCount).findClusters(iterationCount);
    }

    /**
     * Finds a collection of clusters for a given set of data points.
     *
     * @param points         the data points.
     * @param clusterCount   the number of clusters.
     * @param iterationCount the number of EM iterations to be made.
     * @param dist           the minimum distance to be exceeded by any pair of initial clusters.
     *
     * @return the cluster decomposition.
     */
    public static Cluster[] findClusters(double[][] points, int clusterCount, int iterationCount, double dist) {
        return new Clusterer(points, clusterCount, dist).findClusters(iterationCount);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     */
    public Clusterer(double[][] points, int clusterCount) {
        this(points, clusterCount, 0.0);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param dist         the minimum distance to be exceeded by any pair of initial clusters.
     */
    public Clusterer(double[][] points, int clusterCount, double dist) {
        this(points, clusterCount, dist, 31415);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param dist         the minimum distance to be exceeded by any pair of initial clusters.
     * @param seed         the seed used for the random initialization of clusters.
     */
    public Clusterer(double[][] points, int clusterCount, double dist, int seed) {
        this(points.length, points[0].length, points, clusterCount, dist, seed);
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param m            the number of data points.
     * @param n            the dimension of the point space.
     * @param points       the data points.
     * @param clusterCount the number of clusters.
     * @param dist         the minimum distance to be exceeded by any pair of initial clusters.
     * @param seed         the seed used for the random initialization of clusters.
     */
    private Clusterer(int m, int n, double[][] points, int clusterCount, double dist, int seed) {
        // todo: check arguments

        this.m = m;
        this.n = n;
        this.points = points;
        this.clusterCount = clusterCount;

        p = new double[clusterCount];
        h = new double[clusterCount][m];
        means = new double[clusterCount][n];
        covariances = new double[clusterCount][n][n];
        distributions = new Distribution[clusterCount];

        initialize(dist, seed);
    }

    /**
     * Finds a collection of clusters.
     *
     * @param iterationCount the number of EM iterations to be made.
     *
     * @return the cluster decomposition.
     */
    private Cluster[] findClusters(int iterationCount) {
        while (iterationCount > 0) {
            iterate();
            iterationCount--;
            // todo - notify observer
        }

        return getClusters();
    }

    /**
     * Carries out a single EM iteration.
     * todo - make private when observer notifications implemented
     */
    public void iterate() {
        // M-step
        for (int k = 0; k < clusterCount; ++k) {
            p[k] = calculateMoments(h[k], means[k], covariances[k]);
            distributions[k] = new MultinormalDistribution(means[k], covariances[k]);
        }
        // E-step
        for (int i = 0; i < m; ++i) {
            double sum = 0.0;
            for (int k = 0; k < clusterCount; ++k) {
                h[k][i] = p[k] * distributions[k].probabilityDensity(points[i]);
                sum += h[k][i];
            }
            if (sum > 0.0) {
                for (int k = 0; k < h.length; ++k) {
                    final double t = h[k][i] / sum;
                    h[k][i] = t;
                }
            } else { // numerical underflow - recompute posterior cluster probabilities
                final double[] sums = new double[clusterCount];
                for (int k = 0; k < clusterCount; ++k) {
                    h[k][i] = distributions[k].logProbabilityDensity(points[i]);
                }
                for (int k = 0; k < clusterCount; ++k) {
                    for (int l = 0; l < clusterCount; ++l) {
                        if (l != k) {
                            sums[k] += (p[l] / p[k]) * exp(h[l][i] - h[k][i]);
                        }
                    }
                }
                for (int k = 0; k < clusterCount; ++k) {
                    final double t = 1.0 / (1.0 + sums[k]);
                    h[k][i] = t;
                }
            }
        }
    }

    /**
     * Returns the clusters found.
     * todo - make private when observer notifications implemented
     *
     * @return the clusters found.
     */
    public Cluster[] getClusters() {
        return getClusters(new DefaultClusterComparator());
    }

    public Cluster[] getClusters(Comparator<Cluster> clusterComparator) {
        final Cluster[] clusters = new Cluster[clusterCount];
        for (int k = 0; k < clusterCount; ++k) {
            clusters[k] = new Cluster(n, points, p[k], h[k], distributions[k]);
        }
        Arrays.sort(clusters, clusterComparator);

        return clusters;
    }

    /**
     * Randomly initializes the clusters using the k-means method.
     *
     * @param dist the minimum distance to be exceeded by any pair of initial clusters.
     * @param seed the seed value used for initializing the random number generator.
     */
    private void initialize(double dist, int seed) {
        final Random random = new Random(seed);

        do {
            kInit(dist, random);
            for (int i = 0; i < m; ++i) {
                for (int k = 0; k < clusterCount; ++k) {
                    h[k][i] = kDist(means[k], points[i]);
                }
                int parent = 0;
                for (int k = 1; k < clusterCount; ++k) {
                    if (h[k][i] < h[parent][i]) {
                        parent = k;
                    }
                }
                for (int k = 0; k < clusterCount; ++k) {
                    if (parent == k) {
                        h[k][i] = 1.0;
                    } else {
                        h[k][i] = 0.0;
                    }
                }
            }
        } while (!kStop());
    }

    /**
     * Random k-means initialization.
     *
     * @param dist   the minimum distance to be exceeded by any pair of initial clusters.
     * @param random the randm number generator.
     */
    private void kInit(double dist, Random random) {
        for (int k = 0; k < clusterCount; ++k) {
            boolean accepted = true;
            do {
                System.arraycopy(points[random.nextInt(m)], 0, means[k], 0, n);
                for (int i = 0; i < k; ++i) {
                    accepted = kDist(means[k], means[i]) > dist * dist;
                    if (!accepted) {
                        // todo - notify observer
                        break;
                    }
                }
            } while (!accepted);
        }
    }

    /**
     * Distance measure used by the k-means method.
     *
     * @param x a point.
     * @param y a point.
     *
     * @return squared Euclidean distance between x and y.
     */
    private double kDist(double[] x, double[] y) {
        double d = 0.0;
        for (int l = 0; l < n; ++l) {
            d += (y[l] - x[l]) * (y[l] - x[l]);
        }

        return d;
    }

    /**
     * Tests whether the k-means initialization was successful.
     *
     * @return {@code true} when each cluster has enough members, {@code false} otherwise.
     */
    private boolean kStop() {
        testing:
        for (int k = 0; k < clusterCount; ++k) {
            int memberCount = 0;
            for (int i = 0; i < m; ++i) {
                if (h[k][i] != 0.0) {
                    ++memberCount;
                    if (memberCount == Math.max(2, n)) {
                        continue testing;
                    }
                }
            }
            // todo - notify observer
            return false;
        }

        return true;
    }

    /**
     * Calculates the statistical moments.
     *
     * @param h           the posterior probabilities associated with the data points.
     * @param mean        the mean of the data points.
     * @param covariances the covariances of the data points.
     *
     * @return the mean posterior probability.
     */
    private double calculateMoments(double[] h, double[] mean, double[][] covariances) {
        for (int k = 0; k < n; ++k) {
            for (int l = k; l < n; ++l) {
                covariances[k][l] = 0.0;
            }
            mean[k] = 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < m; ++i) {
            for (int k = 0; k < n; ++k) {
                mean[k] += h[i] * points[i][k];
            }
            sum += h[i];
        }
        for (int k = 0; k < n; ++k) {
            mean[k] /= sum;
        }
        for (int i = 0; i < m; ++i) {
            for (int k = 0; k < n; ++k) {
                for (int l = k; l < n; ++l) {
                    covariances[k][l] += h[i] * (points[i][k] - mean[k]) * (points[i][l] - mean[l]);
                }
            }
        }
        for (int k = 0; k < n; ++k) {
            for (int l = k; l < n; ++l) {
                covariances[k][l] /= sum;
                covariances[l][k] = covariances[k][l];
            }
        }

        return sum / m;
    }

    /**
     * Cluster comparator.
     */
    private static class DefaultClusterComparator implements Comparator<Cluster> {

        public int compare(Cluster c1, Cluster c2) {
            return Double.compare(c2.getPriorProbability(), c1.getPriorProbability());
        }
    }
}
