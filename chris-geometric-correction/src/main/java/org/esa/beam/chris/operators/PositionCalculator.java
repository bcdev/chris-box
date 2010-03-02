package org.esa.beam.chris.operators;

import java.awt.geom.Point2D;

class PositionCalculator {

    private static final double R = 6378.137;
    private static final double F = 1.0 / 298.257223563;
    private static final double JD2001 = TimeConverter.julianDate(2001, 0, 1);

    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    public static void calculatePositions(int rowCount,
                                          int colCount,
                                          double[][] pitchAngles, // [nLines][nCols]
                                          double[][] rollAngles, // [nLines][nCols]
                                          double[][] pitchAxes, //[3][nlines]
                                          double[][] rollAxes,  //[3][nlines]
                                          double[][] yawAxes,   //[3][nlines]
                                          double targetAltitude,
                                          double[] satX,
                                          double[] satY,
                                          double[] satZ,
                                          double[] satT,
                                          double[][] lons, // [nLines][nCols]
                                          double[][] lats // [nLines][nCols]
    ) {
        final double[][][] pointings = new double[rowCount][colCount][3];

        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                // 1. initialize the pointing
                final double[] pointing = pointings[i][j];
                for (int k = 0; k < 3; k++) {
                    pointing[k] = -yawAxes[k][i];
                }

                // 2. rotate the pointing around the pitch axis
                final Quaternion pitchRotation = createQuaternion(i, pitchAxes, -pitchAngles[i][j]);
                pitchRotation.transform(pointing, pointing);

                // 3. rotate the roll axis around the pitch axis
                final double[] rollAxis = new double[]{rollAxes[X][i], rollAxes[Y][i], rollAxes[Z][i]};
                pitchRotation.transform(rollAxis, rollAxis);

                // 4. rotate pointing around roll axis
                final Quaternion rollRotation = createQuaternion(rollAxis, rollAngles[i][j]);
                rollRotation.transform(pointing, pointing);
            }
        }

        final double[] earthRadii = new double[]{
                R + targetAltitude, R + targetAltitude, (1.0 - F) * R + targetAltitude
        };
        final double[] earthCenter = new double[3];
        final double[] pos = new double[3];

        for (int i = 0; i < rowCount; i++) {
            final double gst = TimeConverter.jdToGST(satT[i] + JD2001);
            for (int j = 0; j < colCount; j++) {
                pos[X] = satX[i];
                pos[Y] = satY[i];
                pos[Z] = satZ[i];

                // todo - intersect with DEM instead
                Intersector.intersect(pos, pointings[i][j], earthCenter, earthRadii);
                EcefEciConverter.eciToEcef(gst, pos, pos);

                final Point2D point = Conversions.ecef2wgs(pos[X], pos[Y], pos[Z]);
                lons[i][j] = point.getX();
                lats[i][j] = point.getY();
            }
        }
    }

    private static Quaternion createQuaternion(int axisIndex, double[][] axes, double angle) {
        return Quaternion.createQuaternion(axes[0][axisIndex], axes[1][axisIndex], axes[2][axisIndex], angle);
    }

    private static Quaternion createQuaternion(double[] axis, double angle) {
        return Quaternion.createQuaternion(axis[0], axis[1], axis[2], angle);
    }
}
