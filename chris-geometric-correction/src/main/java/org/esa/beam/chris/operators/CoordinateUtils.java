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

import static org.esa.beam.chris.util.math.internal.Pow.pow2;


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
    static double[] angVel(double[] secs, double[] xeci, double[] yeci, double[] zeci) {
        double[] av = new double[secs.length];
        for (int i = 0; i < (av.length - 1); i++) {
            double a = vectAngle(xeci[i], yeci[i], zeci[i], xeci[i + 1], yeci[i + 1], zeci[i + 1]);
            double b = secs[i] - secs[i + 1];

            av[i] = a / Math.abs(b);
        }
        double a = vectAngle(xeci[0], yeci[0], zeci[0], xeci[secs.length - 1], yeci[secs.length - 1],
                             zeci[secs.length - 1]);
        double b = secs[0] - secs[secs.length - 1];

        av[secs.length - 1] = a / Math.abs(b);
        return av;
    }

    /**
     * Angular distance between points.
     */
    static double vectAngle(double x1, double y1, double z1, double x2, double y2, double z2) {
        // compute the scalar product
        final double cs = x1 * x2 + y1 * y2 + z1 * z2;
        // compute the vector product
        final double u = y1 * z2 - z1 * y2;
        final double v = z1 * x2 - x1 * z2;
        final double w = x1 * y2 - y1 * x2;

        final double sn = Math.sqrt(u * u + v * v + w * w);
        return angle(cs, sn);
    }

    private static double angle(double x, double y) {
        double angle = 0.0;
        if (x != 0 && y != 0) {
            angle = Math.atan2(y, x);
        }
        if (angle < 0.0) {
            angle = angle + 2.0 * Math.PI;
        }
        return angle;
    }


    /**
     * Calculate the View Angles in the LHLV coordinate system (Local Horizon, Local Vertical)
     * <p/>
     * NOTE: The local vertical is obtained from the latitude geodetica calculated from the position TGT
     */
    static ViewAng computeViewAng(double TgtX, double TgtY, double TgtZ, double SatX, double SatY, double SatZ) {
        double RangX = SatX - TgtX;
        double RangY = SatY - TgtY;
        double RangZ = SatZ - TgtZ;
        double Rango = Math.sqrt(pow2(RangX) + pow2(RangY) + pow2(RangZ));

        // Calculates the latitude of geodetica TGT (point where you define the vertical Local)

        double[] gdt = CoordinateConverter.ecefToWgs(TgtX, TgtY, TgtZ, new double[3]);

        double sin_lat = Math.sin(Math.toRadians(gdt[1]));
        double cos_lat = Math.cos(Math.toRadians(gdt[1]));

        double sin_theta = TgtY / Math.sqrt(pow2(TgtX) + pow2(TgtY));
        double cos_theta = TgtX / Math.sqrt(pow2(TgtX) + pow2(TgtY));

        double top_s = sin_lat * cos_theta * RangX + sin_lat * sin_theta * RangY - cos_lat * RangZ;
        double top_e = -sin_theta * RangX + cos_theta * RangY;
        double top_z = cos_lat * cos_theta * RangX + cos_lat * sin_theta * RangY + sin_lat * RangZ;

        double AZI = Math.atan(-top_e / top_s);
        if (top_s > 0) {
            AZI += Math.PI;
        }
        if (top_s < 0) {
            AZI += 2.0 * Math.PI;
        }
        AZI = AZI % (2.0 * Math.PI);
        double ZEN = Math.PI / 2 - Math.asin(top_z / Rango);

        return new ViewAng(AZI, ZEN, RangX, RangY, RangZ);
    }

    static class ViewAng {

        final double azi;
        final double zen;
        final double rangeX;
        final double rangeY;
        final double rangeZ;

        public ViewAng(double azi, double zen, double rangeX, double rangeY, double rangeZ) {
            this.azi = azi;
            this.zen = zen;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
            this.rangeZ = rangeZ;
        }
    }
}
