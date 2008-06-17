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
package org.esa.beam.chris.operators.internal;

import java.util.Arrays;

/**
 * Powell minimisation.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Minimizer {
    private final int maxIter;

    /**
     * Constructs a new instance of this class.
     *
     * @param maxIter the maximum number of iterations.
     */
    public Minimizer(int maxIter) {
        this.maxIter = maxIter;
    }

    public double findMinimum(MultivariateFunction function, double[] x) {
        final double[] p = Arrays.copyOf(x, x.length);

        return 0.0;
    }
}
