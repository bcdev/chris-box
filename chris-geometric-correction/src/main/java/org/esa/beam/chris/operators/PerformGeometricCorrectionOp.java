/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.operators.CoordinateUtils.ViewAng;
import org.esa.beam.chris.operators.IctDataRecord.IctDataReader;
import org.esa.beam.chris.operators.TelemetryFinder.Telemetry;
import org.esa.beam.chris.operators.math.PolynomialSplineFunction;
import org.esa.beam.chris.operators.math.SplineInterpolator;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.chris.util.math.internal.Pow;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@OperatorMetadata(alias = "chris.PerformGeometricCorrection",
                  version = "1.0",
                  authors = "Ralf Quast, Marco Zühlke",
                  copyright = "(c) 2009 by Brockmann Consult",
                  description = "Performs the geometric correction for a CHRIS/Proba RCI.")
public class PerformGeometricCorrectionOp extends Operator {

    private static final int SLOW_DOWN = 5; // Slowdown factor

    //Epoch for a reduced Julian Day (all JD values are substracted by this value). 
    //This way the calculations can be performed more efficiently.
    private static final double JD0 = TimeConverter.julianDate(2001, 0, 1);

    // there is a delay of 0.999s between the GPS time tag and the actual time of the reported position/velocity.
    private static final double DELAY = 0.999;

    // Variables used for array subscripting, so the code is more readable.
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    private static final int NUM_IMG = 5;


    ///////////////////////////////////////////////////////////

    @Parameter(label = "Telemetry search path",
               description = "The path of the directory containing the CHRIS telemetry data. If left blank,\n" +
                             "the path of the CHRIS telemetry repository is used.")
    private String telemetrySearchPath;

    @SourceProduct
    private Product sourceProduct;

    private double[][][] igm;
    private AcquisitionInfo info;
    private Band pixelLonBand;
    private Band pixelLatBand;

    ///////////////////////////////////////////////////////////

    @Override
    public void initialize() throws OperatorException {
        final Telemetry telemetry = getTelemetry();
        info = AcquisitionInfo.createAcquisitionInfo(sourceProduct);
        final double dTgps = getDeltaGPS(sourceProduct);

        final IctDataRecord ictData = readIctData(telemetry.getIctFile(), dTgps);
        final List<GpsDataRecord> gpsData = readGpsData(telemetry.getGpsFile(), DELAY, dTgps);
        final ChrisModeConstants mode = ChrisModeConstants.get(info.getMode());

        //////////////////////////
        // Prepare Time Frames
        //////////////////////////

        // The last element of ict_njd corresponds to the acquisition setup time, that occurs 390s before the start of acquisition.
        final double acquisitionSetupTime = ictData.ict1 - (10.0 + 390.0) / TimeConverter.SECONDS_PER_DAY - JD0;
        double[] ict_njd = {
                ictData.ict1 - JD0,
                ictData.ict2 - JD0,
                ictData.ict3 - JD0,
                ictData.ict4 - JD0,
                ictData.ict5 - JD0,
                acquisitionSetupTime
        };

        double[] T_ict = Arrays.copyOfRange(ict_njd, 0, NUM_IMG);

        //---- Nos quedamos con todos los elementos de GPS menos el ultimo ----------------
        //----   ya q es donde se almacena la AngVel media, y perder un dato --------------
        //----   de GPS no es un problema. ------------------------------------------------

        // We are left with all elements but the last GPS
        // and q is where stores AngVel half, and losing data
        // GPS is not a problem.
        final int numGPS = gpsData.size() - 2;
        double[] gps_njd = new double[numGPS];
        for (int i = 0; i < gps_njd.length; i++) {
            gps_njd[i] = gpsData.get(i).jd - JD0;
        }

        // ---- Critical Times ---------------------------------------------

        double[] T_ini = new double[NUM_IMG]; // imaging start time (imaging lasts ~9.5s in every mode)
        double[] T_end = new double[NUM_IMG]; // imaging stop time
        for (int i = 0; i < T_ini.length; i++) {
            T_ini[i] = ict_njd[i] - (mode.getTimg() / 2.0) / TimeConverter.SECONDS_PER_DAY;
            T_end[i] = ict_njd[i] + (mode.getTimg() / 2.0) / TimeConverter.SECONDS_PER_DAY;
        }
//        double T_i = ict_njd[0] - 10 / Conversions.SECONDS_PER_DAY; // "imaging mode" start time
//        double T_e = ict_njd[4] + 10 / Conversions.SECONDS_PER_DAY; // "imaging mode" stop time

        // Searches the closest values in the telemetry to the Critical Times (just for plotting purposses)
        // skipped

        //---- determine per-Line Time Frame -----------------------------------

        // Time elapsed since imaging start at each image line
        double[] T_lin = new double[mode.getNLines()];
        for (int line = 0; line < T_lin.length; line++) {
            T_lin[line] = (line * mode.getDt() + mode.getTpl() / 2) / TimeConverter.SECONDS_PER_DAY;
            // +TpL/2 is added to set the time at the middle of the integration time, i.e. pixel center
        }

        double[][] T_img = new double[mode.getNLines()][NUM_IMG];
        for (int line = 0; line < mode.getNLines(); line++) {
            for (int img = 0; img < NUM_IMG; img++) {
                T_img[line][img] = T_ini[img] + T_lin[line];
            }
        }

        double[] T = new double[1 + NUM_IMG * mode.getNLines()];
        T[0] = ict_njd[5];
        int Tindex = 1;
        for (int img = 0; img < NUM_IMG; img++) {
            for (int line = 0; line < mode.getNLines(); line++) {
                T[Tindex] = T_img[line][img];
                Tindex++;
            }
        }

        //double Tobs = T_e - T_i; // Duration of "imaging mode" for each image. (redundant, should be 20s)
//        double[] Tobs2 = new double[T_end.length]; // Duration of actual imaging for each image
//        for (int i = 0; i < Tobs2.length; i++) {
//            Tobs2[i] = T_end[i] - T_ini[i];
//        }

        // Set the indices of T that correspond to critical times (integration start and stop) for each image

        // The first element of T corresponds to the acquisition-setup time, so Tini[0] must skip element 0 of T
        int[] Tini = new int[NUM_IMG];
        int[] Tend = new int[NUM_IMG];
        for (int img = 0; img < NUM_IMG; img++) {
            Tini[img] = mode.getNLines() * img + 1;
            Tend[img] = Tini[img] + mode.getNLines() - 1;
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
            double[] ecf = {
                    gpsDataRecord.posX / 1000.0, gpsDataRecord.posY / 1000.0, gpsDataRecord.posZ / 1000.0,
                    gpsDataRecord.velX / 1000.0, gpsDataRecord.velY / 1000.0, gpsDataRecord.velZ / 1000.0
            };
            double gst = TimeConverter.jdToGST(gpsDataRecord.jd);
            EcefEciConverter.ecefToEci(gst, ecf, eci[i]);
        }

        // =======================================================================
        // ===                  Data to per-Line Time Frame                    ===
        // =======================================================================
        // ---- Interpolate GPS ECI position/velocity to per-line time -------

        double[] iX = spline(gps_njd, get2ndDim(eci, 0, numGPS), T);
        double[] iY = spline(gps_njd, get2ndDim(eci, 1, numGPS), T);
        double[] iZ = spline(gps_njd, get2ndDim(eci, 2, numGPS), T);
        double[] iVX = spline(gps_njd, get2ndDim(eci, 3, numGPS), T);
        double[] iVY = spline(gps_njd, get2ndDim(eci, 4, numGPS), T);
        double[] iVZ = spline(gps_njd, get2ndDim(eci, 5, numGPS), T);

        double[] iR = new double[T.length];
        for (int i = 0; i < iR.length; i++) {
            iR[i] = Math.sqrt(iX[i] * iX[i] + iY[i] * iY[i] + iZ[i] * iZ[i]);
        }

        // ==v==v== Get Orbital Plane Vector ==================================================
        // ---- Calculates normal vector to orbital plane --------------------------
        double[][] Wop = vect_prod(iX, iY, iZ, iVX, iVY, iVZ);
        double[][] uWop = unit(Wop);

        // Fixes orbital plane vector to the corresponding point on earth at the time of acquistion setup
        double gst_opv = TimeConverter.jdToGST(T[Tfix] + JD0);
        double[] eci_opv = new double[]{uWop[X][Tfix], uWop[Y][Tfix], uWop[Z][Tfix]};
        double[] uWecf = new double[3];
        EcefEciConverter.eciToEcef(gst_opv, eci_opv, uWecf);

        double[][] uW = new double[T.length][3];
        for (int i = 0; i < T.length; i++) {
            double gst = TimeConverter.jdToGST(T[i] + JD0);
            EcefEciConverter.ecefToEci(gst, uWecf, uW[i]);
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
        final double[] AngVelRaw = CoordinateUtils.angVel(gpsSecs, eciX, eciY, eciZ);
        final double[] AngVelRawSubset = Arrays.copyOfRange(AngVelRaw, 0, numGPS);
        SimpleSmoother smoother = new SimpleSmoother(5);
        final double[] AngVel = new double[AngVelRawSubset.length];
        smoother.smooth(AngVelRawSubset, AngVel);
        double[] iAngVel = spline(gps_njd, AngVel, T);

        // ==v==v== Initialize Variables ======================================================

        //double[][] Range = new double[3][mode.getNLines()];
        //double[][] EjeYaw = new double[3][mode.getNLines()];
        //double[][] EjePitch = new double[3][mode.getNLines()];
        //double[][] EjeRoll = new double[3][mode.getNLines()];
        //double[][] SP = new double[3][mode.getNLines()];
        //double[][] SL = new double[3][mode.getNLines()];
        //double[] PitchAng = new double[mode.getNLines()];
        //double[] RollAng = new double[mode.getNLines()];
        //double[][] PitchAngR = new double[mode.getNCols()][mode.getNLines()];
        //double[][] RollAngR = new double[mode.getNCols()][mode.getNLines()];
        //double[] ObsAngAzi = new double[mode.getNLines()];
        //double[] ObsAngZen = new double[mode.getNLines()];

        // ===== Process the correct image ==========================================

        final int img = info.getChronologicalImageNumber();

        // ---- Target Coordinates in ECI using per-Line Time -------------------
        final double TgtAlt = info.getTargetAlt() / 1000;
        final double[] TGTecf = new double[3];
        Conversions.wgsToEcef(info.getTargetLon(), info.getTargetLat(), TgtAlt, TGTecf);

        // Case with Moving Target for imaging time
        double[][] iTGT0 = new double[T.length][3];
        for (int i = 0; i < iTGT0.length; i++) {
            double gst = TimeConverter.jdToGST(T[i] + JD0);
            EcefEciConverter.ecefToEci(gst, TGTecf, iTGT0[i]);
        }

        // ==v==v== Rotates TGT to perform scanning ======================================

        for (int i = 0; i < mode.getNLines(); i++) {
            final double x = uW[Tini[img] + i][X];
            final double y = uW[Tini[img] + i][Y];
            final double z = uW[Tini[img] + i][Z];
            final double a = Math.pow(-1.0,
                                      img) * iAngVel[0] / SLOW_DOWN * (T_img[i][img] - T_ict[img]) * TimeConverter.SECONDS_PER_DAY;
            Quaternion.createQuaternion(x, y, z, a).transform(iTGT0[Tini[img] + i], iTGT0[Tini[img] + i]);
        }

        final ProductNodeGroup<Pin> gcpGroup = sourceProduct.getGcpGroup();
        final int gcpCount = gcpGroup.getNodeCount();
        if (gcpCount != 0) {
            // we assume only one GCP at target altitude
            final Pin gcp = gcpGroup.get(0);
            final GeoPos gcpPos = gcp.getGeoPos();

            if (gcpPos == null) {
                throw new OperatorException("GCP without geolocation found,");
            }

            /**
             * 0. Calculate GCP position in ECEF
             */
            final double[] GCP_ecf = new double[3];
            Conversions.wgsToEcef(gcpPos.getLon(), gcpPos.getLat(), TgtAlt, GCP_ecf);
            final Point2D point2D = Conversions.ecef2wgs(GCP_ecf[0], GCP_ecf[1], GCP_ecf[2]);
            System.out.println("lon = " + point2D.getX());
            System.out.println("lat = " + point2D.getY());

            /**
             * 1. Transform nominal Moving Target to ECEF
             */
            // Transform Moving Target to ECF in order to find the point closest to GCP0
            // iTGT0_ecf = eci2ecf(T+jd0, iTGT0[X,*], iTGT0[Y,*], iTGT0[Z,*])
            final double[][] iTGT0_ecf = new double[iTGT0.length][3];
            for (int i = 0; i < iTGT0.length; i++) {
                final double gst = TimeConverter.jdToGST(T[i] + JD0);
                EcefEciConverter.eciToEcef(gst, iTGT0[i], iTGT0_ecf[i]);
            }

            // Find the closest point
            // diff = SQRT((iTGT0_ecf[X,*] - GCP_ecf[X])^2 + (iTGT0_ecf[Y,*] - GCP_ecf[Y])^2 + (iTGT0_ecf[Z,*] - GCP_ecf[Z])^2)	; Calculates the distance between the GCP and each point of the moving target
            // mn = min(diff,wmin) ; Finds where the minimun point is located (wmin)

            /**
             * 2. Find time offset dT
             */
            double minDiff = Double.MAX_VALUE;
            double tmin = Double.MAX_VALUE;
            int wmin = -1;
            for (int i = Tini[img]; i <= Tend[img]; i++) {
                double[] pos = iTGT0_ecf[i];
                final double diff = Math.sqrt(
                        Pow.pow2(pos[X] - GCP_ecf[X]) + Pow.pow2(pos[Y] - GCP_ecf[Y]) + Pow.pow2(pos[Z] - GCP_ecf[Z]));
//                System.out.println("i = " + (i - Tini[img]));
//                System.out.println("diff = " + diff);
                if (diff < minDiff) {
                    minDiff = diff;
                    tmin = T[i];
                    wmin = i; // This is necessary in order to recompute the times more easily
                }
            }

            System.out.println("minDiff = " + minDiff);
            System.out.println("tmin = " + tmin);
            System.out.println("wmin = " + wmin);
            final double dY;
            if (info.isBackscanning()) {
                dY = (wmin % mode.getNLines()) - (mode.getNLines() - gcp.getPixelPos().getY() + 0.5);
            } else {
                dY = (wmin % mode.getNLines()) - (gcp.getPixelPos().getY() + 0.5);
            }
//            final double dT = tmin - T_ict[img];
//            final double dT = tmin - T_img[(int) gcp.getPixelPos().getY()][img];
            final double dT = (dY * mode.getDt()) / TimeConverter.SECONDS_PER_DAY;
            System.out.println("dT = " + dT);

            /**
             * 3. Update T[]: add dT to all times in T[].
             */
            final double[] T2 = T.clone();
            for (int i = 0; i < T.length; i++) {
//                System.out.println("T[i] = " + T[i]);
                final double newT = T[i] + dT; //tmin + (mode.getDt() * (i - wmin)) / TimeConverter.SECONDS_PER_DAY;
//                System.out.println("newT = " + newT);
                T[i] = newT;
            }
            for (int line = 0; line < mode.getNLines(); line++) {
                T_img[line][img] += dT;
            }

            /**
             * 4. Calculate GCP position in ECI for updated times T[]
             */
            // T_GCP = T[wmin]			; Assigns the acquisition time to the GCP
            final double T_GCP = tmin;
            // GCP_eci = ecf2eci(T+jd0, GCP_ecf.X, GCP_ecf.Y, GCP_ecf.Z, units = GCP_ecf.units)	; Transform GCP coords to ECI for every time in the acquisition
            final double[][] GCP_eci = new double[T.length][3];
            for (int i = 0; i < T.length; i++) {
                final double gst = TimeConverter.jdToGST(T[i] + JD0);
                EcefEciConverter.ecefToEci(gst, GCP_ecf, GCP_eci[i]);
//                EcefEciConverter.ecefToEci(gst, TGTecf, iTGT0[i]);
            }

//// COPIED FROM ABOVE

            /**
             * 5. Interpolate satellite positions & velocities for updated times T[]
             */
            iX = spline(gps_njd, get2ndDim(eci, 0, numGPS), T);
            iY = spline(gps_njd, get2ndDim(eci, 1, numGPS), T);
            iZ = spline(gps_njd, get2ndDim(eci, 2, numGPS), T);
            iVX = spline(gps_njd, get2ndDim(eci, 3, numGPS), T);
            iVY = spline(gps_njd, get2ndDim(eci, 4, numGPS), T);
            iVZ = spline(gps_njd, get2ndDim(eci, 5, numGPS), T);

            iR = new double[T.length];
            for (int i = 0; i < iR.length; i++) {
                iR[i] = Math.sqrt(iX[i] * iX[i] + iY[i] * iY[i] + iZ[i] * iZ[i]);
            }

            // ==v==v== Get Orbital Plane Vector ==================================================
            // ---- Calculates normal vector to orbital plane --------------------------
            Wop = vect_prod(iX, iY, iZ, iVX, iVY, iVZ);
            uWop = unit(Wop);

            // Fixes orbital plane vector to the corresponding point on earth at the time of acquistion setup
            gst_opv = TimeConverter.jdToGST(T[Tfix] + JD0);
            eci_opv = new double[]{uWop[X][Tfix], uWop[Y][Tfix], uWop[Z][Tfix]};
            uWecf = new double[3];
            EcefEciConverter.eciToEcef(gst_opv, eci_opv, uWecf);

            uW = new double[T.length][3];
            for (int i = 0; i < T.length; i++) {
                double gst = TimeConverter.jdToGST(T[i] + JD0);
                EcefEciConverter.ecefToEci(gst, uWecf, uW[i]);
            }

            // ==v==v== Get Angular Velocity ======================================================
            // Angular velocity is not really used in the model, except the AngVel at orbit fixation time (iAngVel[0])

            iAngVel = spline(gps_njd, AngVel, T);
////  EVOBA MORF DEIPOC

            for (int i = 0; i < mode.getNLines(); i++) {
                final double x = uW[Tini[img] + i][X];
                final double y = uW[Tini[img] + i][Y];
                final double z = uW[Tini[img] + i][Z];
                final double a = Math.pow(-1.0,
                                          img) * iAngVel[0] / SLOW_DOWN * (T_img[i][img] - tmin) * TimeConverter.SECONDS_PER_DAY;
                Quaternion.createQuaternion(x, y, z, a).transform(GCP_eci[Tini[img] + i], GCP_eci[Tini[img] + i]);
                System.out.println("i = " + i);
                final double gst = TimeConverter.jdToGST(T[Tini[img] + i] + JD0);
                final double[] ecef = new double[3];
                EcefEciConverter.eciToEcef(gst, GCP_eci[Tini[img] + i], ecef);
                final Point2D p = Conversions.ecef2wgs(ecef[0], ecef[1], ecef[2]);
                System.out.println("lon = " + p.getX());
                System.out.println("lat = " + p.getY());
                System.out.println();
//                Quaternion.createQuaternion(x, y, z, a).transform(iTGT0[Tini[img] + i], iTGT0[Tini[img] + i]);
            }


//            final DefaultXYDataset ds1 = new DefaultXYDataset();
//            final double[][] ds1XY = new double[2][T.length];
//            final double[][] ds2XY = new double[2][T.length];
//            for (int i = 0; i < T.length; i++) {
//                final double[] tmp = new double[3];
//                EcefEciConverter.eciToEcef(TimeConverter.jdToGST(T2[i] + JD0), iTGT0[i], tmp);
//                final Point2D p1 = Conversions.ecef2wgs(tmp[0], tmp[1], tmp[2]);
//                ds1XY[0][i] = p1.getX();
//                ds1XY[1][i] = p1.getY();
//                EcefEciConverter.eciToEcef(TimeConverter.jdToGST(T[i] + JD0), GCP_eci[i], tmp);
//                final Point2D p2 = Conversions.ecef2wgs(tmp[0], tmp[1], tmp[2]);
//                ds2XY[0][i] = p2.getX();
//                ds2XY[1][i] = p2.getY();
//            }
//            ds1.addSeries("Nominal", ds1XY);
//            ds1.addSeries("GCP", ds2XY);
//            final XYPlot xyPlot = new XYPlot(ds1, new NumberAxis("lon"), new NumberAxis("lat"),
//                                             new StandardXYItemRenderer());
//
//            final JFreeChart chart = new JFreeChart(xyPlot);
//            final ChartPanel chartPanel = new ChartPanel(chart);
//            chartPanel.setVisible(true);
//
//            final JFrame frame = new JFrame("Plots");
//            frame.add(chartPanel);
//            frame.setVisible(true);

            iTGT0 = GCP_eci;
        }

        // Once GCP and TT are used iTGT0 will be subsetted to the corrected T, but in the nominal case iTGT0 matches already T
        double[][] iTGT = iTGT0;

        // Determine the roll offset due to GCP not being in the middle of the CCD
        // IF info.Mode NE 5 THEN nC2 = nCols/2 ELSE nC2 = nCols-1		; Determine the column number of the middle of the CCD
        // dRoll = (nC2-GCP[X])*IFOV									; calculates the IFOV angle difference from GCP0's pixel column to the image central pixel (the nominal target)
        double dRoll = 0.0;
        if (gcpCount != 0) {
            final int nC2;
            if (info.getMode() != 5) {
                nC2 = mode.getNCols() / 2;
            } else {
                nC2 = mode.getNCols() - 1;
            }
            final Pin gcp = gcpGroup.get(0);
            dRoll = (nC2 - gcp.getPixelPos().getX()) * mode.getIfov();
        }

        //==== Calculates View Angles ==============================================

//            ViewAng[] viewAngs = new ViewAng[mode.getNLines()];
        double[][] viewRange = new double[3][mode.getNLines()];
        for (int i = 0; i < mode.getNLines(); i++) {
            double TgtX = iTGT[Tini[img] + i][X];
            double TgtY = iTGT[Tini[img] + i][Y];
            double TgtZ = iTGT[Tini[img] + i][Z];
            double SatX = iX[Tini[img] + i];
            double SatY = iY[Tini[img] + i];
            double SatZ = iZ[Tini[img] + i];
            double TgtLat = info.getTargetLat();
            ViewAng viewAng = CoordinateUtils.computeViewAng(TgtX, TgtY, TgtZ, SatX, SatY, SatZ, TgtLat);

            // ---- View.Rang[XYZ] is the vector pointing from TGT to SAT,
            // ----  but for the calculations it is nevessary the oposite one, therefore (-) appears.
            viewRange[X][i] = -viewAng.rangeX;
            viewRange[Y][i] = -viewAng.rangeY;
            viewRange[Z][i] = -viewAng.rangeZ;

            //ObsAngAzi[i] = viewAng.azi;
            //ObsAngZen[i] = viewAng.zen;
        }

        // Observation angles are not needed for the geometric correction but they are used for research. They are a by-product.
        // But ViewAngs provides also the range from the target to the satellite, which is needed later (Range, of course could be calculated independently).

        // ==== Satellite Rotation Axes ==============================================

        double[][] uEjeYaw = new double[3][mode.getNLines()];
        for (int i = 0; i < mode.getNLines(); i++) {
            uEjeYaw[X][i] = iX[Tini[img] + i];
            uEjeYaw[Y][i] = iY[Tini[img] + i];
            uEjeYaw[Z][i] = iZ[Tini[img] + i];
        }
        uEjeYaw = unit(uEjeYaw);
//        for (int i = 0; i < mode.getNLines(); i++) {
//            EjeYaw[X][i] = uEjeYaw[X][i];
//            EjeYaw[Y][i] = uEjeYaw[Y][i];
//            EjeYaw[Z][i] = uEjeYaw[Z][i];
//        }

        double[][] uEjePitch = new double[3][mode.getNLines()];
        for (int i = 0; i < mode.getNLines(); i++) {
            uEjePitch[X][i] = uWop[X][Tini[img] + i];
            uEjePitch[Y][i] = uWop[Y][Tini[img] + i];
            uEjePitch[Z][i] = uWop[Z][Tini[img] + i];
        }
//        for (int i = 0; i < mode.getNLines(); i++) {
//            EjePitch[X][i] = uEjePitch[X][i] = uWop[X][Tini[img] + i];
//            EjePitch[Y][i] = uEjePitch[Y][i] = uWop[Y][Tini[img] + i];
//            EjePitch[Z][i] = uEjePitch[Z][i] = uWop[Z][Tini[img] + i];
//        }

        double[][] uEjeRoll = vect_prod(uEjePitch, uEjeYaw);
//        for (int i = 0; i < mode.getNLines(); i++) {
//            EjeRoll[X][i] = uEjeRoll[X][i];
//            EjeRoll[Y][i] = uEjeRoll[Y][i];
//            EjeRoll[Z][i] = uEjeRoll[Z][i];
//        }

        double[][] uRange = unit(viewRange);

        // ScanPlane:
        //double[][] uSP = new double[3][mode.getNLines()];
        //double[][] uSPr = new double[3][mode.getNLines() * mode.getNCols()];

        // SightLine:
        //double[][] uSL = new double[3][mode.getNLines()];
        //double[][] uSLr = new double[3][mode.getNLines() * mode.getNCols()];

        // RollSign:
        int[] uRollSign = new int[mode.getNLines()];
        //int[] uRollSignR = new int[mode.getNLines() * mode.getNCols()];

        double[][] uSP = vect_prod(uRange, uEjePitch);
        double[][] uSL = vect_prod(uEjePitch, uSP);
        double[][] uRoll = unit(vect_prod(uSL, uRange));
        for (int i = 0; i < mode.getNLines(); i++) {
            double total = 0;
            total += uRoll[X][i] / uSP[X][i];
            total += uRoll[Y][i] / uSP[Y][i];
            total += uRoll[Z][i] / uSP[Z][i];
            uRollSign[i] = (int) Math.signum(total);
        }

//        for (int i = 0; i < mode.getNLines(); i++) {
//            SP[X][i] = uSP[X][i];
//            SP[Y][i] = uSP[Y][i];
//            SP[Z][i] = uSP[Z][i];
//
//            SL[X][i] = uSL[X][i];
//            SL[Y][i] = uSL[Y][i];
//            SL[Z][i] = uSL[Z][i];
//        }

        double[] uPitchAng = new double[mode.getNLines()];
        double[] uRollAng = new double[mode.getNLines()];
        System.out.println("dRoll = " + dRoll);
        for (int i = 0; i < mode.getNLines(); i++) {
            uPitchAng[i] = Math.PI / 2.0 - CoordinateUtils.vectAngle(uSP[X][i], uSP[Y][i], uSP[Z][i],
                                                                     uEjeYaw[X][i], uEjeYaw[Y][i], uEjeYaw[Z][i]);
            uRollAng[i] = uRollSign[i] * CoordinateUtils.vectAngle(uSL[X][i], uSL[Y][i], uSL[Z][i],
                                                                   uRange[X][i], uRange[Y][i], uRange[Z][i]);
            uRollAng[i] += dRoll;
            // Stores the results for each image
            //PitchAng[i] = uPitchAng[i];
            //RollAng[i] = uRollAng[i];
        }

        // ==== Rotate the Line of Sight and intercept with Earth ==============================================

        double[] ixSubset = new double[mode.getNLines()];
        double[] iySubset = new double[mode.getNLines()];
        double[] izSubset = new double[mode.getNLines()];
        double[] timeSubset = new double[mode.getNLines()];
        for (int i = 0; i < timeSubset.length; i++) {
            ixSubset[i] = iX[Tini[img] + i];
            iySubset[i] = iY[Tini[img] + i];
            izSubset[i] = iZ[Tini[img] + i];
            timeSubset[i] = T[Tini[img] + i];
        }
        igm = ProbaIgmDoer.doIgmJava(mode.getNLines(), mode.getNCols(), mode.getFov(), mode.getIfov(),
                                     uEjePitch, uPitchAng,
                                     uEjeRoll, uRollAng,
                                     uEjeYaw,
                                     TgtAlt,
                                     ixSubset, iySubset, izSubset,
                                     timeSubset, info.getMode());

        final Product targetProduct = createTargetProduct();

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (targetBand.equals(pixelLonBand)) {
            fillTile(targetBand, targetTile, igm[0]);
        }
        if (targetBand.equals(pixelLatBand)) {
            fillTile(targetBand, targetTile, igm[1]);
        }
    }

    private void fillTile(Band targetBand, Tile targetTile, double[][] data) {
        final int h = targetBand.getSceneRasterHeight();
        if (info.isBackscanning()) {
            for (final Tile.Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, data[h - 1 - pos.y][pos.x]);
            }
        } else {
            for (final Tile.Pos pos : targetTile) {
                targetTile.setSample(pos.x, pos.y, data[pos.y][pos.x]);
            }
        }
    }

    private Telemetry getTelemetry() {
        final File telemetryRepository;
        if (telemetrySearchPath == null || telemetrySearchPath.isEmpty()) {
            telemetrySearchPath = System.getProperty("beam.chris.telemetryRepositoryPath");
        }
        if (telemetrySearchPath == null || telemetrySearchPath.isEmpty()) {
            telemetryRepository = null;
        } else {
            telemetryRepository = new File(telemetrySearchPath);
        }
        try {
            return TelemetryFinder.findTelemetry(sourceProduct, telemetryRepository);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private Product createTargetProduct() {
        final String type = sourceProduct.getProductType() + "_GC";
        final Product targetProduct = createCopy(sourceProduct, "GC", type, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return true;
            }
        });

        final int w = targetProduct.getSceneRasterWidth();
        final int h = targetProduct.getSceneRasterHeight();

        final float[] lons = new float[w * h];
        final float[] lats = new float[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (info.isBackscanning()) {
                    lons[y * w + x] = (float) igm[0][h - 1 - y][x];
                    lats[y * w + x] = (float) igm[1][h - 1 - y][x];
                } else {
                    lons[y * w + x] = (float) igm[0][y][x];
                    lats[y * w + x] = (float) igm[1][y][x];
                }
            }
        }

        final TiePointGrid lonGrid = new TiePointGrid("lon", w, h, 0.5f, 0.5f, 1.0f, 1.0f, lons);
        final TiePointGrid latGrid = new TiePointGrid("lat", w, h, 0.5f, 0.5f, 1.0f, 1.0f, lats);
        targetProduct.addTiePointGrid(lonGrid);
        targetProduct.addTiePointGrid(latGrid);
        targetProduct.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        pixelLonBand = targetProduct.addBand("pixelLon", ProductData.TYPE_FLOAT64);
        pixelLatBand = targetProduct.addBand("pixelLat", ProductData.TYPE_FLOAT64);

        return targetProduct;
    }

    private static double[][] unit(double[][] vec) {
        double[][] result = new double[vec.length][vec[0].length];
        for (int i = 0; i < vec[0].length; i++) {
            double norm = Math.sqrt(vec[X][i] * vec[X][i] + vec[Y][i] * vec[Y][i] + vec[Z][i] * vec[Z][i]);
            result[X][i] = vec[X][i] / norm;
            result[Y][i] = vec[Y][i] / norm;
            result[Z][i] = vec[Z][i] / norm;
        }
        return result;
    }

    private static double[][] vect_prod(double[] x1, double[] y1, double[] z1, double[] x2, double[] y2, double[] z2) {
        double[][] product = new double[3][x1.length];
        for (int i = 0; i < x1.length; i++) {
            product[X][i] = y1[i] * z2[i] - z1[i] * y2[i];
            product[Y][i] = z1[i] * x2[i] - x1[i] * z2[i];
            product[Z][i] = x1[i] * y2[i] - y1[i] * x2[i];
        }
        return product;
    }

    private static double[][] vect_prod(double[][] a1, double[][] a2) {
        double[][] product = new double[3][a1[0].length];
        for (int i = 0; i < a1[0].length; i++) {
            product[X][i] = a1[Y][i] * a2[Z][i] - a1[Z][i] * a2[Y][i];
            product[Y][i] = a1[Z][i] * a2[X][i] - a1[X][i] * a2[Z][i];
            product[Z][i] = a1[X][i] * a2[Y][i] - a1[Y][i] * a2[X][i];
        }
        return product;
    }

    private static double[] get2ndDim(double[][] twoDimArray, int secondDimIndex, int numElems) {
        double[] secondDim = new double[numElems];
        for (int i = 0; i < numElems; i++) {
            secondDim[i] = twoDimArray[i][secondDimIndex];
        }
        return secondDim;
    }

    private static double[] spline(double[] x, double[] y, double[] t) {
        double[] v = new double[t.length];
        PolynomialSplineFunction splineFunction = SplineInterpolator.interpolate(x, y);
        for (int i = 0; i < v.length; i++) {
            v[i] = splineFunction.value(t[i]);
        }
        return v;
    }

    private static double getDeltaGPS(Product product) {
        final Date date = product.getStartTime().getAsDate(); // image center time for CHRIS products
        final double mjd = TimeConverter.dateToMJD(date);

        try {
            return TimeConverter.getInstance().deltaGPS(mjd);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    // TODO - this is almost a general utility method (rq-20090708)
    private static Product createCopy(Product sourceProduct, String name, String type, BandFilter bandFilter) {
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(name, type, w, h);

        // 1. set start and end times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        // 2. copy flag codings
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        // TODO - tie point grids

        // 3. copy all bands from source product to target product
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (bandFilter.accept(sourceBand)) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct);
                targetBand.setSourceImage(sourceBand.getSourceImage());
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
                }
            }
        }

        // 4. copy bitmask definitions
        ProductUtils.copyBitmaskDefs(sourceProduct, targetProduct);

        // 5. copy metadata tree
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        // 6. set preferred tile size
        targetProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());

        // 7. copy pins
        for (int i = 0; i < sourceProduct.getPinGroup().getNodeCount(); i++) {
            final Pin pin = sourceProduct.getPinGroup().get(i);
            targetProduct.getPinGroup().add(new Pin(pin.getName(),
                                                    pin.getLabel(),
                                                    pin.getDescription(),
                                                    pin.getPixelPos(),
                                                    pin.getGeoPos(),
                                                    pin.getSymbol()));
        }
        // 8. copy GCPs
        for (int i = 0; i < sourceProduct.getGcpGroup().getNodeCount(); i++) {
            final Pin pin = sourceProduct.getGcpGroup().get(i);
            targetProduct.getGcpGroup().add(new Pin(pin.getName(),
                                                    pin.getLabel(),
                                                    pin.getDescription(),
                                                    pin.getPixelPos(),
                                                    pin.getGeoPos(),
                                                    pin.getSymbol()));
        }

        return targetProduct;
    }

    private static List<GpsDataRecord> readGpsData(File gpsFile, double delay, double deltaGPS) {
        InputStream is = null;
        try {
            is = new FileInputStream(gpsFile);
            return GpsDataRecord.create(new GpsDataRecord.GpsDataReader(is).getReadRecords(), deltaGPS, delay);
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static IctDataRecord readIctData(File ictFile, double deltaGPS) {
        InputStream is = null;
        try {
            is = new FileInputStream(ictFile);
            return IctDataRecord.create(new IctDataReader(is).getLastIctValues(), deltaGPS);
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PerformGeometricCorrectionOp.class);
        }
    }

}
