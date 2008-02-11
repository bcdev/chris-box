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
 * @version $Revision: 1412 $ $Date: 2007-11-24 02:18:27 +0100 (Sa, 24 Nov 2007) $
 */
public class MultinormalDistribution implements Distribution {

    private final int n;

    private double[] mean;
    private Eigendecomposition eigendecomposition;
    private double normFactor;

    /**
     * Constructs a new multinormal distribution.
     *
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances. Only the upper triangular
     *                    elements are used.
     */
    public MultinormalDistribution(double[] mean, double[][] covariances) {
        this(mean.length, mean, covariances, new SymmetricEigenproblemSolver());
    }

    /**
     * Constructs a new multinormal distribution.
     *
     * @param n           the dimension of the domain.
     * @param mean        the distribution mean.
     * @param covariances the distribution covariances. Only the upper triangular
     *                    elements are used.
     * @param solver      the {@link org.esa.beam.chris.operators.internal.MultinormalDistribution.EigenproblemSolver} used for decomposing the covariance matrix.
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
        normFactor = 1.0 / pow(2.0 * PI, 0.5 * n) / sqrt(eigendecomposition.getEigenvalueProduct());
    }

    /**
     * Returns the probability density for a given vector.
     *
     * @param y the vector.
     * @return the probability density for the vector y.
     */
    public final double probabilityDensity(double[] y) {
        if (y.length != n) {
            throw new IllegalArgumentException("y.length != n");
        }

        return normFactor * exp(-0.5 * mahalanobisSquaredDistance(y));
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

    private static class SymmetricEigenproblemSolver implements EigenproblemSolver {

        public Eigendecomposition createEigendecomposition(int n, double[][] matrix) {
            final double[][] symmetricMatrix = matrix.clone();

            for (int i = 0; i < n; ++i) {
                for (int j = i + 1; j < n; ++j) {
                    symmetricMatrix[j][i] = symmetricMatrix[i][j];
                }
            }

            return new SymmetricEigendecomposition(new Jama.Matrix(symmetricMatrix, n, n).eig());
        }
    }

    private static class SymmetricEigendecomposition implements Eigendecomposition {
        private final Jama.EigenvalueDecomposition decomposition;

        public SymmetricEigendecomposition(Jama.EigenvalueDecomposition decomposition) {
            this.decomposition = decomposition;
        }

        public double getEigenvalue(int i) {
            return decomposition.getRealEigenvalues()[i];
        }

        public double getV(int i, int j) {
            return decomposition.getV().get(i, j);
        }

        public double getEigenvalueProduct() {
            final double[] eigenvalues = decomposition.getRealEigenvalues();

            double product = eigenvalues[0];
            for (int i = 1; i < eigenvalues.length; ++i) {
                product *= eigenvalues[i];
            }

            return product;
        }
    }

    private static interface EigenproblemSolver {
        public Eigendecomposition createEigendecomposition(int n, double[][] matrix);
    }

    private static interface Eigendecomposition {
        public double getEigenvalue(int i);

        public double getV(int i, int j);

        public double getEigenvalueProduct();
    }
}
