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

package org.esa.beam.chris.operators;

import com.bc.ceres.core.Assert;

import static java.lang.Math.sqrt;

/**
 * Utility class for calculating the intersection of a straight line, which is defined
 * by a point and a direction, and an ellipsoid.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class Intersector {

    /**
     * Calculates the intersection of a straight line and an ellipsoid.
     * <p/>
     * Note that all parameters must be arrays of length 3.
     *
     * @param point     any point on the straight line. On return contains the
     *                  intersection point, which is closest to this point.
     *                  If the straight line and the ellipsoid do not intersect,
     *                  all coordinates of this point are {@link Double#NaN}.
     * @param direction the direction of the straight line.
     * @param center    the center of the ellipsoid.
     * @param semiAxes  the semi-axes of the ellipsoid.
     */
    public static void intersect(double[] point, double[] direction, double[] center, double[] semiAxes) {
        Assert.notNull(point);
        Assert.notNull(direction);
        Assert.notNull(center);
        Assert.notNull(semiAxes);

        Assert.argument(point.length == 3);
        Assert.argument(direction.length == 3);
        Assert.argument(center.length == 3);
        Assert.argument(semiAxes.length == 3);

        double a = 0.0;
        double b = 0.0;
        double c = 0.0;
        for (int i = 0; i < 3; i++) {
            final double q = point[i] - center[i];
            final double rr = semiAxes[i] * semiAxes[i];
            a += (direction[i] * direction[i]) / rr;
            b += (direction[i] * q) / rr;
            c += (q * q) / rr;
        }
        c -= 1.0;

        final double d = b * b - a * c;
        final double t;
        if (b > 0.0) {
            t = (-b + sqrt(d)) / a;
        } else {
            t = (-b - sqrt(d)) / a;
        }

        for (int i = 0; i < 3; i++) {
            point[i] += t * direction[i];
        }
    }
}
