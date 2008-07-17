/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.chris.util.math;

/**
 * todo - add API doc
*
* @author Ralf Quast
* @version $Revision$ $Date$
* @since BEAM 4.2
*/
public class Regression {
    private final int m;
    private final int n;

    private final double[][] a;
    private final double[][] u;
    private final double[][] v;
    private final double[] s;

    private final int rank;

    public Regression(double[]... x) {
        m = x[0].length;
        n = x.length;
        a = new double[m][n];

        // build the design matrix
        for (int i = 0; i < m; ++i) {
            final double[] ai = a[i];
            for (int j = 0; j < n; ++j) {
                ai[j] = x[j][i];
            }
        }

        final Jama.SingularValueDecomposition svd = new Jama.Matrix(a, m, n).svd();

        u = svd.getU().getArray();
        v = svd.getV().getArray();
        s = svd.getSingularValues();

        rank = svd.rank();
    }

    public double[] fit(double[] y, double[] z, double[] c, double[] w) {
        // compute coefficients
        for (int j = 0; j < rank; ++j) {
            c[j] = 0.0;
            w[j] = 0.0;
            for (int i = 0; i < m; ++i) {
                w[j] += u[i][j] * y[i];
            }
            w[j] /= s[j];
        }
        for (int j = 0; j < n; ++j) {
            final double[] vj = v[j];

            for (int i = 0; i < rank; ++i) {
                c[j] += vj[i] * w[i];
            }
        }
        // compute fit values
        for (int i = 0; i < rank; ++i) {
            final double ci = c[i];

            for (int j = 0; j < m; ++j) {
                z[j] += ci * a[j][i];
            }
        }

        return z;
    }
}
