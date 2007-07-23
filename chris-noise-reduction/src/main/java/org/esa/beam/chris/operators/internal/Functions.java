/* $Id: $
 *
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

/**
 * Interface representing an infinite sequence of real functions. The
 * interface is suited for families of functions which are defined by
 * recursion, for example Hermite or Legendre polynomials.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public interface Functions {

    /**
     * Evaluates the first n functions at a given abscissa value.
     *
     * @param x the abscissa value.
     * @param y the calculated function values. The number of calculated
     *          values is defined by the length of this array.  No value
     *          is calculated if the array is empty.
     * @throws NullPointerException if {@code y} is {@code null}.
     */
    void calculate(double x, double[] y) throws NullPointerException;

}
