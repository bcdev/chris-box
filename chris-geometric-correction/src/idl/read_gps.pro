; ##### Open PROBA GPS Telemetry Files ######
; CUIDADO!! No realiza comprobaciones sobre los archivos a abrir
; Requiere abrir el Template: GPS_template.sav de PROBA_toolbox
;
; Version 1.2	Searches GPS template for ASCII importing if not found in working directory
;		Added TimeType tag to returned structure to reflect if the time frame is GPS or UT1/UTC
;		Added TimeDelay tag to returned structure to reflect the correction applied to the GPS time delay
; Version 1.5	Included DELAY keyword in order to make optional the time correction


function read_gps, file, dGPStime=dTgps, delay=delay

; dTgps - substracts to GPS time the accumulated time difference with UT1/UTC due to "leap seconds"
; delay - Correction of delay in GPS time tagging. Ref: "In-flight Performance Analysis of the PROBA Onboard Navigation System"

IF N_PARAMS() EQ 0 THEN begin
	file=dialog_pickfile(title='Select GPS Telemetry file', path='~/proyectos', filter='*GPS_Data*')
	IF file EQ '' THEN RETURN, -1
ENDIF

IF strpos(file, 'GPS_Data') EQ -1 THEN RETURN, -1

; restore, dialog_pickfile(filter='gps*.sav', path='/Applications/rsi/librerias/PROBA_toolbox')
sep = path_sep()
cd, '.', current=path
tFile = path+sep+'gps_template.sav'
IF NOT file_test(tFile) THEN file = file_search(tFile, /fully_qualify_path)
IF NOT file_test(tFile) THEN file = dialog_pickfile(filter='gps*.sav')
IF tFile EQ '' THEN message, 'GPS Template not found'
restore, tFile				; El Template deberia estar en la ruta de busqueda
gps = read_ascii(file, template=gps_template)

dTgps = keyword_set(dTgps) ? dTgps : 0
IF dTgps THEN begin     ; If dTgps /= 0 it implies convertion to the UT time frame (no check on the proper value passed to the function).
	IF dTgps - fix(dTgps) THEN TimeType = 'UT1' ELSE TimeType = 'UTC'
ENDIF ELSE TimeType = 'GPS'

delay = n_elements(delay) ? delay : 0.999	;If no argument is passed, the GPS time is corrected according to the documented delay

gps.secs = gps.secs - delay		;Correction of delay in GPS time tagging. Ref: "In-flight Performance Analysis of the PROBA Onboard Navigation System"
gps.secs = gps.secs - dTgps		;Convert GPS time to UT time frame if dTgps is given.

JD = gpstime2datetime(gps.week, gps.secs, /JD)  ; Calculate Corresponding Julian Day

ndx = where(gps.secs NE shift(gps.secs,-1))		; In the telemetry files some packages are not updated, thus containing outdated (i.e. repeated) data

RETURN, {TAG:gps.time[ndx], X:gps.posx[ndx], Y:gps.posy[ndx], Z:gps.posz[ndx], VX:gps.VelX[ndx], VY:gps.vely[ndx], VZ:gps.velz[ndx], week:gps.week[ndx], secs:gps.secs[ndx], JD:JD[ndx], $
	Coord:'ECF', TimeType:TimeType, TimeDelay:delay, Units:'[] 3[m] 3[m/s] [weeks] [s] [days]'}

END
