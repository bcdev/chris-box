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

/**
 * Class with static methods for calculating integral powers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Pow {

    public static double pow2(double x) {
        return x * x;
    }

    public static double pow3(double x) {
        return x * x * x;
    }

    public static double pow4(double x) {
        final double x2 = x * x;

        return x2 * x2;
    }

    public static double pow5(double x) {
        final double x2 = x * x;

        return x2 * x2 * x;
    }

    public static double pow6(double x) {
        final double x2 = x * x;

        return x2 * x2 * x2;
    }

    public static double pow7(double x) {
        final double x3 = x * x * x;

        return x3 * x3 * x;
    }

    public static double pow8(double x) {
        final double x2 = x * x;
        final double x4 = x2 * x2;

        return x4 * x4;
    }

    public static double pow9(double x) {
        final double x2 = x * x;
        final double x4 = x2 * x2;

        return x4 * x4 * x;
    }
}
