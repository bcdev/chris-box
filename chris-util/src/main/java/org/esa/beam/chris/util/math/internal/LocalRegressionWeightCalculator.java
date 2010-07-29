/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.chris.util.math.internal;

/**
 * Local regression weight calculator interface.
 *
 * @author Ralf Quast
 * @version $Revision: 2530 $ $Date: 2008-07-09 13:10:39 +0200 (Wed, 09 Jul 2008) $
 * @since BEAM 4.2
 */
public interface LocalRegressionWeightCalculator {
    /**
     * Calculates the local regression weights for the ith point in the span.
     *
     * @param i the point index.
     * @param w the regression weights.
     */
    void calculateRegressionWeights(int i, double[] w);

    /**
     * Calculates the robust regression weights from the absolute residuals
     * of the local regression smoothing procedure.
     *
     * @param a the absolute residuals.
     * @param w the robust regression weights.
     */
    void calculateRobustRegressionWeights(double[] a, double[] w);
}
