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
     * 
     * The result is an array with the angular velocity calculated from
     * consecutive points of the trajectory. The last element is the mean
     * angular velocity (calculated from the first and last position of the
     * trajectory)
     * 
     * Values are in radians!
     * 
     * @return the Angular Velocity for a given 3D trajectory
     */
    static double[] angVel(double[] secs, double[] xeci, double[] yeci, double[] zeci) {
        double[] av = new double[secs.length - 1];
        for (int i = 0; i < av.length; i++) {
            double a = vectAngle(xeci[i], yeci[i], zeci[i], xeci[i+1], yeci[i+1], zeci[i+1]);
            double b = secs[i] - secs[i+1];
            
            av[i] = a / Math.abs(b);
        }
        return av;
    }

    /**
     * Angular distance between points.
     */
    static double vectAngle(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Compute vector dot product for both points
        double cs = x1*x2 + y1*y2 + z1*z2;
     
        // Compute the vector cross product for both points
        double xc = y1*z2 - z1*y2;
        double yc = z1*x2 - x1*z2;
        double zc = x1*y2 - y1*x2;
        double sn = Math.sqrt(xc*xc + yc*yc + zc*zc);
     
        //--- Convert to polar.  ------
        double[] ra = recpol(cs, sn);
        return ra[1];
    }
    
    /**
     * Convert 2-d rectangular coordinates to polar coordinates.

     * @param x y component of vector in rectangular form
     * @param y y component of vector in rectangular form
     * @return two component array containing vector in polar form: radius, angle
     */
    static double[] recpol(double x, double y) {
        //  Angle complicated because atan won't take (0,0) and
        //  also because want to keep angle in 0 to 360 (2 pi) range.
        double a = 0; // Output angle
        if (x != 0 && y != 0) {
            a = Math.atan2(y, x);
        }
        // add 2 pi to angles < 0
        if (a < 0 ) {
            a = a + 2 * Math.PI;
        }
     
        double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); // Find radii

        return new double[] {r, a};
    }
}
