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

import com.bc.math.matrix.DefaultEigenproblemSolver;
import com.bc.math.matrix.Eigendecomposition;

import static java.lang.Math.*;
import java.text.MessageFormat;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class MultinormalDistribution {

    private final int n;

    private final double[] mean;
    private final Eigendecomposition decomposition;
    private final double normFactor;

    public MultinormalDistribution(double[] mean, double[][] covariances) {
        this(mean.length, mean, covariances);
    }

    private MultinormalDistribution(int n, double[] mean, double[][] covariances) {
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

        decomposition = new DefaultEigenproblemSolver().createEigenvalueDecomposition(n, covariances);
        normFactor = 1.0 / pow(2.0 * PI, 0.5 * n) / sqrt(product(decomposition.getEigenvalues()));
    }

    /**
     * Returns the probability density for a given vector.
     *
     * @param y the vector.
     *
     * @return the probability density for the vector y.
     */
    public final double probabilityDensity(double[] y) {
        if (y.length != n) {
            throw new IllegalArgumentException("y.length != n");
        }

        double u = 0.0;

        for (int i = 0; i < n; ++i) {
            double d = 0.0;

            for (int j = 0; j < n; ++j) {
                d += decomposition.getV(i, j) * (y[j] - mean[j]);
            }

            u += (d * d) / decomposition.getEigenvalue(i);
        }

        return normFactor * exp(-0.5 * u);
    }

    private static double product(double[] w) {
        double p = w[0];

        for (int i = 1; i < w.length; ++i) {
            p *= w[i];
        }

        return p;
    }
}
