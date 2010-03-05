package org.esa.beam.chris.operators;

import static java.lang.Math.*;

/**
 * Viewing geometry.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class ViewingGeometry {

    private static final double HALF_PI = PI / 2.0;
    private static final double TWO_PI = 2.0 * PI;

    // components of the pointing vector
    final double x;
    final double y;
    final double z;
    // viewing angles
    final double azimuth;
    final double zenith;

    private ViewingGeometry(double x, double y, double z, double azimuth, double zenith) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.azimuth = azimuth;
        this.zenith = zenith;
    }

    static ViewingGeometry create(double posX, double posY, double posZ, double satX, double satY, double satZ) {
        // the components of the pointing vector
        final double x = posX - satX;
        final double y = posY - satY;
        final double z = posZ - satZ;

        final double[] wgs = CoordinateConverter.ecefToWgs(posX, posY, posZ, new double[3]);
        final double phi = Math.toRadians(wgs[1]);
        final double sinPhi = sin(phi);
        final double cosPhi = cos(phi);
        final double sinTheta = posY / sqrt(posX * posX + posY * posY);
        final double cosTheta = posX / sqrt(posX * posX + posY * posY);
        final double topS = cosPhi * z - sinPhi * cosTheta * x - sinPhi * sinTheta * y;
        final double topE = cosTheta * y - sinTheta * x;
        final double topZ = cosPhi * cosTheta * x + cosPhi * sinTheta * y + sinPhi * z;
        final double zenith = HALF_PI + asin(topZ / sqrt(x * x + y * y + z * z));

        double azimuth = Math.atan(topE / topS);
        if (topS > 0) {
            azimuth += PI;
        }
        if (topS < 0) {
            azimuth += TWO_PI;
        }
        azimuth %= TWO_PI;

        return new ViewingGeometry(x, y, z, azimuth, zenith);
    }
}
