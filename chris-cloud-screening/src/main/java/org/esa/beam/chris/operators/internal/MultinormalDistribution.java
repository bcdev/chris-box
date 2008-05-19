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

import static java.lang.Math.*;
import java.text.MessageFormat;

/**
 * Multinormal distribution.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MultinormalDistribution implements Distribution {

    private final int n;

    private final double[] mean;
    private final Eigendecomposition eigendecomposition;
    private final double logNormFactor;

    /**
     * Constructs a new multinormal distribution.
     *
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances.
     */
    public MultinormalDistribution(double[] mean, double[][] covariances) {
        this(mean.length, mean, covariances, new EigenproblemSolver());
    }

    /**
     * Constructs a new multinormal distribution.
     *
     * @param n           the dimension of the domain.
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances. Only the upper triangular
     *                    elements are used.
     * @param solver      the {@link EigenproblemSolver} used for decomposing the covariance matrix.
     */
    private MultinormalDistribution(int n, double[] mean, double[][] covariances, EigenproblemSolver solver) {
        if (mean.length != n) {
            throw new IllegalArgumentException("mean.length != n");
        }
        if (covariances.length != n) {
            throw new IllegalArgumentException("covariances.length != n");
        }
        for (int i = 0; i < n; ++i) {
            if (covariances[i].length != n) {
                throw new IllegalArgumentException(MessageFormat.format("covariances[{0}].length != n", i));
            }
        }

        this.n = n;
        this.mean = mean;

        eigendecomposition = solver.createEigendecomposition(n, covariances);
        logNormFactor = -0.5 * (n * log(2.0 * PI) + log(eigendecomposition.getEigenvalueProduct()));
    }

    public final double probabilityDensity(double[] y) {
        return exp(logProbabilityDensity(y));
    }

    public final double logProbabilityDensity(double[] y) {
        if (y.length != n) {
            throw new IllegalArgumentException("y.length != n");
        }

        return logNormFactor - 0.5 * mahalanobisSquaredDistance(y);
    }

    public double[] getMean() {
        return mean;
    }

    private double mahalanobisSquaredDistance(double[] y) {
        double u = 0.0;

        for (int i = 0; i < n; ++i) {
            double d = 0.0;

            for (int j = 0; j < n; ++j) {
                d += eigendecomposition.getV(i, j) * (y[j] - mean[j]);
            }

            u += (d * d) / eigendecomposition.getEigenvalue(i);
        }

        return u;
    }

    private static class EigenproblemSolver {
        private static final double MAX_CONDITION = 1.0E14;

        public Eigendecomposition createEigendecomposition(int n, double[][] symmetricMatrix) {
            final Eigendecomposition eigendecomposition =
                    new Eigendecomposition(new Jama.Matrix(symmetricMatrix, n, n).svd());
            final double[] d = eigendecomposition.getEigenvalues();
            final double t = d[0] / MAX_CONDITION - d[n - 1];
            if (t > 0.0) {
                for (int i = 0; i < n; ++i) {
                    symmetricMatrix[i][i] += t;
                    d[i] += t;
                }
            }

            return eigendecomposition;
        }
    }

    private static class Eigendecomposition {
        private double[] eigenvalues;
        private double[][] v;

        public Eigendecomposition(Jama.SingularValueDecomposition decomposition) {
            eigenvalues = decomposition.getSingularValues();
            v = decomposition.getV().getArray();
        }

        public double getEigenvalue(int i) {
            return eigenvalues[i];
        }

        public double[] getEigenvalues() {
            return eigenvalues;
        }

        public double getV(int i, int j) {
            return v[i][j];
        }

        public double getEigenvalueProduct() {
            double product = eigenvalues[0];
            for (int i = 1; i < eigenvalues.length; ++i) {
                product *= eigenvalues[i];
            }

            return product;
        }
    }
}
