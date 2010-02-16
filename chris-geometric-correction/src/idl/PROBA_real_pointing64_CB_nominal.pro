 FORWARD_FUNCTION read_ict, read_gps, read_tle_eci, open_info, closest, ecf2eci, gdt2ecf, vect_prod, unit, eci2ecf, angvel, open_realtgt, $
                  qtcompose, qtvrot, viewangs, vect_angle, sign, tvscale, colorbar, envi_header, proba_do_igm, file_basename_u, $
                  plot_angdif_new, pllot_derivangs, plot_vectfield, proba_igm_georect

 PRO PROBA_real_pointing

ver = '6.4 CB'	; Release version of the Nominal Case for CHRIS-Bo

gcp_f=0			; Take into account GCP for moving target correction
do_georect=0	; Performs Georectification from calculated IGM

; ==v==v== Constants ==v==v==
SlowDown = 5            ; Slowdown factor

aEarth = 6378.137D		; Earth Radius [km] at Equator (WGS84)
f = 1/298.257223563D	; Earth Flatening Factor (WGS84)

jd0 = julday(1,1,2001)	; Epoch for a reduced Julian Day (all JD values are substracted by this value). This way the calculations can be performed more efficiently.

X=0						; Variables used for array subscripting, so the code is more readable.
Y=1
Z=2

dTgps = 13				; Time diference between GPS Time and UT1 (year dependent). In 2009 it will be 14.
						; It is CRITICAL to substract it to both GPS and ITC data
info_ndx=[3,1,0,2,4]	; This is used to access metadata from the info file. Relates the acquisition sequence with image tagging.
; ==-==-==



;=======================================================================
;===                    Start Up & Input Data                        ===
;=======================================================================

; ==v==v== Read Metadata ==v==v==
;
; NOTE: I stored an acquisition set metadata in a .info file. These metadata should be already available at BEAM.

filen = dialog_pickfile(title='Select CHRIS-PROBA Info file', filter='*.info', path=path, get_path=path)
if filen EQ '' then STOP

info = open_info(filen)
if size(info, /type) EQ 2 then STOP		; If properly read, info should be a structure (type 8), otherwise -1 is returned, i.e. a scalar (type 2)
date_ = strsplit(info.date,'-',/extr)	; transfroms the date retrieved from info from YYYY-MM-DD to YYYYMMDD needed to detect automatically the proper ICT telemetry file
date = strjoin(date_)
; ==-==-==

; ==v==v== Read ICT ==v==v==
; filter='*CHRIS_center_times_'+date+'*' allows to determine automatically the correct ICT file
file = dialog_pickfile(title='Select ICT Telemetry file', filter='*CHRIS_center_times_'+date+'*', path=path, get_path=path)
if file EQ '' then STOP

ict = read_ict(file, dGPStime=dTgps)
if size(ict, /type) EQ 2 then STOP		; If properly read, ict should be a structure (type 8), otherwise -1 is returned, i.e. a scalar (type 2)
code = strmid(file, strpos(file,'_CHRIS')-5, 5)		; allows determine the proper GPS telemetry file
; ==-==-==

; ==v==v== Read GPS ==v==v==
; filter='*'+code+'*GPS_Data*'	allows determine automatically the correct GPS file
file = dialog_pickfile(title='Select GPS Telemetry file', filter='*'+code+'*GPS_Data*', path=path, get_path=path)
if file EQ '' then STOP

gps = read_gps(file, dGPStime=dTgps, delay=0.999)	; there is a delay of 0.999s between the GPS time tag and the actual time of the reported position/velocity.
if size(gps, /type) NE 8 then STOP		; If properly read, gps should be a structure (type 8), otherwise -1 is returned, i.e. a scalar (type 2)
; ==-==-==


; ==v==v== Read GCP ==v==v==
; Reads Lat, Lon, Alt and corresponding X and Y pixel coords
; In case Alt is missing mean target altitude (from metadata) is assumed

IF GCP_f THEN begin
	GCP = fltarr(6,n_gcp)	; GCP depends on Pixel_X, Pixel_Y, Lon, Lat, Alt and image number (reads GCP for all images, so each point must be identified with the corresponding image)
	; Here should go: GCP = read_gcp(file)
	; In case Altitude is not available in the GCP selection, the main target altitude in CHRIS metadata should be assigned
ENDIF


; ==-==-==

; ==v==v== Constantes depedientes del CHRIS-MODE ==v==v==
CASE info.mode OF
	1: BEGIN
		nCols = 372L				; Number of Columns (372 detected, 374 reported)
		nLines = 374L				; Number of Lines
		TpL = 24.1899D-03			; Integration Time per Line [s]
		FOV = 1.29261 *!dpi/180		; Field Of View [rad]
	END
	5: BEGIN
		nCols = 370L
		nLines = 748L
		TpL = 11.4912D-03
		FOV = 0.63939 *!dpi/180
	END
	; Modes 0 and 20 characteristics are missing (already requested to Mike Cutter)
	ELSE: BEGIN
		; Modes 2, 3, 3A(30) and 4 share characteristics 
		nCols = 744L
		nLines = 748L
		TpL = 11.4912D-03
		FOV = 1.28570 *!dpi/180
	END
ENDCASE

; ==-==-==



; ========================================================================
; ===                     Prepare Time Frames                          ===
; ==v==v== ==v==v== ==v==v== ==v==v== ==v==v== ==v==v== ==v==v== ==v==v===
FTT = 1.2096D-03				; Frame Transfer Time: [s]  every line. common to all modes
IFOV = FOV / nCols				; Instant Field of View [rad]
dT = TpL + FTT					; Total time for one line
Timg = nLines * dT				; Time for one image acquisition

;---- get Image Center Time from the last element of the telemetry ------------
n = n_elements(ict.ict1) - 1
ict_njd = [ict.ict1[n], ict.ict2[n], ict.ict3[n], ict.ict4[n], ict.ict5[n], ict.ict1[n]-(10+390)/86400D] - jd0
;																						  ^- Suponemos q el inicio de la adquisicion se refiere a PROBA (20s) y no a CHRIS (9.5s)
; The last element of ict_njd corresponds to the acquisition setup time, that occurs 390s before the start of acquisition.
T_ict = ict_njd[0:4]

;---- Nos quedamos con todos los elementos de GPS menos el ultimo ----------------
;----   ya q es donde se almacena la AngVel media, y perder un dato --------------
;----   de GPS no es un problema. ------------------------------------------------
n = n_elements(gps.jd) - 2
gps_njd = gps.jd[0:n] - jd0

;---- Critical Times ---------------------------------------------
T_ini = ict_njd[0:4] - (Timg/2)/86400.D		; imaging start time (imaging lasts ~9.5s in every mode)
T_end = ict_njd[0:4] + (Timg/2)/86400.D		; imaging stop time
T_i = ict_njd[0]-10/86400.D					; "imaging mode" start time
T_e = ict_njd[4]+10/86400.D					; "imaging mode" stop time

; Searches the closest values in the telemetry to the Critical Times (just for plotting purposses)
Ti_ndx = closest(T_ini[0], gps_njd)
Te_ndx = closest(T_end[4], gps_njd)

;---- determine per-Line Time Frame -----------------------------------
; Time elapsed since imaging start at each image line
T_lin = (dindgen(nLines) * dT + TpL/2)/86400.D
;									^- +TpL/2 is added to set the time at the middle of the integration time, i.e. pixel center

T_img = dblarr(nLines, 5)
FOR i = 0, 4 DO T_img[*,i] = T_ini[i]+T_lin

T = [ict_njd[5], reform(T_img, 5*nLines)]

Tobs = T_e - T_i			; Duration of "imaging mode" for each image. (redundant, should be 20s)
Tobs2 = T_end - T_ini		; Duration of actual imaging for each image

; Set the indices of T that correspond to critical times (integration start and stop) for each image
Tini = nLines * [0,1,2,3,4] + 1
;							  ^-  The first element of T corresponds to the acquisition-setup time, so Tini[0] must skip element 0 of T
Tend = Tini + nLines - 1
Tfix = 0			; Indice correspondiente al tiempo de fijacion de la orbita
nT = n_elements(T)

;---- Larger time span for moving target in order to find along track pointing offsets using GCPs -----------------------------------
;nTgt = (T_e - T_i)/(dT/86400.D/2)			; Number of Moving Target points
;T_tgt_ = dindgen(n_tgt)*dT/86400.D/2		; Moving Target will be sampled at twice the time of the scanning, therefore dT/2
;T_tgt = dblarr(nTgt,5)
;FOR i = 0, 4 DO T_tgt[*,i] = T_i[i]+T_tgt_
;
;TT = reform(T_tgt, 5*nTgt)					; Times for the Moving Target for each of the imaging mode periods (at twice the sampling rate of CHRIS)
;
;nTT = n_elements(TT)
; ==-==-==


; ========================================================================
; ===                     Inertial Coordinates                         ===
; ==v==v== ==v== Converts coordinates from ECEF to ECI ==v==v== ==v==v== =

; ---- Pos/Vel with Time from Telemetry --------------------------------
eci = ecf2eci(gps.jd, gps.X/1000., gps.Y/1000., gps.Z/1000., gps.VX/1000., gps.VY/1000., gps.VZ/1000., units='[km]')
;                             ^- position and velocity is given in meters, we transform to km in order to keep the values smaller. from now all distances in Km
; ==-==-==


;=======================================================================
;===                  Data to per-Line Time Frame                    ===
;=======================================================================
;---- Interpolate GPS ECI position/velocity to per-line time -------

iX = spline(gps_njd, eci.X[0:n], T, /double)
iY = spline(gps_njd, eci.Y[0:n], T, /double)
iZ = spline(gps_njd, eci.Z[0:n], T, /double)
iVX = spline(gps_njd, eci.VX[0:n], T, /double)
iVY = spline(gps_njd, eci.VY[0:n], T, /double)
iVZ = spline(gps_njd, eci.VZ[0:n], T, /double)
iR = SQRT(iX^2 + iY^2 + iZ^2)
; ==-==-==


; ==v==v== Get Orbital Plane Vector ==================================================

;---- Calculates normal vector to orbital plane --------------------------
Wop = vect_prod(iX,iY,iZ, iVX,iVY,iVZ)
uWop = unit(Wop)

uWecf = eci2ecf(T[Tfix]+jd0, uWop[X,Tfix], uWop[Y,Tfix], uWop[Z,Tfix], Units='Unitary')		; Fixes orbital plane vector to the corresponding point on earth at the time of acquistion setup
uW = ecf2eci(T+jd0, uWecf.X, uWecf.Y, uWecf.Z, Units='Unitary')								; Y calculamos su posicion ECI para cada Tiempo Critico
;uW = ecf2eci(TT+jd0, uWecf.X, uWecf.Y, uWecf.Z, Units='Unitary')							; Y calculamos su posicion ECI para cada Tiempo Critico
; ==-==-==

; ==v==v== Get Angular Velocity ======================================================
; Angular velocity is not really used in the model, except the AngVel at orbit fixation time (iAngVel[0])
AngVelRaw = AngVel(gps.secs, eci.x, eci.y, eci.z)
AngVel = smooth(AngVelRaw[0:n], 5)
iAngVel = spline(gps_njd, AngVel, T, /double)

; ==-==-==


; ==v==v== Initialize Variables ======================================================

Range = dblarr(3,nLines, 5)
EjeYaw = dblarr(3,nLines, 5)
EjePitch = dblarr(3,nLines, 5)
EjeRoll = dblarr(3,nLines, 5)
SP = dblarr(3,nLines, 5)
SL = dblarr(3,nLines, 5)
PitchAng = dblarr(nLines, 5)
RollAng = dblarr(nLines, 5)
PitchAngR = dblarr(nCols, nLines, 5)
RollAngR = dblarr(nCols, nLines, 5)
ObsAngAzi = dblarr(nLines, 5)
ObsAngZen = dblarr(nLines, 5)
; ==-==-==


; ===== Process each image separately ==========================================

FOR img = 0, 4 DO begin
print, 'Initiating calculation for image '+strtrim(img,2)


; ==v==v== Find the closest point in the Moving Target to the GCPs ==============
IF GCP_f THEN begin
	IF Mode EQ 5 THEN nCols2 = nCols-1 ELSE nCols2 = nCols/2 
	wGCP = where(GCP[5,*] EQ img)
	IF wGCP[0] NE -1 THEN begin
		d = sqrt((GCP[2,wGCP]-info.lon)^2 + (GCP[3,wGCP]-info.lat)^2)
		dmin = min(d, wmin)		; wmin stores the subscript of the minimum element
		GCP0 = GCP[0:4,wmin]
	ENDIF ELSE GCP0 = [nCols2, nLines2, info.lon, info.lat, info.alt]
ENDIF

; ==-==-==

; ---- Target Coordinates in ECI using per-Line Time -------------------
TgtAlt = info.alt/1000	; 
;TGTecf = gdt2ecf(info.lat, info.lon, TgtAlt, /km)
TGTecf = gdt2ecf(GCP0[3], GCP0[2], GCP0[4], /km)
iTGT0 = ecf2eci(T+jd0, TGTecf.X, TGTecf.Y, TGTecf.Z, units = TGTecf.units)			; Case with Moving Target for imaging time
;iTGT0 = ecf2eci(TT+jd0, TGTecf.X, TGTecf.Y, TGTecf.Z, units = TGTecf.units)		; Case with Moving Target for imaging mode time

; ==v==v== Rotates TGT to perform scanning ======================================

	qTscn = QTCOMPOSE(transpose([[uW.X[Tini[img]:Tend[img]]],[uW.Y[Tini[img]:Tend[img]]],[uW.Z[Tini[img]:Tend[img]]]]), (-1)^img * iAngVel[0] / SlowDown * (T_img[*,img] - T_ict[img])*86400D)
;	qTscn = QTCOMPOSE(transpose([[uW.X[TTini[img]:TTend[img]]],[uW.Y[TTini[img]:TTend[img]]],[uW.Z[TTini[img]:TTend[img]]]]), (-1)^img * iAngVel[0] / SlowDown * (T_tgt[*,img] - T_ict[img])*86400D)

	V = TRANSPOSE([[iTGT0.X[Tini[img]:Tend[img]]], [iTGT0.Y[Tini[img]:Tend[img]]], [iTGT0.Z[Tini[img]:Tend[img]]]])
;	V = TRANSPOSE([[iTGT0.X[TTini[img]:TTend[img]]], [iTGT0.Y[TTini[img]:TTend[img]]], [iTGT0.Z[TTini[img]:TTend[img]]]])

	tmp = QTVROT(V, qTscn)
	iTGT0.X[Tini[img]:Tend[img]] = tmp[X,*]		;iTGT0.X[TTini[img]:TTend[img]] = tmp[X,*]
	iTGT0.Y[Tini[img]:Tend[img]] = tmp[Y,*]		;iTGT0.Y[TTini[img]:TTend[img]] = tmp[Y,*]
	iTGT0.Z[Tini[img]:Tend[img]] = tmp[Z,*]		;iTGT0.Z[TTini[img]:TTend[img]] = tmp[Z,*]
; ==-==-==

; 
; Once GCP and TT are used iTGT0 will be subsetted to the corrected T, but in the nominal case iTGT0 matches already T
iTGT = iTGT0


;==== Calculates View Angles ==============================================

View = ViewAngs(iTGT.X[Tini[img]:Tend[img]], iTGT.Y[Tini[img]:Tend[img]], iTGT.Z[Tini[img]:Tend[img]], iX[Tini[img]:Tend[img]], iY[Tini[img]:Tend[img]], iZ[Tini[img]:Tend[img]], info.LAT)

ObsAngAzi[*,img] = View.Azi
ObsAngZen[*,img] = View.Zen

; Observation angles are not needed for the geometric correction but they are used for research. They are a by-product.
; But ViewAngs provides also the range from the target to the satellite, which is needed later (Range, of course could be calculated independently).

;==== Satellite Rotation Axes ==============================================

uEjeYaw = unit(transpose([[ iX[Tini[img]:Tend[img]] ], [ iY[Tini[img]:Tend[img]] ], [ iZ[Tini[img]:Tend[img]] ]]))
EjeYaw[*,*,img] = uEjeYaw

uEjePitch = uWop[*,Tini[img]:Tend[img]]
EjePitch[*,*,img] = uEjePitch

uEjeRoll = dblarr(3, FLOOR(nLines))

FOR i=0L,FLOOR(nLines)-1 DO uEjeRoll[*,i] = vect_prod(uEjePitch[*,i], uEjeYaw[*,i])
EjeRoll[*,*,img] = uEjeRoll

;---- View.Rang[XYZ] is the vector pointing from TGT to SAT,
;----  but for the calculations it is nevessary the oposite one, therefore (-) appears.
Range[*,*,img] = -transpose([[View.RangX],[View.RangY],[View.RangZ]])
uRange = unit(Range[*,*,img])

; ScanPlane:
uSP = dblarr(3,FLOOR(nLines))
uSPr = dblarr(3,FLOOR(nLines*nCols))

; SightLine:
uSL = dblarr(3,FLOOR(nLines))
uSLr = dblarr(3,FLOOR(nLines*nCols))

; RollSign:
uRollSign = intarr(FLOOR(nLines))
uRollSignR = intarr(FLOOR(nLines*nCols))

FOR i=0L,FLOOR(nLines)-1 DO begin
	uSP[*,i] = unit(vect_prod(uRange[*,i], uEjePitch[*,i]))			; hay q hacerlo unitario xq Range y EjePitch no son perpendiculares, aunque casi!!
	uSL[*,i] = unit(vect_prod(uEjePitch[*,i], uSP[*,i]))
	uRollSign[i] = sign(total(unit(vect_prod(uSL[*,i],uRange[*,i]))/uSP[*,i]))     ; ??? Por que hago esto asi?
ENDFOR
SP[*,*,img] = uSP
SL[*,*,img] = uSL

uPitchAng = !dpi/2 - Vect_Angle(uSP[X,*],uSP[Y,*],uSP[Z,*], uEjeYaw[X,*],uEjeYaw[Y,*],uEjeYaw[Z,*])
uRollAng = uRollSign * Vect_Angle(uSL[X,*],uSL[Y,*],uSL[Z,*], uRange[X,*],uRange[Y,*],uRange[Z,*])

; Stores the results for each image
PitchAng[*,img] = uPitchAng
RollAng[*,img] = uRollAng

;-----------------------
;   Here Pointing Angles will be updated using the Ground Control Points
;
; IF gcp_f THEN begin
; ENDIF
;-----------------------


;==== Rotate the Line of Sight and intercept with Earth ==============================================

print, '% Starting model-IGM generation'

IGMmod = PROBA_DO_IGM(Lines=nLines, Columns=nCols, FOV=FOV, iFOV=iFOV, PitchAxis=uEjePitch, PitchAngle=uPitchAng, $
						 RollAxis=uEjeRoll, RollAngle=uRollAng, YawAxis=uEjeYaw, TgtAlt=TgtAlt, PosX=iX[Tini[img]:Tend[img]], $
						 PosY=iY[Tini[img]:Tend[img]], PosZ=iZ[Tini[img]:Tend[img]], Time=T[Tini[img]:Tend[img]])

; ---- Output Calculated IGM to binary file -----
oFile = oPath+file_basename_u(info.file)+'_'+info.tag[info_ndx[img]]+'_model-IGM.img'
openw, oLun, oFile, /get_lun, /swap_if_big_endian
	writeu, oLun, IGMmod
free_lun, oLun
; ---- Write corresponding ENVI header file -----
dsc = ['IGM file for CHRIS/PROBA image: '+file_basename(oFile), 'WGS84 Geodetic coordinates', 'Created with PROBA_real_pointing6.pro '+ver]
ENVI_HEADER, ofile, COLUMNS=nCols, LINES=nLines, BANDS=2, DATA_TYPE=4, BYTE_ORDER=0B, DESC=dsc, B_NAMES=['WGS84 Longitude [Deg]','WGS84 Latitude [Deg]']


ENDFOR	; img


;============= Output Ancillary Data =============================================
;---- Stores the pointing angles in ENVI format
IF oPath NE '' THEN begin
	oFile=oPath+'Ang_Model_'+date+'.img'
	openw, olun, oFile, /get_lun
	for i=0,4 do begin
		writeu, olun, reform( ((dblarr(nCols)+1) # PitchAng[*,i]), nCols, nLines )
		writeu, olun, reform( ((dblarr(nCols)+1) # RollAng[*,i]), nCols, nLines )
	endfor
	close, olun
	free_lun, olun

	envi_header, oFile, col=nCols, lin=nLines, bands=10, data_type=5, description=['File generated by PROBA_real_pointing v'+ver,'Modelled Pointing Angles'], $
	  b_names=['Pitch +55 [rad]','Roll +55 [rad]','Pitch +36 [rad]','Roll +36 [rad]','Pitch +0 [rad]','Roll +0 [rad]','Pitch -36 [rad]','Roll -36 [rad]','Pitch -55 [rad]','Roll -55 [rad]']
ENDIF



;---- Outputs a text summary of the Observarion Angles to the screen ----

nL2 = nLines/2

print, '================================================================='
print, ' Model Observation Angles:'
print, '            ','    Tini   Tict[1]   Tend   |    Tini   Tict[2]   Tend   |    Tini   Tict[3]   Tend   |    Tini   Tict[4]   Tend   |    Tini   Tict[5]   Tend   |', FORMAT='(2A)'
print, ObsAngAzi[0,0]*!radeg, ObsAngAzi[nL2,0]*!radeg, ObsAngAzi[nLines-1,0]*!radeg, $
		ObsAngAzi[0,1]*!radeg, ObsAngAzi[nL2,1]*!radeg, ObsAngAzi[nLines-1,1]*!radeg, $
		ObsAngAzi[0,2]*!radeg, ObsAngAzi[nL2,2]*!radeg, ObsAngAzi[nLines-1,2]*!radeg, $
		ObsAngAzi[0,3]*!radeg, ObsAngAzi[nL2,3]*!radeg, ObsAngAzi[nLines-1,3]*!radeg, $
		ObsAngAzi[0,4]*!radeg, ObsAngAzi[nL2,4]*!radeg, ObsAngAzi[nLines-1,4]*!radeg, $
        FORMAT='("Azimuth[Deg]", 5(3F9.3," |"))'
print, ObsAngZen[0,0]*!radeg, ObsAngZen[nL2,0]*!radeg, ObsAngZen[nLines-1,0]*!radeg, $
		ObsAngZen[0,1]*!radeg, ObsAngZen[nL2,1]*!radeg, ObsAngZen[nLines-1,1]*!radeg, $
		ObsAngZen[0,2]*!radeg, ObsAngZen[nL2,2]*!radeg, ObsAngZen[nLines-1,2]*!radeg, $
		ObsAngZen[0,3]*!radeg, ObsAngZen[nL2,3]*!radeg, ObsAngZen[nLines-1,3]*!radeg, $
		ObsAngZen[0,4]*!radeg, ObsAngZen[nL2,4]*!radeg, ObsAngZen[nLines-1,4]*!radeg, $
        FORMAT='("Zenith [Deg]", 5(3F9.3," |"))'
print, '-----------------------------------------------------------------'
print, ' Official Observation Angles:'
print, '            ','    Tini   Tict[1]   Tend   |    Tini   Tict[2]   Tend   |    Tini   Tict[3]   Tend   |    Tini   Tict[4]   Tend   |    Tini   Tict[5]   Tend   |', FORMAT='(2A)'
print, '',info.azi[info_ndx[0]],'', '',info.azi[info_ndx[1]],'', '',info.azi[info_ndx[2]],'', '',info.azi[info_ndx[3]],'', '',info.azi[info_ndx[4]],'', $
		FORMAT='("Azimuth[deg]", 5(A9,F9.3,A9," |"))'
print, '',info.zen[info_ndx[0]],'', '',info.zen[info_ndx[1]],'', '',info.zen[info_ndx[2]],'', '',info.zen[info_ndx[3]],'', '',info.zen[info_ndx[4]],'', $
		FORMAT='("Zenith [deg]", 5(A9,F9.3,A9," |"))'
print, '-----------------------------------------------------------------'


;---- Outputs a text summary of the Observarion Angles to a file ----
IF oPath NE '' THEN begin
    oFile = oPath+'ObsAng_'+date+'_summary.txt'
	openw, olun, ofile, /get_lun
	printf, olun, '================================================================='
	printf, olun, ' Model Observation Angles:'
	printf, olun, '            ','    Tini   Tict[1]   Tend   |    Tini   Tict[2]   Tend   |    Tini   Tict[3]   Tend   |    Tini   Tict[4]   Tend   |    Tini   Tict[5]   Tend   |', FORMAT='(2A)'
	printf, olun, ObsAngAzi[0,0]*!radeg, ObsAngAzi[nL2,0]*!radeg, ObsAngAzi[nLines-1,0]*!radeg, $
		ObsAngAzi[0,1]*!radeg, ObsAngAzi[nL2,1]*!radeg, ObsAngAzi[nLines-1,1]*!radeg, $
		ObsAngAzi[0,2]*!radeg, ObsAngAzi[nL2,2]*!radeg, ObsAngAzi[nLines-1,2]*!radeg, $
		ObsAngAzi[0,3]*!radeg, ObsAngAzi[nL2,3]*!radeg, ObsAngAzi[nLines-1,3]*!radeg, $
		ObsAngAzi[0,4]*!radeg, ObsAngAzi[nL2,4]*!radeg, ObsAngAzi[nLines-1,4]*!radeg, $
        FORMAT='("Azimuth[Deg]", 5(3F9.3," |"))'
	printf, olun, ObsAngZen[0,0]*!radeg, ObsAngZen[nL2,0]*!radeg, ObsAngZen[nLines-1,0]*!radeg, $
		ObsAngZen[0,1]*!radeg, ObsAngZen[nL2,1]*!radeg, ObsAngZen[nLines-1,1]*!radeg, $
		ObsAngZen[0,2]*!radeg, ObsAngZen[nL2,2]*!radeg, ObsAngZen[nLines-1,2]*!radeg, $
		ObsAngZen[0,3]*!radeg, ObsAngZen[nL2,3]*!radeg, ObsAngZen[nLines-1,3]*!radeg, $
		ObsAngZen[0,4]*!radeg, ObsAngZen[nL2,4]*!radeg, ObsAngZen[nLines-1,4]*!radeg, $
        FORMAT='("Zenith [Deg]", 5(3F9.3," |"))'
	printf, olun, '-----------------------------------------------------------------'
	printf, olun, ' Official Observation Angles:'
	printf, olun, '            ','    Tini   Tict[1]   Tend   |    Tini   Tict[2]   Tend   |    Tini   Tict[3]   Tend   |    Tini   Tict[4]   Tend   |    Tini   Tict[5]   Tend   |', FORMAT='(2A)'
	printf, olun, '',info.azi[info_ndx[0]],'', '',info.azi[info_ndx[1]],'', '',info.azi[info_ndx[2]],'', '',info.azi[info_ndx[3]],'', '',info.azi[info_ndx[4]],'', $
			FORMAT='("Azimuth[deg]", 5(A9,F9.3,A9," |"))'
	printf, olun, '',info.zen[info_ndx[0]],'', '',info.zen[info_ndx[1]],'', '',info.zen[info_ndx[2]],'', '',info.zen[info_ndx[3]],'', '',info.zen[info_ndx[4]],'', $
			FORMAT='("Zenith [deg]", 5(A9,F9.3,A9," |"))'
	printf, olun, '-----------------------------------------------------------------'
	free_lun, olun
ENDIF



;====== Generates the Georectified Images ======

IF do_georect THEN PROBA_IGM_GEORECT, info=info, path=path, oPath=oPath, /UTM, first=1, last=2


END
