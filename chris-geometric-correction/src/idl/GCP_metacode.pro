Sphr_R = [aEarth, aEarth, (1-f)*aEarth] + TgtAlt    ; Earth spheroid with nominal target altitude

GCP = [X, Y, lat, lon, alt]		; The GCP is defined by the pixel coordinates (X,Y) and the geographic position (lat, lon, alt in km), in case altitude is not given the nominal target altitude should be used

; Transform GCPs to ECF
GCP_ecf = gdt2ecf(GCP[lat], GCP[lon], GCP[alt], /km)			; we asume only one GCPs per image

; Transform Moving Target to ECF in order to find the point closest to GCP0
iTGT0_ecf = eci2ecf(T+jd0, iTGT0[X,*], iTGT0[Y,*], iTGT0[Z,*])

; Find the closest point
diff = SQRT((iTGT0_ecf[X,*] - GCP_ecf[X])^2 + (iTGT0_ecf[Y,*] - GCP_ecf[Y])^2 + (iTGT0_ecf[Z,*] - GCP_ecf[Z])^2)	; Calculates the distance between the GCP and each point of the moving target
mn = min(diff,wmin)			; Finds where the minimun point is located (wmin)

T_GCP = T[wmin]			; Assigns the acquisition time to the GCP

GCP_eci = ecf2eci(T+jd0, GCP_ecf.X, GCP_ecf.Y, GCP_ecf.Z, units = GCP_ecf.units)	; Transform GCP coords to ECI for every time in the acquisition

iTGT0 = GCP_eci
; In case the GCP's altitude does not match the nominal one, the coordinates must be projected to that altitude, as all the pointing of the satellite is comanded for the nominal altitude
IF GCP[Alt] NE TgtAlt THEN begin                                                    
    SatPos = [iX[wmin],iY[wmin],iZ[wmin]]														; Sat position at the moment GCP line was acquired
    tmp = Line_Sphr_i(unit(SatPos-[GCP_eci.X[i],GCP_eci.Y[i],GCP_eci.Z[i]]), Sphr_R, SatPos)	; Finds the interception of the line from Sat to GCP with Earth spheroid at nominal target altitude
    kk = min([TOTAL((SatPos-tmp[*,0])^2), TOTAL((SatPos-tmp[*,1])^2)], mn_ndx)					; Determine which one of the two intercepts is the good one Cthe one closest to the Sat)
    iTGT0.X[i] = tmp[X,mn_ndx] & GCP0.Y[i] = tmp[Y,mn_ndx] & GCP0.Z[i] = tmp[Z,mn_ndx]			; Store the corrected coordinate in ECI corresponding to the nominal altitude
ENDIF

; In case more GCPs are availabe here T, Tini, Tend variables should be updated, and Sat pos calculated correspondingly

; Generate new Moving Target
qTscn = QTCOMPOSE(transpose([[uW.X[Tini[img]:Tend[img]]],[uW.Y[Tini[img]:Tend[img]]],[uW.Z[Tini[img]:Tend[img]]]]), (-1)^img * iAngVel[0] / SlowDown * (T_img[*,img] - T_GCP)*86400D)

V = TRANSPOSE([[GCP0.X[Tini[img]:Tend[img]]], [GCP0.Y[Tini[img]:Tend[img]]], [GCP0.Z[Tini[img]:Tend[img]]]])

tmp = QTVROT(V, qTscn)
iTGT0.X[Tini[img]:Tend[img]] = tmp[X,*]
iTGT0.Y[Tini[img]:Tend[img]] = tmp[Y,*]
iTGT0.Z[Tini[img]:Tend[img]] = tmp[Z,*]

;Determine the roll offset due to GCP not being in the middle of the CCD
IF info.Mode NE 5 THEN nC2 = nCols/2 ELSE nC2 = nCols-1		; Determine the column number of the middle of the CCD
dRoll = (nC2-GCP[X])*IFOV									; calculates the IFOV angle difference from GCP0's pixel column to the image central pixel (the nominal target)


; Now proceed to calculate Sat axis and pointing angles as in the nominal case
; Remember to update the roll angle with dRoll before calculating the IGMs (in nominal case dRoll=0)