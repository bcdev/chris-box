package org.esa.beam.chris.operators;

import static org.esa.beam.chris.util.math.internal.Pow.pow2;
import static org.esa.beam.chris.util.math.internal.Pow.pow3;

import java.awt.geom.Point2D;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

class Conversions {

    /**
     * Major radius of WGS-84 ellipsoid (km).
     */
    private static final double WGS84_A = 6378.137;
    /**
     * Flattening of WGS-84 ellipsoid.
     */
    private static final double WGS84_F = 1.0 / 298.257223563;

    /**
     * Eccentricity squared.
     */
    private static final double WGS84_E = WGS84_F * (2.0 - WGS84_F);

    private Conversions() {
    }

    public static void wgsToEcef(double lon, double lat, double alt, double[] ecef) {
        if (Math.abs(lat) > 90.0) {
            throw new IllegalArgumentException("|lat| > 90.0");
        }
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (ecef.length != 3) {
            throw new IllegalArgumentException("ecef.length != 3");
        }

        final double u = Math.toRadians(lon);
        final double v = Math.toRadians(lat);

        final double cu = Math.cos(u);
        final double su = Math.sin(u);
        final double cv = Math.cos(v);
        final double sv = Math.sin(v);

        final double a = WGS84_A / Math.sqrt(1.0 - WGS84_E * pow2(sv));
        final double b = (a + alt) * cv;

        ecef[0] = b * cu;
        ecef[1] = b * su;
        ecef[2] = ((1.0 - WGS84_E) * a + alt) * sv;
    }

    /**
     * Transforms geodetic coordinates (spherical) to Earth Centered Fixed ECF
     * cartesian coordinates (rectangular) Altitude h starts at the reference
     * geoid and must be in kilometers, Latitude and Longitude are operated in degrees.
     * <p/>
     * NOTE: accuracy might be improved for coordinates close to the XY plane or
     * the Z axis by using alternative trigonometric formulation in those cases
     */
    public static Point2D ecef2wgs(double x, double y, double z) {
        double b = WGS84_A * (1.0 - WGS84_F);
        b = b * Math.signum(z);

        double R = Math.sqrt(pow2(x) + pow2(y));
        double E = (b * z - (pow2(WGS84_A) - pow2(b))) / (WGS84_A * R);
        double F = (b * z + (pow2(WGS84_A) - pow2(b))) / (WGS84_A * R);
        double P = 4 * (E * F + 1) / 3.0;
        double Q = 2 * (pow2(E) - pow2(F));
        double D = Math.sqrt(pow3(P) + pow2(Q));
        double nu = Math.pow((D - Q), (1.0 / 3.0)) - Math.pow((D + Q), (1.0 / 3.0));
        double G = (Math.sqrt(pow2(E) + nu) + E) / 2.0;
        double t = Math.sqrt(pow2(G) + (F - nu * G) / (2 * G - E)) - G;

        double LATgdt = Math.atan2(WGS84_A * (1 - pow2(t)), 2 * b * t);
        double LONgdt = Math.atan2(y, x);
        double ALTgdt = (R - WGS84_A * t) * Math.cos(LATgdt) + (z - b) * Math.sin(LATgdt);

        if (x == 0.0 && y == 0.0 && z != 0.0) {
            LATgdt = Math.PI / 2.0 * Math.signum(z);
            LONgdt = 0.0;
            ALTgdt = Math.abs(z - b);
        }
        if (z == 0.0) {
            LATgdt = 0.0;
            ALTgdt = R - WGS84_A;
        }
        double lonDeg = Math.toDegrees(LONgdt);
        if (lonDeg > 180.0) {
            lonDeg -= 360.0;
        }
        double latDeg = Math.toDegrees(LATgdt);
        if (latDeg > 90.0) {
            latDeg -= 180.0;
        }

        return new Point2D.Double(lonDeg, latDeg);
    }

}
