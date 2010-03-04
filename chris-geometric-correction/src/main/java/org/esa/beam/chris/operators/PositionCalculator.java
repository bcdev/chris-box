package org.esa.beam.chris.operators;

class PositionCalculator {

    private static final double JD2001 = TimeConverter.julianDate(2001, 0, 1);

    private static final double WGS84_INVERSE_FLATTENING = 1.0 / 298.257223563;
    private static final double WGS84_SEMI_MAJOR = 6378.137;
    private static final double WGS84_SEMI_MINOR = WGS84_SEMI_MAJOR * (1.0 - WGS84_INVERSE_FLATTENING);
    private static final double[] WGS84_CENTER = new double[3];

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    private final double[] semiAxes;

    PositionCalculator(double targetAltitude) {
        final double semiMajor = WGS84_SEMI_MAJOR + targetAltitude;
        final double semiMinor = WGS84_SEMI_MINOR + targetAltitude;
        semiAxes = new double[]{semiMajor, semiMajor, semiMinor};
    }

    void calculatePositions(
            double[][] pitchAngles, // [nLines][nCols]
            double[][] rollAngles, // [nLines][nCols]
            double[][] pitchAxes, //[nlines][3]
            double[][] rollAxes,  //[nlines][3]
            double[][] yawAxes,   //[nlines][3]
            double[] satX,
            double[] satY,
            double[] satZ,
            double[] satT,
            double[][] lons, // [nLines][nCols]
            double[][] lats, // [nLines][nCols]
            double[][] vaa,
            double[][] vza
    ) {
        final int rowCount = pitchAngles.length;
        final int colCount = pitchAngles[0].length;
        final double[][][] pointings = new double[rowCount][colCount][3];

        for (int i = 0; i < rowCount; i++) {
            final double[] pitchAxis = pitchAxes[i];
            final double[] rollAxis = new double[3];
            final double[] yawAxis = yawAxes[i];
            for (int j = 0; j < colCount; j++) {
                // 1. initialize the pointing
                final double[] pointing = pointings[i][j];
                for (int k = 0; k < 3; k++) {
                    pointing[k] = -yawAxis[k];
                }
                // 2. rotate the pointing around the pitch axis
                final Quaternion pitchRotation = createQuaternion(pitchAxis, -pitchAngles[i][j]);
                pitchRotation.transform(pointing, pointing);

                // 3. rotate the roll axis around the pitch axis
                pitchRotation.transform(rollAxes[i], rollAxis);

                // 4. rotate pointing around roll axis
                final Quaternion rollRotation = createQuaternion(rollAxis, rollAngles[i][j]);
                rollRotation.transform(pointing, pointing);
            }
        }

        final double[] point = new double[3];
        for (int i = 0; i < rowCount; i++) {
            final double gst = TimeConverter.jdToGST(satT[i] + JD2001);
            for (int j = 0; j < colCount; j++) {
                point[X] = satX[i];
                point[Y] = satY[i];
                point[Z] = satZ[i];

                Intersector.intersect(point, pointings[i][j], WGS84_CENTER, semiAxes);
                final ViewingGeometry viewingGeometry = ViewingGeometry.create(point[X], point[Y], point[Z], satX[i],
                                                                               satY[i], satZ[i]);
                vaa[i][j] = Math.toDegrees(viewingGeometry.azimuth);
                vza[i][j] = Math.toDegrees(viewingGeometry.zenith);

                CoordinateConverter.eciToEcef(gst, point, point);
                final double[] wgs = CoordinateConverter.ecefToWgs(point[X], point[Y], point[Z], new double[3]);
                lons[i][j] = wgs[X];
                lats[i][j] = wgs[Y];
            }
        }
    }

    private static Quaternion createQuaternion(double[] axis, double angle) {
        return Quaternion.createQuaternion(axis[0], axis[1], axis[2], angle);
    }
}
