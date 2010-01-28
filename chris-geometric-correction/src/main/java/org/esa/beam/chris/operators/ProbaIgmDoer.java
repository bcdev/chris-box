package org.esa.beam.chris.operators;

import java.awt.geom.Point2D;

class ProbaIgmDoer {

//    public static void doIgm(int nLines,
//                             int nCols,
//                             double FOV,
//                             double IFOV,
//                             double[][] uEjePitch,
//                             double[] uPitchAng,
//                             double[][] uEjeRoll,
//                             double[] uRollAng,
//                             double[][] uEjeYaw,
//                             double TgtAlt,
//                             double[] iX,
//                             double[] iY,
//                             double[] iZ,
//                             double[] Time,
//                             int Mode) {
//        //---- Constantes ------------------------------------------------------------------
//        // Radio [km] de la Tierra en el Ecuador WGS84
//        final double aEarth = 6378.137;
//        // Factor de achatamiento de la Tierra WGS84
//        final double f = 1 / 298.257223563;
//
//        // Origen de tiempo del NJD (New Julian Day) q me he inventado para q en las graficas no se vaya de baras el eje de tiempos ya q el JD tiene demasiadas cifras :(
//        final double jd0 = Conversions.julianDate(2003, 0, 1);
//
//        final int X = 0;
//        final int Y = 1;
//        final int Z = 2;
//        //----------------------------------------------------------------------------------
//
//        final double[][][] LoS = new double[nCols][nLines][3];
//
//        // By default Mode 1 is considered. In reality it does not matter except for Mode 5
//
//        // ScanAng is the rotation that must be applied to the Line of Sight in order to point properly for each pixel.
//        final double[] ScanAng = new double[nCols];
//        if (Mode == 5) {
//            // In Mode 5 the last pixel is the one pointing to the target, i.e. ScanAng must equal zero for the last pixel.
//            final double fovOffset = FOV / 2.0;
//            for (int i = 0; i < ScanAng.length; i++) {
//                ScanAng[i] = (i + 0.5) * IFOV - fovOffset;
//            }
//        } else {
//            for (int i = 0; i < ScanAng.length; i++) {
//                ScanAng[i] = (i + 0.5) * IFOV - FOV;
//            }
//        }
//
//        for (int L = 0; L < nLines; L++) {
////          qPitch = QTCOMPOSE(uEjePitch[*,L], -uPitchAng[L])	; Quaternion for pitch rotation
//            final Quaternion qPitch = createQuaternion(uEjePitch[L], -uPitchAng[L]);
//
////          newRoll = QTVROT(uEjeRoll[*,L], qPitch)				; New-Roll-Axis: Rotates the Roll-axis around the Pitch-axis to match the Scan-Plane normal vector
//            final double[] newRoll = qPitch.rotateVector(uEjeRoll[L], uEjeRoll[L].clone());
//
////            fwdLoS = QTVROT(-uEjeYaw[*,L], qPitch)				; Rotates the Downward LoS (-Yaw_Axis) around pitch-axis to get the Forward LoS
//            final double[] fwdLoS = uEjeYaw[L].clone();
//            for (int i = 0; i < fwdLoS.length; i++) {
//                fwdLoS[i] = -fwdLoS[i];
//            }
//            qPitch.rotateVector(fwdLoS, fwdLoS);
//
////          qRoll = QTCOMPOSE(newRoll, uRollAng[L])				; Quaternion for roll rotation around the new-roll-axis
//            final Quaternion qRoll = createQuaternion(newRoll, uRollAng[L]);
//
////          Nadir2 = QTVROT(fwdLoS, qRoll)						; Rotates the Forward LoS around new-roll-axis
//            final double[] Nadir2 = qRoll.rotateVector(fwdLoS, fwdLoS.clone());
//
//            for (int C = 0; C < nCols; C++) {
////              qRoll_VL = QTCOMPOSE(newRoll, ScanAng[C])	; Quaternion for rotating along the CCD space lines
//                final Quaternion qRoll_VL = createQuaternion(newRoll, ScanAng[C]);
//
////              LoS[*,L,C] = QTVROT(Nadir2, qRoll_VL)	; Line of Sight
//                qRoll_VL.rotateVector(Nadir2, LoS[C][L]);
//            }
//        }
//
////      VL_TGT = dblarr(3,nLines,nCols)
////      VL_Range = dblarr(nLines,nCols)
////      VL_Img = dblarr(3,nLines,nCols)
////      IGM = fltarr(nCols,nLines,2)
//
//        final double[][][] VL_TGT = new double[nCols][nLines][3];
//        final double[][] VL_Range = new double[nCols][nLines];
//        final double[][][] VL_Img = new double[nCols][nLines][3];
//        final double[][][] IGM = new double[2][nLines][nCols];
//
////      Sphr_R = [aEarth, aEarth, (1-f)*aEarth]+TgtAlt			; The geoide surface has an altide equal to the Target's mean altitude (from the metadata).
//        final double[] Sphr_R = {aEarth + TgtAlt, aEarth + TgtAlt, (1.0 - f) * aEarth + TgtAlt};
//
//        for (int L = 0; L < nLines; L++) {
////	        SatPos = [iX[L],iY[L],iZ[L]]
//            final double[] SatPos = new double[]{iX[L], iY[L], iZ[L]};
//            final double[] center = new double[3];
//
//            for (int C = 0; C < nCols; C++) {
////		        tmp = Line_Sphr_i(reform(LoS[*,L,C],3), Sphr_R, SatPos)
////		        LoS_Range[L,C] = min([SQRT(TOTAL((SatPos-tmp[*,0])^2)), SQRT(TOTAL((SatPos-tmp[*,1])^2))], mn_ndx)
////		        LoS_TGT[*,L,C] = tmp[*,mn_ndx]
//                final double[] tmp = SatPos.clone();
//                Intersector.intersect(tmp, LoS[C][L], center, Sphr_R);
//
////		        tmp = eci2ecf(Time[L]+jd0, VL_TGT[X,L,C], VL_TGT[Y,L,C], VL_TGT[Z,L,C])
//                final double gst = Conversions.jdToGST(Time[L] + jd0);
//                EcefEciConverter.eciToEcef(gst, VL_TGT[C][L], tmp);
//
////		        LoS_Img[X,L,C]=tmp.X
////		        LoS_Img[Y,L,C]=tmp.Y
////		        LoS_Img[Z,L,C]=tmp.Z
////
////		        tmp = ecf2gdt(LoS_Img[X,L,C], LoS_Img[Y,L,C], LoS_Img[Z,L,C], /KM)
////		        IGM[C,L,0]=float(reform(tmp.LON))
////		        IGM[C,L,1]=float(reform(tmp.LAT))
//                final Point2D point = Conversions.ecef2wgs(tmp[X], tmp[Y], tmp[Z]);
//                IGM[0][L][C] = point.getX();
//                IGM[1][L][C] = point.getY();
//            }
//        }
//    }

    private static Quaternion createQuaternion(double[] axis, double angle) {
        return Quaternion.createQuaternion(axis[0], axis[1], axis[2], angle);
    }

    public static double[][][] doIgmJava(int nLines,
                                         int nCols,
                                         double FOV,
                                         double IFOV,
                                         double[][] uEjePitch, //[3][nlines]
                                         double[] uPitchAng,   //[nlines]
                                         double[][] uEjeRoll,  //[3][nlines]
                                         double[] uRollAng,    //[nlines]
                                         double[][] uEjeYaw,   //[3][nlines]
                                         double TgtAlt,
                                         double[] iX,
                                         double[] iY,
                                         double[] iZ,
                                         double[] Time,
                                         int Mode) {
        //---- Constantes ------------------------------------------------------------------
        // Radio [km] de la Tierra en el Ecuador WGS84
        final double aEarth = 6378.137;
        // Factor de achatamiento de la Tierra WGS84
        final double f = 1.0 / 298.257223563;

        // Origen de tiempo del NJD (New Julian Day) q me he inventado para q en las graficas no se vaya de baras el eje de tiempos ya q el JD tiene demasiadas cifras :(
        final double jd0 = TimeConverter.julianDate(2001, 0, 1);

        final int X = 0;
        final int Y = 1;
        final int Z = 2;
        //----------------------------------------------------------------------------------

        final double[][][] LoS = new double[nCols][nLines][3];

        // By default Mode 1 is considered. In reality it does not matter except for Mode 5

        // ScanAng is the rotation that must be applied to the Line of Sight in order to point properly for each pixel.
        final double[] ScanAng = new double[nCols];
        if (Mode == 5) {
            // In Mode 5 the last pixel is the one pointing to the target, i.e. ScanAng must equal zero for the last pixel.
            for (int i = 0; i < ScanAng.length; i++) {
                ScanAng[i] = (i + 0.5) * IFOV - FOV;
            }
        } else {
            for (int i = 0; i < ScanAng.length; i++) {
                ScanAng[i] = (i + 0.5) * IFOV - FOV / 2.0;
            }
        }

        for (int L = 0; L < nLines; L++) {
//          qPitch = QTCOMPOSE(uEjePitch[*,L], -uPitchAng[L])   ; Quaternion for pitch rotation
            final Quaternion qPitch = createQuaternionJ(uEjePitch, -uPitchAng[L], L);

//          newRoll = QTVROT(uEjeRoll[*,L], qPitch)             ; New-Roll-Axis: Rotates the Roll-axis around the Pitch-axis to match the Scan-Plane normal vector
            double[] uEjeRoll1D = new double[]{uEjeRoll[X][L], uEjeRoll[Y][L], uEjeRoll[Z][L]};
            final double[] newRoll = qPitch.transform(uEjeRoll1D, new double[3]);

//            fwdLoS = QTVROT(-uEjeYaw[*,L], qPitch)                ; Rotates the Downward LoS (-Yaw_Axis) around pitch-axis to get the Forward LoS
            final double[] fwdLoS = new double[3];
            for (int i = 0; i < fwdLoS.length; i++) {
                fwdLoS[i] = -uEjeYaw[i][L];
            }
            qPitch.transform(fwdLoS, fwdLoS);

//          qRoll = QTCOMPOSE(newRoll, uRollAng[L])             ; Quaternion for roll rotation around the new-roll-axis
            final Quaternion qRoll = createQuaternion(newRoll, uRollAng[L]);

//          Nadir2 = QTVROT(fwdLoS, qRoll)                      ; Rotates the Forward LoS around new-roll-axis
            final double[] Nadir2 = qRoll.transform(fwdLoS, new double[3]);

            for (int C = 0; C < nCols; C++) {
//              qRoll_VL = QTCOMPOSE(newRoll, ScanAng[C])   ; Quaternion for rotating along the CCD space lines
                final Quaternion qRoll_VL = createQuaternion(newRoll, ScanAng[C]);

//              LoS[*,L,C] = QTVROT(Nadir2, qRoll_VL)   ; Line of Sight
                qRoll_VL.transform(Nadir2, LoS[C][L]);
            }
        }

//      LOS_TGT = dblarr(3,nLines,nCols)
//      LOS_Range = dblarr(nLines,nCols)
//      LOS_Img = dblarr(3,nLines,nCols)
//      IGM = fltarr(nCols,nLines,2)

//        final double[][][] LOS_TGT = new double[nCols][nLines][3];
        //final double[][] LOS_Range = new double[nCols][nLines];
        //final double[][][] LOS_Img = new double[nCols][nLines][3];
        final double[][][] IGM = new double[2][nLines][nCols];

//      Sphr_R = [aEarth, aEarth, (1-f)*aEarth]+TgtAlt          ; The geoide surface has an altide equal to the Target's mean altitude (from the metadata).
        final double[] Sphr_R = {aEarth + TgtAlt, aEarth + TgtAlt, (1.0 - f) * aEarth + TgtAlt};

        for (int L = 0; L < nLines; L++) {
//          SatPos = [iX[L],iY[L],iZ[L]]
            final double[] SatPos = new double[]{iX[L], iY[L], iZ[L]};
            final double[] center = new double[3];

            final double gst = TimeConverter.jdToGST(Time[L] + jd0);
            for (int C = 0; C < nCols; C++) {
//              tmp = Line_Sphr_i(reform(LoS[*,L,C],3), Sphr_R, SatPos)
//              LoS_Range[L,C] = min([SQRT(TOTAL((SatPos-tmp[*,0])^2)), SQRT(TOTAL((SatPos-tmp[*,1])^2))], mn_ndx)
//              LoS_TGT[*,L,C] = tmp[*,mn_ndx]
                final double[] tmp = SatPos.clone();
                // todo - intersect with DEM instead
                Intersector.intersect(tmp, LoS[C][L], center, Sphr_R);

//              tmp = eci2ecf(Time[L]+jd0, LOS_TGT[X,L,C], LOS_TGT[Y,L,C], LOS_TGT[Z,L,C])
                EcefEciConverter.eciToEcef(gst, tmp, tmp);

//              LoS_Img[X,L,C]=tmp.X
//              LoS_Img[Y,L,C]=tmp.Y
//              LoS_Img[Z,L,C]=tmp.Z
//
//              tmp = ecf2gdt(LoS_Img[X,L,C], LoS_Img[Y,L,C], LoS_Img[Z,L,C], /KM)
//              IGM[C,L,0]=float(reform(tmp.LON))
//              IGM[C,L,1]=float(reform(tmp.LAT))
                final Point2D point = Conversions.ecef2wgs(tmp[X], tmp[Y], tmp[Z]);
                IGM[0][L][C] = point.getX();
                IGM[1][L][C] = point.getY();
            }
        }

        return IGM;
    }


    public static double[][][] doIgmJava2(int nLines,
                                          int nCols,
                                          double[][] pitchRotation, // [nLines][nCols]
                                          double[][] rollRotation, // [nLines][nCols]
                                          double[][] uEjePitch, //[3][nlines]
                                          double[][] uEjeRoll,  //[3][nlines]
                                          double[][] uEjeYaw,   //[3][nlines]
                                          double TgtAlt,
                                          double[] iX,
                                          double[] iY,
                                          double[] iZ,
                                          double[] Time,
                                          int Mode) {
        //---- Constantes ------------------------------------------------------------------
        // Radio [km] de la Tierra en el Ecuador WGS84
        final double aEarth = 6378.137;
        // Factor de achatamiento de la Tierra WGS84
        final double f = 1.0 / 298.257223563;

        // Origen de tiempo del NJD (New Julian Day) q me he inventado para q en las graficas no se vaya de baras el eje de tiempos ya q el JD tiene demasiadas cifras :(
        final double jd0 = TimeConverter.julianDate(2001, 0, 1);

        final int X = 0;
        final int Y = 1;
        final int Z = 2;
        //----------------------------------------------------------------------------------

        final double[][][] LoS = new double[nLines][nCols][3];

        for (int L = 0; L < nLines; L++) {
            for (int C = 0; C < nCols; C++) {
                final Quaternion qPitch = createQuaternionJ(uEjePitch, -pitchRotation[L][C], L);

//          newRoll = QTVROT(uEjeRoll[*,L], qPitch)             ; New-Roll-Axis: Rotates the Roll-axis around the Pitch-axis to match the Scan-Plane normal vector
                double[] uAxisRoll1D = new double[]{uEjeRoll[X][L], uEjeRoll[Y][L], uEjeRoll[Z][L]};
                final double[] newRollAxis = qPitch.transform(uAxisRoll1D, new double[3]);

//            fwdLoS = QTVROT(-uEjeYaw[*,L], qPitch)                ; Rotates the Downward LoS (-Yaw_Axis) around pitch-axis to get the Forward LoS
                final double[] fwdLoS = new double[3];
                for (int i = 0; i < fwdLoS.length; i++) {
                    fwdLoS[i] = -uEjeYaw[i][L];
                }
                qPitch.transform(fwdLoS, fwdLoS);

//          qRoll = QTCOMPOSE(newRoll, uRollAng[L])             ; Quaternion for roll rotation around the new-roll-axis
                final Quaternion qRoll = createQuaternion(newRollAxis, rollRotation[L][C]);

//          Nadir2 = QTVROT(fwdLoS, qRoll)                      ; Rotates the Forward LoS around new-roll-axis
                LoS[L][C] = qRoll.transform(fwdLoS, new double[3]);
            }
        }

//      LOS_TGT = dblarr(3,nLines,nCols)
//      LOS_Range = dblarr(nLines,nCols)
//      LOS_Img = dblarr(3,nLines,nCols)
//      IGM = fltarr(nCols,nLines,2)

//        final double[][][] LOS_TGT = new double[nCols][nLines][3];
        //final double[][] LOS_Range = new double[nCols][nLines];
        //final double[][][] LOS_Img = new double[nCols][nLines][3];
        final double[][][] IGM = new double[2][nLines][nCols];

//      Sphr_R = [aEarth, aEarth, (1-f)*aEarth]+TgtAlt          ; The geoide surface has an altide equal to the Target's mean altitude (from the metadata).
        final double[] Sphr_R = {aEarth + TgtAlt, aEarth + TgtAlt, (1.0 - f) * aEarth + TgtAlt};

        for (int L = 0; L < nLines; L++) {
//          SatPos = [iX[L],iY[L],iZ[L]]
            final double[] satPositionEci = new double[]{iX[L], iY[L], iZ[L]};
            final double[] center = new double[3];

            final double gst = TimeConverter.jdToGST(Time[L] + jd0);
            for (int C = 0; C < nCols; C++) {
//              tmp = Line_Sphr_i(reform(LoS[*,L,C],3), Sphr_R, SatPos)
//              LoS_Range[L,C] = min([SQRT(TOTAL((SatPos-tmp[*,0])^2)), SQRT(TOTAL((SatPos-tmp[*,1])^2))], mn_ndx)
//              LoS_TGT[*,L,C] = tmp[*,mn_ndx]
                final double[] position = satPositionEci.clone();
                // todo - intersect with DEM instead
                
                Intersector.intersect(position, LoS[L][C], center, Sphr_R);

//              tmp = eci2ecf(Time[L]+jd0, LOS_TGT[X,L,C], LOS_TGT[Y,L,C], LOS_TGT[Z,L,C])
                EcefEciConverter.eciToEcef(gst, position, position);

//              LoS_Img[X,L,C]=tmp.X
//              LoS_Img[Y,L,C]=tmp.Y
//              LoS_Img[Z,L,C]=tmp.Z
//
//              tmp = ecf2gdt(LoS_Img[X,L,C], LoS_Img[Y,L,C], LoS_Img[Z,L,C], /KM)
//              IGM[C,L,0]=float(reform(tmp.LON))
//              IGM[C,L,1]=float(reform(tmp.LAT))
                final Point2D point = Conversions.ecef2wgs(position[X], position[Y], position[Z]);
                IGM[0][L][C] = point.getX();
                IGM[1][L][C] = point.getY();
            }
        }

        return IGM;
    }

    private static Quaternion createQuaternionJ(double[][] axis, double angle, int index) {
        return Quaternion.createQuaternion(axis[0][index], axis[1][index], axis[2][index], angle);
    }

}
