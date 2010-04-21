package org.esa.beam.chris.operators;

import org.esa.beam.chris.util.math.internal.Pow;
import org.esa.beam.framework.datamodel.RationalFunctionModel;

import java.util.Arrays;
import java.util.List;

/**
 * Geometry calculator.
 * <p/>
 * NOTE: This is largely unoptimized code.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class GeometryCalculator {

    private static final double HALF_PI = Math.PI / 2.0;

    private static final int SLOW_DOWN_FACTOR = 5;
    private static final double JD2001 = TimeConverter.julianDate(2001, 0, 1);

    // constants used for array indexes, so the code is more readable.
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final int U = 3;
    private static final int V = 4;
    private static final int W = 5;

    // five images for each acquisition
    private static final int IMAGE_COUNT = 5;

    private final IctDataRecord ictData;
    private final List<GpsDataRecord> gpsData;
    private final AcquisitionInfo info;
    private final GCP[] gcps;

    private final ModeCharacteristics modeCharacteristics;
    private final int rowCount;
    private final int colCount;

    private final double[][] lats;
    private final double[][] lons;
    private final double[][] vaas;
    private final double[][] vzas;
    private double[][] pitches;
    private double[][] rolls;

    public GeometryCalculator(IctDataRecord ictData, List<GpsDataRecord> gpsData, AcquisitionInfo info, GCP[] gcps) {
        this.ictData = ictData;
        this.gpsData = gpsData;
        this.info = info;
        this.gcps = gcps;

        modeCharacteristics = ModeCharacteristics.get(info.getMode());
        rowCount = modeCharacteristics.getRowCount();
        colCount = modeCharacteristics.getColCount();

        lats = new double[rowCount][colCount];
        lons = new double[rowCount][colCount];
        vaas = new double[rowCount][colCount];
        vzas = new double[rowCount][colCount];

        pitches = new double[rowCount][colCount];
        rolls = new double[rowCount][colCount];
    }

    void calculate(boolean useTargetAltitude) {
        // TODO: rq/rq -  REVISE CODE BELOW
        // The last element of ict_njd corresponds to the acquisition setup time, that occurs 390s before the start of acquisition.
        final double acquisitionSetupTime = ictData.ict1 - (10.0 + 390.0) / TimeConverter.SECONDS_PER_DAY - JD2001;
        double[] ict_njd = {
                ictData.ict1 - JD2001,
                ictData.ict2 - JD2001,
                ictData.ict3 - JD2001,
                ictData.ict4 - JD2001,
                ictData.ict5 - JD2001,
                acquisitionSetupTime
        };

        double[] T_ict = Arrays.copyOfRange(ict_njd, 0, IMAGE_COUNT);

        // We are left with all elements but the last GPS
        // and q is where stores AngVel half, and losing data
        // GPS is not a problem.
        final int numGPS = gpsData.size() - 2;
        double[] gps_njd = new double[numGPS];
        for (int i = 0; i < gps_njd.length; i++) {
            gps_njd[i] = gpsData.get(i).jd - JD2001;
        }

        // ---- Critical Times ---------------------------------------------

        double[] T_ini = new double[IMAGE_COUNT]; // imaging start time (imaging lasts ~9.5s in every mode)
        for (int i = 0; i < T_ini.length; i++) {
            T_ini[i] = ict_njd[i] - (modeCharacteristics.getTimePerImage() / 2.0) / TimeConverter.SECONDS_PER_DAY;
        }

        //---- determine per-Line Time Frame -----------------------------------

        // Time elapsed since imaging start at each image line
        double[] T_lin = new double[modeCharacteristics.getRowCount()];
        for (int line = 0; line < T_lin.length; line++) {
            T_lin[line] = (line * modeCharacteristics.getTotalTimePerLine() + modeCharacteristics.getIntegrationTimePerLine() / 2) / TimeConverter.SECONDS_PER_DAY;
            // +TpL/2 is added to set the time at the middle of the integration time, i.e. pixel center
        }

        double[][] T_img = new double[modeCharacteristics.getRowCount()][IMAGE_COUNT];
        for (int line = 0; line < modeCharacteristics.getRowCount(); line++) {
            for (int img = 0; img < IMAGE_COUNT; img++) {
                T_img[line][img] = T_ini[img] + T_lin[line];
            }
        }

        double[] trjT = new double[1 + IMAGE_COUNT * modeCharacteristics.getRowCount()];
        trjT[0] = ict_njd[5];
        int Tindex = 1;
        for (int img = 0; img < IMAGE_COUNT; img++) {
            for (int line = 0; line < modeCharacteristics.getRowCount(); line++) {
                trjT[Tindex] = T_img[line][img];
                Tindex++;
            }
        }

        // Set the indices of T that correspond to critical times (integration start and stop) for each image

        // The first element of T corresponds to the acquisition-setup time, so Tini[0] must skip element 0 of T
        int[] iniT = new int[IMAGE_COUNT];
        int[] Tend = new int[IMAGE_COUNT];
        for (int img = 0; img < IMAGE_COUNT; img++) {
            iniT[img] = modeCharacteristics.getRowCount() * img + 1;
            Tend[img] = iniT[img] + modeCharacteristics.getRowCount() - 1;
        }
        final int Tfix = 0; // Index corresponding to the time of fixing the orbit


        // ========================================================================
        // ===                     Inertial Coordinates                         ===
        // ==v==v== ==v== Converts coordinates from ECEF to ECI ==v==v== ==v==v== =

        // Pos/Vel with Time from Telemetry

        double[][] eci = new double[gpsData.size()][6];
        for (int i = 0; i < gpsData.size(); i++) {
            GpsDataRecord gpsDataRecord = gpsData.get(i);
            // position and velocity is given in meters,
            // we transform to km in order to keep the values smaller. from now all distances in Km
            double[] ecef = {
                    gpsDataRecord.posX / 1000.0, gpsDataRecord.posY / 1000.0, gpsDataRecord.posZ / 1000.0,
                    gpsDataRecord.velX / 1000.0, gpsDataRecord.velY / 1000.0, gpsDataRecord.velZ / 1000.0
            };
            double gst = TimeConverter.jdToGST(gpsDataRecord.jd);
            CoordinateConverter.ecefToEci(gst, ecef, eci[i]);
        }

        // =======================================================================
        // ===                  Data to per-Line Time Frame                    ===
        // =======================================================================
        // ---- Interpolate GPS ECI position/velocity to per-line time -------

        double[][] transposedEci = VectorMath.transpose(Arrays.copyOf(eci, numGPS));
        double[] trjX = interpolate(gps_njd, transposedEci[X], trjT);
        double[] trjY = interpolate(gps_njd, transposedEci[Y], trjT);
        double[] trjZ = interpolate(gps_njd, transposedEci[Z], trjT);
        double[] trjU = interpolate(gps_njd, transposedEci[U], trjT);
        double[] trjV = interpolate(gps_njd, transposedEci[V], trjT);
        double[] trjW = interpolate(gps_njd, transposedEci[W], trjT);

        double[] iR = new double[trjT.length];
        for (int i = 0; i < iR.length; i++) {
            iR[i] = Math.sqrt(trjX[i] * trjX[i] + trjY[i] * trjY[i] + trjZ[i] * trjZ[i]);
        }

        // ==v==v== Get Orbital Plane Vector ==================================================
        // ---- Calculates normal vector to orbital plane --------------------------
        double[][] uWop = VectorMath.unitVectors(VectorMath.vectorProducts(trjX, trjY, trjZ, trjU, trjV, trjW));

        // Fixes orbital plane vector to the corresponding point on earth at the time of acquistion setup
        double gst_opv = TimeConverter.jdToGST(trjT[Tfix] + JD2001);
        double[] uWecf = CoordinateConverter.eciToEcef(gst_opv, uWop[Tfix], new double[3]);

        double[][] uW = new double[trjT.length][3];
        for (int i = 0; i < trjT.length; i++) {
            double gst = TimeConverter.jdToGST(trjT[i] + JD2001);
            CoordinateConverter.ecefToEci(gst, uWecf, uW[i]);
        }

        // ==v==v== Get Angular Velocity ======================================================
        // Angular velocity is not really used in the model, except the AngVel at orbit fixation time (iAngVel[0])

        final double[] gpsSecs = new double[gpsData.size()];
        final double[] eciX = new double[gpsData.size()];
        final double[] eciY = new double[gpsData.size()];
        final double[] eciZ = new double[gpsData.size()];
        for (int i = 0; i < gpsData.size(); i++) {
            gpsSecs[i] = gpsData.get(i).secs;
            eciX[i] = eci[i][X];
            eciY[i] = eci[i][Y];
            eciZ[i] = eci[i][Z];
        }
        final double[] AngVelRaw = VectorMath.angularVelocities(gpsSecs, eciX, eciY, eciZ);
        final double[] AngVelRawSubset = Arrays.copyOfRange(AngVelRaw, 0, numGPS);
        SimpleSmoother smoother = new SimpleSmoother(5);
        final double[] AngVel = new double[AngVelRawSubset.length];
        smoother.smooth(AngVelRawSubset, AngVel);
        double[] iAngVel = interpolate(gps_njd, AngVel, trjT);

        // ===== Process the correct image ==========================================

        final int imageIndex = info.getChronologicalImageNumber();

        // ---- Target Coordinates in ECI using per-Line Time -------------------
        final double targetAltitude = info.getTargetAlt();
        final double[] TGTecf = CoordinateConverter.wgsToEcef(info.getTargetLon(),
                                                              info.getTargetLat(), targetAltitude,
                                                              new double[3]);

        // Case with Moving Target for imaging time
        double[][] iTGT0 = new double[trjT.length][3];
        for (int i = 0; i < iTGT0.length; i++) {
            double gst = TimeConverter.jdToGST(trjT[i] + JD2001);
            CoordinateConverter.ecefToEci(gst, TGTecf, iTGT0[i]);
        }

        // ==v==v== Rotates TGT to perform scanning ======================================

        for (int i = 0; i < rowCount; i++) {
            final double x = uW[iniT[imageIndex] + i][X];
            final double y = uW[iniT[imageIndex] + i][Y];
            final double z = uW[iniT[imageIndex] + i][Z];
            final double a = iAngVel[0] / SLOW_DOWN_FACTOR * (T_img[i][imageIndex] - T_ict[imageIndex]) * TimeConverter.SECONDS_PER_DAY;
            final Quaternion q;
            if (imageIndex % 2 == 0) {
                q = Quaternion.createQuaternion(x, y, z, a);
            } else {
                q = Quaternion.createQuaternion(x, y, z, -a);
            }
            q.transform(iTGT0[iniT[imageIndex] + i], iTGT0[iniT[imageIndex] + i]);
        }

        final int bestGcpIndex = findBestGCP(rowCount / 2, rowCount / 2);
        if (bestGcpIndex != -1) {
            final GCP gcp = gcps[bestGcpIndex];

            /**
             * 0. Calculate GCP position in ECEF
             */
            final double[] GCP_ecf = new double[3];
            CoordinateConverter.wgsToEcef(gcp.getLon(), gcp.getLat(), gcp.getAlt(), GCP_ecf);

            /**
             * 1. Transform nominal Moving Target to ECEF
             */
            // Transform Moving Target to ECF in order to find the point closest to GCP0
            // iTGT0_ecf = eci2ecf(T+jd0, iTGT0[X,*], iTGT0[Y,*], iTGT0[Z,*])
            final double[][] iTGT0_ecf = new double[iTGT0.length][3];
            for (int i = 0; i < iTGT0.length; i++) {
                final double gst = TimeConverter.jdToGST(trjT[i] + JD2001);
                CoordinateConverter.eciToEcef(gst, iTGT0[i], iTGT0_ecf[i]);
            }

            /**
             * 2. Find time offset dT
             */
            double minDiff = Double.MAX_VALUE;
            double tmin = Double.MAX_VALUE;
            int wmin = -1;
            for (int i = iniT[imageIndex]; i <= Tend[imageIndex]; i++) {
                double[] pos = iTGT0_ecf[i];
                final double diff = Math.sqrt(
                        Pow.pow2(pos[X] - GCP_ecf[X]) + Pow.pow2(pos[Y] - GCP_ecf[Y]) + Pow.pow2(
                                pos[Z] - GCP_ecf[Z]));
                if (diff < minDiff) {
                    minDiff = diff;
                    tmin = trjT[i];
                    wmin = i; // This is necessary in order to recompute the times more easily
                }
            }

            final double dY;
            if (info.isBackscanning()) {
                dY = (wmin % modeCharacteristics.getRowCount()) - (modeCharacteristics.getRowCount() - gcp.getY() + 0.5);
            } else {
                dY = (wmin % modeCharacteristics.getRowCount()) - (gcp.getY() + 0.5);
            }
            final double dT = (dY * modeCharacteristics.getTotalTimePerLine()) / TimeConverter.SECONDS_PER_DAY;

            /**
             * 3. Update T[]: add dT to all times in T[].
             */
            for (int i = 0; i < trjT.length; i++) {
                final double newT = trjT[i] + dT; //tmin + (mode.getDt() * (i - wmin)) / TimeConverter.SECONDS_PER_DAY;
                trjT[i] = newT;
            }
            for (int line = 0; line < modeCharacteristics.getRowCount(); line++) {
                T_img[line][imageIndex] += dT;
            }

            /**
             * 4. Calculate GCP position in ECI for updated times T[]
             */
            // T_GCP = T[wmin]			; Assigns the acquisition time to the GCP
            // GCP_eci = ecf2eci(T+jd0, GCP_ecf.X, GCP_ecf.Y, GCP_ecf.Z, units = GCP_ecf.units)	; Transform GCP coords to ECI for every time in the acquisition
            final double[][] GCP_eci = new double[trjT.length][3];
            for (int i = 0; i < trjT.length; i++) {
                final double gst = TimeConverter.jdToGST(trjT[i] + JD2001);
                CoordinateConverter.ecefToEci(gst, GCP_ecf, GCP_eci[i]);
            }
            // NOTE: rq/rq - code below is duplicated
            /**
             * 5. Interpolate satellite positions & velocities for updated times T[]
             */
            transposedEci = VectorMath.transpose(Arrays.copyOf(eci, numGPS));
            trjX = interpolate(gps_njd, transposedEci[0], trjT);
            trjY = interpolate(gps_njd, transposedEci[1], trjT);
            trjZ = interpolate(gps_njd, transposedEci[2], trjT);
            trjU = interpolate(gps_njd, transposedEci[3], trjT);
            trjV = interpolate(gps_njd, transposedEci[4], trjT);
            trjW = interpolate(gps_njd, transposedEci[5], trjT);

            iR = new double[trjT.length];
            for (int i = 0; i < iR.length; i++) {
                iR[i] = Math.sqrt(trjX[i] * trjX[i] + trjY[i] * trjY[i] + trjZ[i] * trjZ[i]);
            }

            // ==v==v== Get Orbital Plane Vector ==================================================
            // ---- Calculates normal vector to orbital plane --------------------------
            uWop = VectorMath.unitVectors(VectorMath.vectorProducts(trjX, trjY, trjZ, trjU, trjV, trjW));

            // Fixes orbital plane vector to the corresponding point on earth at the time of acquistion setup
            gst_opv = TimeConverter.jdToGST(trjT[Tfix] + JD2001);
            uWecf = new double[3];
            CoordinateConverter.eciToEcef(gst_opv, uWop[Tfix], uWecf);

            uW = new double[trjT.length][3];
            for (int i = 0; i < trjT.length; i++) {
                double gst = TimeConverter.jdToGST(trjT[i] + JD2001);
                CoordinateConverter.ecefToEci(gst, uWecf, uW[i]);
            }

            // ==v==v== Get Angular Velocity ======================================================
            // Angular velocity is not really used in the model, except the AngVel at orbit fixation time (iAngVel[0])

            iAngVel = interpolate(gps_njd, AngVel, trjT);
            // NOTE: rq/rq - code above is duplicated

            for (int i = 0; i < rowCount; i++) {
                final double x = uW[iniT[imageIndex] + i][X];
                final double y = uW[iniT[imageIndex] + i][Y];
                final double z = uW[iniT[imageIndex] + i][Z];
                final double a = iAngVel[0] / SLOW_DOWN_FACTOR * (T_img[i][imageIndex] - tmin) * TimeConverter.SECONDS_PER_DAY;
                final Quaternion q;
                if (imageIndex % 2 == 0) {
                    q = Quaternion.createQuaternion(x, y, z, a);
                } else {
                    q = Quaternion.createQuaternion(x, y, z, -a);
                }
                q.transform(GCP_eci[iniT[imageIndex] + i], GCP_eci[iniT[imageIndex] + i]);
            }
            iTGT0 = GCP_eci;
        }

        final double[][] target = iTGT0;
        // TODO: rq/rq -  REVISE CODE ABOVE

        /*
           CALCULATE VIEWING GEOMETRY AND POSITIONS
           ========================================
        */

        // extract the part of the satellite trajectory, which is relevant for the image
        final double[] satX = new double[rowCount];
        final double[] satY = new double[rowCount];
        final double[] satZ = new double[rowCount];
        final double[] satT = new double[rowCount];
        for (int i = 0; i < satT.length; i++) {
            satT[i] = trjT[iniT[imageIndex] + i];
            satX[i] = trjX[iniT[imageIndex] + i];
            satY[i] = trjY[iniT[imageIndex] + i];
            satZ[i] = trjZ[iniT[imageIndex] + i];
        }

        // calculate pointing vectors
        final double[][] pointings = new double[rowCount][3];
        for (int i = 0; i < rowCount; i++) {
            final double[] targetPos = target[iniT[imageIndex] + i];
            final ViewingGeometry viewingGeometry =
                    ViewingGeometry.create(targetPos[X], targetPos[Y], targetPos[Z], satX[i], satY[i], satZ[i]);

            pointings[i][X] = viewingGeometry.x;
            pointings[i][Y] = viewingGeometry.y;
            pointings[i][Z] = viewingGeometry.z;
        }
        VectorMath.unitVectors(pointings);

        // calculate yaw axes
        final double[][] yawAxes = new double[rowCount][3];
        for (int i = 0; i < rowCount; i++) {
            yawAxes[i][X] = trjX[iniT[imageIndex] + i];
            yawAxes[i][Y] = trjY[iniT[imageIndex] + i];
            yawAxes[i][Z] = trjZ[iniT[imageIndex] + i];
        }
        VectorMath.unitVectors(yawAxes);

        // calculate pitch axes
        final double[][] pitchAxes = new double[rowCount][3];
        for (int i = 0; i < rowCount; i++) {
            pitchAxes[i][X] = uWop[iniT[imageIndex] + i][X];
            pitchAxes[i][Y] = uWop[iniT[imageIndex] + i][Y];
            pitchAxes[i][Z] = uWop[iniT[imageIndex] + i][Z];
        }

        // calculate roll axes, center pitch and rolls
        final double[][] rollAxes = VectorMath.vectorProducts(pitchAxes, yawAxes, new double[rowCount][3]);
        final double[][] sp = VectorMath.vectorProducts(pointings, pitchAxes, new double[rowCount][3]);
        final double[][] sl = VectorMath.vectorProducts(pitchAxes, sp, new double[rowCount][3]);
        final double[][] sm = VectorMath.vectorProducts(sl, pointings, new double[rowCount][3]);
        final double[] rollSign = new double[rowCount];
        for (int i = 0; i < modeCharacteristics.getRowCount(); i++) {
            rollSign[i] = Math.signum(sm[i][X] / sp[i][X] + sm[i][Y] / sp[i][Y] + sm[i][Z] / sp[i][Z]);
        }

        double rollDelta = 0.0;
        if (bestGcpIndex != -1) {
            // calculate the roll offset due to the GCP not being in the middle of the CCD
            final int nC2;
            if (info.getMode() != 5) {
                nC2 = modeCharacteristics.getColCount() / 2;
            } else {
                nC2 = modeCharacteristics.getColCount() - 1;
            }
            final GCP gcp = gcps[bestGcpIndex];
            rollDelta = (nC2 - gcp.getX()) * modeCharacteristics.getIfov();
        }

        final double[] centerPitchAngles = new double[rowCount];
        final double[] centerRollAngles = new double[rowCount];
        for (int i = 0; i < rowCount; i++) {
            centerPitchAngles[i] = HALF_PI - VectorMath.angle(sp[i], yawAxes[i]);
            centerRollAngles[i] = rollSign[i] * VectorMath.angle(sl[i], pointings[i]);
            centerRollAngles[i] += rollDelta;
        }

        // calculate pitch and rolls for each image pixel
        calculatePitchAndRollAngles(centerPitchAngles, centerRollAngles);
        if (gcps.length > 2) {
            refinePitchAndRollAngles(satT, satX, satY, satZ, pitchAxes, yawAxes);
        }

        // calculate position and viewing geometry for each image pixel
        final PositionCalculator calculator = new PositionCalculator(useTargetAltitude ? targetAltitude : 0.0);
        calculator.calculatePositions(satT, satX, satY, satZ, pitchAxes, rollAxes, yawAxes, pitches, rolls, lons,
                                      lats, vaas, vzas);
    }

    final double getLon(int x, int y) {
        return lons[y][x];
    }

    final double getLat(int x, int y) {
        return lats[y][x];
    }

    final double getVaa(int x, int y) {
        return vaas[y][x];
    }

    final double getVza(int x, int y) {
        return vzas[y][x];
    }

    final double getPitch(int x, int y) {
        return pitches[y][x];
    }

    final double getRoll(int x, int y) {
        return rolls[y][x];
    }

    private void refinePitchAndRollAngles(double[] satT,
                                          double[] satX,
                                          double[] satY,
                                          double[] satZ,
                                          double[][] pitchAxes,
                                          double[][] yawAxes) {
        final int gcpCount = gcps.length;
        final double[] x = new double[gcpCount];
        final double[] y = new double[gcpCount];
        final double[] deltaPitch = new double[gcpCount];
        final double[] deltaRoll = new double[gcpCount];
        final double[] pitchRoll = new double[2];

        for (int i = 0; i < gcpCount; i++) {
            final GCP gcp = gcps[i];
            final int row = gcp.getRow();
            final int col = gcp.getCol();
            final double actT = satT[row];
            final double actX = satX[row];
            final double actY = satY[row];
            final double actZ = satZ[row];
            final double[] pitchAxis = pitchAxes[row];
            final double[] yawAxis = yawAxes[row];

            calculatePitchRoll(actT, actX, actY, actZ, gcp, pitchAxis, yawAxis, pitchRoll);

            x[i] = gcp.getX();
            y[i] = gcp.getY();
            deltaPitch[i] = pitchRoll[0] - pitches[row][col];
            deltaRoll[i] = pitchRoll[1] - rolls[row][col];
        }

        final RationalFunctionModel deltaPitchModel = new RationalFunctionModel(2, 0, x, y, deltaPitch);
        final RationalFunctionModel deltaRollModel = new RationalFunctionModel(2, 0, x, y, deltaRoll);

        for (int k = 0; k < pitches.length; k++) {
            final double[] rowPitchAngles = pitches[k];
            final double[] rowRollAngles = rolls[k];
            for (int l = 0; l < rowPitchAngles.length; l++) {
                rowPitchAngles[l] += deltaPitchModel.getValue(l + 0.5, k + 0.5);
                rowRollAngles[l] += deltaRollModel.getValue(l + 0.5, k + 0.5);
            }
        }
    }

    private static void calculatePitchRoll(double satT, double satX, double satY, double satZ, GCP gcp,
                                           double[] pitchAxis, double[] yawAxis, double[] pitchRoll) {
        final double[] gcpPos = CoordinateConverter.wgsToEcef(gcp.getLon(), gcp.getLat(), gcp.getAlt(), new double[3]);
        CoordinateConverter.ecefToEci(TimeConverter.jdToGST(satT + JD2001), gcpPos, gcpPos);

        final double dx = gcpPos[X] - satX;
        final double dy = gcpPos[Y] - satY;
        final double dz = gcpPos[Z] - satZ;
        final double[] pointing = VectorMath.unitVector(new double[]{dx, dy, dz});
        final double[] sp = VectorMath.vectorProduct(pointing, pitchAxis, new double[3]);
        final double[] sl = VectorMath.vectorProduct(pitchAxis, sp, new double[3]);
        final double[] rollAxis = VectorMath.unitVector(VectorMath.vectorProduct(sl, pointing, new double[3]));
        final double total = rollAxis[X] / sp[X] + rollAxis[Y] / sp[Y] + rollAxis[Z] / sp[Z];

        final double pitch = HALF_PI - VectorMath.angle(sp[X],
                                                        sp[Y],
                                                        sp[Z],
                                                        yawAxis[X],
                                                        yawAxis[Y],
                                                        yawAxis[Z]);
        final double rollSign = Math.signum(total);
        final double roll = rollSign * VectorMath.angle(sl[X],
                                                        sl[Y],
                                                        sl[Z],
                                                        pointing[X],
                                                        pointing[Y],
                                                        pointing[Z]);

        pitchRoll[0] = pitch;
        pitchRoll[1] = roll;
    }

    private void calculatePitchAndRollAngles(double[] rowCenterPitches, double[] rowCenterRolls) {

        final double fov = modeCharacteristics.getFov();
        final double ifov = modeCharacteristics.getIfov();

        final double[] deltas = new double[colCount];
        if (info.getMode() == 5) {
            // for Mode 5 the last pixel points to the target, i.e. delta is zero for the last pixel
            for (int i = 0; i < deltas.length; i++) {
                deltas[i] = (i + 0.5) * ifov - fov;
            }
        } else {
            final double halfFov = fov / 2.0;
            for (int i = 0; i < deltas.length; i++) {
                deltas[i] = (i + 0.5) * ifov - halfFov;
            }
        }
        for (int l = 0; l < rowCount; l++) {
            for (int c = 0; c < colCount; c++) {
                pitches[l][c] = rowCenterPitches[l];
                rolls[l][c] = rowCenterRolls[l] + deltas[c];
            }
        }
    }

    /**
     * Finds the GCP which is nearest to some given pixel coordinates (x, y).
     *
     * @param x the x pixel coordinate.
     * @param y the y pixel coordinate.
     *
     * @return the index of the GCP nearest to ({@code x}, {@code y}) or {@code -1},
     *         if no such GCP could be found.
     */
    private int findBestGCP(double x, double y) {
        int bestIndex = -1;
        double bestDelta = Double.POSITIVE_INFINITY;

        for (int i = 0; i < gcps.length; i++) {
            final double dx = gcps[i].getX() - x;
            final double dy = gcps[i].getY() - y;
            final double delta = dx * dx + dy * dy;
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    /**
     * Interpolates a set of values y(x) using a natural spline.
     *
     * @param x  the original x values.
     * @param y  the original y values.
     * @param x2 the x values used for interpolation.
     *
     * @return the interpolated y values, which is an array of length {@code x2.length}.
     */
    private static double[] interpolate(double[] x, double[] y, double[] x2) {
        final PolynomialSplineFunction splineFunction = new SplineInterpolator().interpolate(x, y);
        final double[] y2 = new double[x2.length];
        for (int i = 0; i < x2.length; i++) {
            y2[i] = splineFunction.value(x2[i]);
        }
        return y2;
    }
}
