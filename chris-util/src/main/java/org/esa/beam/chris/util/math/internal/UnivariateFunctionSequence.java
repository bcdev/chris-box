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
 * Interface representing an infinite sequence of univariate functions.
 * The interface is suited for families of functions which are defined
 * by recurrence relations, such as Hermite or Legendre polynomials.
 *
 * @author Ralf Quast
 * @version $Revision: 2402 $ $Date: 2008-07-02 21:14:58 +0200 (Wed, 02 Jul 2008) $
 */
public interface UnivariateFunctionSequence {

    /**
     * Evaluates the first n functions at a given abscissa value.
     *
     * @param x the abscissa value.
     * @param y the calculated function values. The number of calculated
     *          values is defined by the length of this array.  No value
     *          is calculated if the array is empty.
     *
     * @throws NullPointerException if {@code y} is {@code null}.
     */
    void calculate(double x, double[] y) throws NullPointerException;

    /**
     * Evaluates the first n functions at given abscissa values.
     *
     * @param x the abscissa values.
     * @param y the calculated function values. The number of calculated
     *          values is defined by the length of this array.  No value
     *          is calculated if the array is empty.
     *          The value {@code y[i][k]} corresponds to the value of the
     *          ith function in the sequence at the kth abscissa value.
     *
     * @throws NullPointerException if {@code x} or {@code y} is {@code null}.
     */
    void calculate(double[] x, double[][] y);
}
