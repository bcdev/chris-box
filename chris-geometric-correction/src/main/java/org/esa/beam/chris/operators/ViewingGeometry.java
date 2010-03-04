package org.esa.beam.chris.operators;

class ViewingGeometry {

    private static final double TWO_PI = 2.0 * Math.PI;
    
    final double x;
    final double y;
    final double z;
    final double azimuth;
    final double zenith;

    private ViewingGeometry(double x, double y, double z, double azimuth, double zenith) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.azimuth = azimuth;
        this.zenith = zenith;
    }

    static ViewingGeometry create(double targetX, double targetY, double targetZ, double satX, double satY,
                                  double satZ) {
        // the components of the pointing vector
        final double x = targetX - satX;
        final double y = targetY - satY;
        final double z = targetZ - satZ;

        final double[] wgs = CoordinateConverter.ecefToWgs(targetX, targetY, targetZ, new double[3]);
        final double phi = Math.toRadians(wgs[1]);
        final double sinPhi = Math.sin(phi);
        final double cosPhi = Math.cos(phi);
        final double sinTheta = targetY / Math.sqrt(targetX * targetX + targetY * targetY);
        final double cosTheta = targetX / Math.sqrt(targetX * targetX + targetY * targetY);
        final double topS = cosPhi * z - sinPhi * cosTheta * x - sinPhi * sinTheta * y;
        final double topE = sinTheta * x - cosTheta * y;
        final double topZ = -cosPhi * cosTheta * x - cosPhi * sinTheta * y - sinPhi * z;
        final double zenith = Math.PI / 2.0 - Math.asin(topZ / Math.sqrt(x * x + y * y + z * z));

        double azimuth = Math.atan(-topE / topS);
        if (topS > 0) {
            azimuth += Math.PI;
        }
        if (topS < 0) {
            azimuth += TWO_PI;
        }
        azimuth %= TWO_PI;

        return new ViewingGeometry(x, y, z, azimuth, zenith);
    }
}
