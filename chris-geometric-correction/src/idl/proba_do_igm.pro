; a PitchAxis hay q pasarle PitchAxis=uEjePitch
; a TgtAlt hay q pasarle la altura en [km]: TgtAlt = TgtAlt
; a PosX[YZ] hay q pasarle PosX=iX[Tini[img]:Tend[img]]
; a Time hay q pasarle Time=T[Tini[img]:Tend[img]]

function PROBA_DO_IGM, Lines=nLines, Columns=nCols, FOV=FOV, iFOV=iFOV, PitchAxis=uEjePitch, PitchAngle=uPitchAng, $
						 RollAxis=uEjeRoll, RollAngle=uRollAng, YawAxis=uEjeYaw, TgtAlt=TgtAlt, PosX=iX, PosY=iY, PosZ=iZ, Time=Time, Mode=Mode

ver = 'v1.0'

;---- Constantes ------------------------------------------------------------------
aEarth = 6378.137D		; Radio [km] de la Tierra en el Ecuador WGS84
f = 1/298.257223563D	; Factor de achatamiento de la Tierra WGS84

jd0 = julday(1,1,2003)		; Origen de tiempo del NJD (New Julian Day) q me he inventado para q en las graficas no se vaya de baras el eje de tiempos ya q el JD tiene demasiadas cifras :(

X=0
Y=1
Z=2
;----------------------------------------------------------------------------------


LoS=dblarr(3,nLines,nCols)		; LoS = [XYZ,nLines,nCols]

Mode = keyword_set(Mode) ? Mode : 1						; By default Mode 1 is considered. In reality it does not matter except for Mode 5
IF Mode EQ 5 THEN ScanAng = (dindgen(nCols)+0.5)*IFOV-FOV $		; In Mode 5 the last pixel is the one pointing to the target, i.e. ScanAng must equal zero for the last pixel.
	ELSE ScanAng = (dindgen(nCols)+0.5)*IFOV-FOV/2.D			; ScanAng is the rotation that must be applied to the Line of Sight in order to point properly for each pixel.


FOR L=0,nLines-1 DO begin
	qPitch = QTCOMPOSE(uEjePitch[*,L], -uPitchAng[L])	; Quaternion for pitch rotation
	newRoll = QTVROT(uEjeRoll[*,L], qPitch)				; New-Roll-Axis: Rotates the Roll-axis around the Pitch-axis to match the Scan-Plane normal vector

	fwdLoS = QTVROT(-uEjeYaw[*,L], qPitch)				; Rotates the Downward LoS (-Yaw_Axis) around pitch-axis to get the Forward LoS

	qRoll = QTCOMPOSE(newRoll, uRollAng[L])				; Quaternion for roll rotation around the new-roll-axis
	Nadir2 = QTVROT(fwdLoS, qRoll)						; Rotates the Forward LoS around new-roll-axis

	FOR C=0,nCols-1 DO begin
		qRoll_VL = QTCOMPOSE(newRoll, ScanAng[C])	; Quaternion for rotating along the CCD space lines
		LoS[*,L,C] = QTVROT(Nadir2, qRoll_VL)	; Line of Sight
	ENDFOR
ENDFOR

LoS_TGT = dblarr(3,nLines,nCols)
LoS_Range = dblarr(nLines,nCols)
LoS_Img = dblarr(3,nLines,nCols)
IGM = fltarr(nCols,nLines,2)

Sphr_R = [aEarth, aEarth, (1-f)*aEarth]+TgtAlt			; The geoide surface has an altide equal to the Target's mean altitude (from the metadata).

FOR L=0,nLines-1 DO begin
	SatPos = [iX[L],iY[L],iZ[L]]

	FOR C=0,nCols-1 DO begin
		tmp = Line_Sphr_i(reform(LoS[*,L,C],3), Sphr_R, SatPos)
		LoS_Range[L,C] = min([SQRT(TOTAL((SatPos-tmp[*,0])^2)), SQRT(TOTAL((SatPos-tmp[*,1])^2))], mn_ndx)
		LoS_TGT[*,L,C] = tmp[*,mn_ndx]

		tmp = eci2ecf(Time[L]+jd0, LoS_TGT[X,L,C], LoS_TGT[Y,L,C], LoS_TGT[Z,L,C])
		LoS_Img[X,L,C]=tmp.X
		LoS_Img[Y,L,C]=tmp.Y
		LoS_Img[Z,L,C]=tmp.Z

		tmp = ecf2gdt(LoS_Img[X,L,C], LoS_Img[Y,L,C], LoS_Img[Z,L,C], /KM)
		IGM[C,L,0]=float(reform(tmp.LON))
		IGM[C,L,1]=float(reform(tmp.LAT))

	ENDFOR
ENDFOR

return, IGM

END
