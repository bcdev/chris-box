FUNCTION GPS2UT, date, utc=utc, JD=jd_f

; Returns the time difference between GPS and UT1 (UTC if keyword /UTC is set)
; ATTENTION: The  return value must be SUBTRACTED to the GPS value!! to keep compatibility with previous versions

; Time difference due to leap seconds between UTC (stellar) and TAI (atomic) times can be found here:
; 	ftp://maia.usno.navy.mil/ser7/tai-utc.dat
; An easier file for importing can fe found at: 
; 	ftp://maia.usno.navy.mil/ser7/leapsec.dat
; More information in Section "Table of time scales 1972-present, and some predictions" at http://stjarnhimlen.se/comp/time.html
; The official time difference UT1-UTC can be found at http://maia.usno.navy.mil/ser7/finals.data with legend at http://maia.usno.navy.mil/ser7/readme.finals (collums 155-165 contain the most exact value, published by Bulletin B of the IERS -International Earth Rotation and Reference Systems Service-)
; IERS Bulletin C provides prediction of Leap Seconds (UTC-TAI): ftp://hpiers.obspm.fr/iers/bul/bulc/bulletinc.dat 
; GPS Time has a constant difference with TAI of 19sec
;
; UTC-GPS = 19 - (UTC-TAI)
; UT1-GPS = (UTC-GPS) + (UT1-UTC)
;                                                             
;                                                 ET 1960-1983
;                                                TDT 1984-2000
; UTC 1972-  GPS 1980-    TAI 1958-               TT 2001-    
;----+---------+-------------+-------------------------+----- 
;    |         |             |                         |      
;    |<------ TAI-UTC ------>|<-----   TT-TAI    ----->|      
;    |         |             |      32.184s fixed      |      
;    |<GPS-UTC>|<- TAI-GPS ->|                         |      
;    |         |  19s fixed  |                         |      
;    |                                                 |      
;    <> delta-UT = UT1-UTC                             |      
;     | (max 0.9 sec)                                  |      
;-----+------------------------------------------------+----- 
;     |<-------------- delta-T = TT-UT1 -------------->|      
;    UT1 (UT)                                       TT/TDT/ET 

; dTime = [-(UTC-GPS),(UT1-UTC)]
; Returns dTime[0]-dTime[1]  or  dTime[0] if /UTC is activated
; ATTENTION: The  return value must be SUBTRACTED to the GPS value!! to keep compatibility with previous versions

; The proper way to do this would be to get the finals.data online and extract the UT1-UTC for the actual date (instead of using the rough table below)
; I'm not sure how much accuracy is needed for CHRIS GeoCor

;; New code to get the leap second from the official file

IF n_params() EQ 0 THEN begin
    print, '% GPS2UT returns the offset (in seconds) to be substracted to the GPS Time for the given date to convert it to UT1 (to UTC if keyword is set)'
    print, '% Usage:  Result = GPS2UT(datetime [, /UTC][, /JD])'
ENDIF
jd_f = keyword_set(jd_f) ? 1 : 0
utc = keyword_set(utc) ? 1 : 0

sep = path_sep()        ; Gets the path separator character depending on the system running (\ for Win, / for the rest)
cd, '.', current=path


;=== Reads Leap Seconds File ===

tFile = path+sep+'leapsec.dat'
IF NOT file_test(tFile) THEN tFile = file_search('.','leapsec.dat', /fully_qualify_path)
IF NOT file_test(tFile) THEN tFile = dialog_pickfile(filter='leapsec.dat', title='Select Leap Second file', get_path=iPath)
IF tFile EQ '' THEN message, 'Not found file "leapsec.dat" containing updated leap second info'+string(10b)+'File might be downloaded from ftp://ftp://maia.usno.navy.mil/ser7/leapsec.dat'
; openr, iLun, tFile, /get_lun
; MM='' & kk1='' & kk2='' & kk3=''
; while NOT EOF(iLun) DO readf, iLun, YY,MM,DD, kk1, JD, kk2, LS, kk3, format='(I5,A4,I3,A4,F10,A10,F6,A)'

KK = READ_ASCII(tFile)      ; Reads the data into a structure kk, which contains one variable (kk.field01) with the data in matrix form
JD0 = reform(kk.field01[4,*])
caldat, JD0, mm, dd, yy     ; Converts the Julian Day stored in the 5th column into Month (mm), Day (dd) and Year (yy)
IF jd_f THEN LS_date0 = JD0 ELSE LS_date0 = yy*10000L + mm*100 +  dd

LS0 = reform(kk.field01[6,*]) - 19               ; Leap seconds, from TAI-UTC to GPS-UTC

nd = n_elements(LS_date0)
IF nd NE n_elements(LS0) THEN message, 'Error parsing Leap Second file: '+tFile

;Print, "% GPS2UT: Leap Seconds last updated on "+strtrim(dd[nd-1],2)+"-"+strtrim(mm[nd-1],2)+"-"+strtrim(yy[nd-1],2)+"."


;=== Reads UT1-UTC File ===

tFile = path+sep+'finals.data'
IF NOT file_test(tFile) THEN tFile = file_search(iPath,'finals.data', /fully_qualify_path)
IF NOT file_test(tFile) THEN tFile = dialog_pickfile(filter='finals.data', title='Select UT1-UTC file', path=iPath)
IF tFile EQ '' THEN message, 'Not found file "finals.data" containing updated UT1-UTC info'+string(10b)+'File might be downloaded from ftp://ftp://maia.usno.navy.mil/ser7/finals.data'

qq = query_ascii(tFile, q)

line='' & yy=0 & mm=0 & dd=0 & kk1='' & kk2='' & dT_f=''
openr, iLun, tFile, /get_lun
point_lun, iLun, q.bytes/q.lines * 3582         ; Jumps the file to the begining of the line corresponding to the 22-10-2001, date of launch of CHRIS/PROBA
readf, iLun, line, format='(A)'
reads, line, yy, mm, dd, mjd, kk1,dT_f, dT, kk2, format='(I2,I2,I2,F9,A42,A1,F10,A)'
IF (yy+2000)*10000L + mm*100 + dd NE 20011022 THEN message, 'Error accessing "finals.data", line 3583 does not correspond to date: 22-10-2001'

nLines = q.lines - 3582
dT_date0 = lonarr(nLines)      ; Array of date YYYYMMDD
MJD0 = fltarr(nLines)       ; Array of Modified Julian Date
dT0 = fltarr(nLines)        ; Array of UT1-UTC values
dT0_f = bytarr(nLines)      ; Array of Calculated (1) / Predicted (0) flag

dT_date0[0] = (yy+2000)*10000L + mm*100 + dd
mjd0[0] = mjd
dT0[0] = dT
dT0_f[0] = dT_f EQ 'I' ? 1 : 0

i = 0
WHILE NOT eof(iLun) DO begin
    i = i + 1
    readf, iLun, yy, mm, dd, mjd, kk1,dT_f, dT, kk2, format='(I2,I2,I2,F9,A42,A1,F10,A)'
    dT_date0[i] = (yy+2000)*10000L + mm*100 + dd
    MJD0[i] = mjd
    dT0[i] = dT
    dT0_f[i] = dT_f EQ 'I' ? 1 : 0
ENDWHILE


;=== Find the proper Leap Second and UT1-UTC values ===

IF size(date, /type) EQ 7 THEN date = long(date)
; Here it should check validity of the date passed: YYYYMMDD with 1980<AAAA<present year, 0<MM<13, 0<DD<31 (depending on MM) 

LS_w = where(date GE LS_date0, nw)
IF nw EQ 0 THEN message, 'datetime is not within the range of GPS historical data.'

LS = LS0[LS_w[nw-1]]

IF NOT utc THEN begin
    dT_w = where(date EQ dT_date0)
    IF dT_w EQ -1 THEN message, 'datetime is not within the range of GPS historical data.'
    
    dT = dT0[dT_w]
    IF NOT dT0_f[dT_w] THEN print, 'Warning: Using a PREDICTED value for UT1-UTC. If you are calculating a past date it is recommended that you update the "finals.data" file.'
ENDIF

IF UTC THEN return, LS ELSE return, (LS-dT)

END
