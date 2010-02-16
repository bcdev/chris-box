; Abre los archivos de Telemetria ICT, los filtra y los pasa a Dias Julianos

function read_ict, file, dGPStime=dTgps

; dTgps substracts to GPS time the accumulated time difference with UT1 due to "leap seconds"

IF N_PARAMS() EQ 0 THEN begin
	file = dialog_pickfile(title='Select ICT Telemetry file', filter='*CHRIS_center_times*')
	IF file EQ '' THEN RETURN, -1
ENDIF

; Checks if the filename contains the "magic words". Weak validation of file type.
IF strpos(file, 'CHRIS_center_time') EQ -1 THEN begin
	print, "% Not a 'CHRIS_center_time' file"
	RETURN, -1
END

; If no leap secods provided it uses a value of zero to transform to Julian Day.
; It should read the date from the first column and use GPS2UT()
; If the telemetry parameter GPR122 is available it should be used instead of GPS2UT()
dTgps = keyword_set(dTgps) ? dTgps : 0
IF dTgps THEN begin     ; If dTgps /= 0 it implies convertion to the UT time frame (no check on the proper value passed to the function).
	IF dTgps - fix(dTgps) THEN TimeType = 'UT1' ELSE TimeType = 'UTC'
ENDIF ELSE TimeType = 'GPS'

; Searches for the template to open the ASCII file (defines which columns to import and which vartype to use for each).
sep = path_sep()        ; Gets the path separator character depending on the system running (\ for Win, / for the rest)
cd, '.', current=path
tFile = path+sep+'ict_template.sav'
IF NOT file_test(tFile) THEN tFile = file_search(tFile, /fully_qualify_path)
IF NOT file_test(tFile) THEN tFile = dialog_pickfile(filter='ict*.sav')
IF tFile EQ '' THEN message, 'GPS Template not found'
restore, tFile				; The Template should be in the search path

; Reads the ASCII file
ict = read_ascii(file, template=ict_template)

; Gets only the data that was updated (+G) discarding the rest (-G)
ndx_ict = where((strmid(ict.ict1_pkt, 0, 8) EQ '+G:29494'), c)
IF c EQ 0 THEN RETURN, -1       ; If no updated data found, return -1 and exit
ndx_fb = where((strmid(ict.flyby_pkt, 0, 9) EQ '+G:294944'))

jd0 = julday(12, 26, 1999, 0, 0, 0.00)	; Time reference (checked with ESTEC)

; Convert from secs to Julian Days
FB = (ict.flyby[ndx_fb]-dTgps) / 86400D + jd0
ICT1 = (ict.ict1[ndx_ict]-dTgps) / 86400D + jd0
ICT2 = (ict.ict2[ndx_ict]-dTgps) / 86400D + jd0
ICT3 = (ict.ict3[ndx_ict]-dTgps) / 86400D + jd0
ICT4 = (ict.ict4[ndx_ict]-dTgps) / 86400D + jd0
ICT5 = (ict.ict5[ndx_ict]-dTgps) / 86400D + jd0

RETURN, {TAG_FB:ict.time[ndx_fb], TAG_ICT:ict.time[ndx_ict], FBT:FB, ICT1:ICT1, ICT2:ICT2, ICT3:ICT3, ICT4:ICT4, ICT5:ICT5, TimeType:TimeType, Units:'JD'}
END


; Image Center Times file structure:
;
; Data columns separated by TABs
; Each line stores: Time, Data Package Number, [Parameter Quality Info, Parameter Value] (these two repeated as many as Parameters are logged)
;
; TIME	= Date/Time Stamp of Logging
; PKT	= Data stream package number
; PKT.AC2030.raw	AC2030.raw     = Fly-By Time	
; PKT.AC2659.raw	AC2659.raw	   = Image Center Time for frame 1
; PKT.AC2660.raw	AC2660.raw	   = Image Center Time for frame 2
; PKT.AC2661.raw	AC2661.raw	   = Image Center Time for frame 3
; PKT.AC2662.raw	AC2662.raw	   = Image Center Time for frame 4
; PKT.AC2663.raw	AC2663.raw     = Image Center Time for frame 5
;   Units are in seconds since 26-12-1999 00:00:00.00
;
;TIME:          21 chars, data separated by DOTs "." except the last one for decimal seconds
;  Year          1-4     INT
;  Day Of Year   6-8     INT
;  Hour          10-11   BYTE
;  Minute        13-14   BYTE
;  Second        16-21   FLOAT
;
;PACKET:        16 chars, data is LONG (6 digits) with trailing spaces " "
;
;Param PKT:     19 chars, data is a string code (2 chars) separated by ":" of a LONG number (6 digits) followed by trailing spaces " "
; Updated       40-41   STRING (+G = updated, -G = not updated)
; Pkt Nr.       43-48   LONG
;
;Param Value:   20 chars, data is DOUBLE (18 digits) with trailing spaces " "
