/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.chris.operators;


class CoordinateUtils {

    /**
     * Given a series satellite positions in ECI and the corresponding times
     * estimates the angular velocity depends on the VECT_ANGLE function.
     * <p/>
     * The result is an array with the angular velocity calculated from
     * consecutive points of the trajectory. The last element is the mean
     * angular velocity (calculated from the first and last position of the
     * trajectory)
     * <p/>
     * Values are in radians!
     *
     * @return the Angular Velocity for a given 3D trajectory
     */
    static double[] angularVelocity(double[] secs, double[] xeci, double[] yeci, double[] zeci) {
        double[] av = new double[secs.length];
        for (int i = 0; i < av.length - 1; i++) {
            double a = angle(xeci[i], yeci[i], zeci[i], xeci[i + 1], yeci[i + 1], zeci[i + 1]);
            double b = secs[i + 1] - secs[i];

            av[i] = a / b;
        }
        double a = angle(xeci[0], yeci[0], zeci[0], xeci[secs.length - 1], yeci[secs.length - 1],
                         zeci[secs.length - 1]);
        double b = secs[secs.length - 1] - secs[0];
        av[secs.length - 1] = a / b;
        return av;
    }

    static double angle(double x1, double y1, double z1, double x2, double y2, double z2) {
        // compute the scalar product
        final double cs = x1 * x2 + y1 * y2 + z1 * z2;
        // compute the vector product
        final double u = y1 * z2 - z1 * y2;
        final double v = z1 * x2 - x1 * z2;
        final double w = x1 * y2 - y1 * x2;

        final double sn = Math.sqrt(u * u + v * v + w * w);
        return polarAngle(cs, sn);
    }

    private static double polarAngle(double x, double y) {
        double angle = 0.0;
        if (x != 0 && y != 0) {
            angle = Math.atan2(y, x);
        }
        if (angle < 0.0) {
            angle = angle + 2.0 * Math.PI;
        }
        return angle;
    }
}
