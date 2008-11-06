; Converts GPS Weeks and Seconds to Date/Time or Julian Day
; GPS Time origin is 1980-01-06 00:00:00

function GPStime2datetime, gpsweek, gpssecs, JD_output=JDout

; NOTA: No chequea los valores de entrada
IF N_PARAMS() NE 2 THEN begin
	print, '% Converts GPS Time to Date/Time'
	print, '% Usage: Result = GPStime2DateTime(GPSweek, GPSsecs [, /JD_output])'
	print, '% Output: a structure containing Year, Month, Day, Hours, Minutes, Seconds'
	print, '% Set keyword /JD_output to get the result in Julian Days'
	RETURN, -1
ENDIF

gpsdays = gpssecs / 86400.D
jd = julday(1,6,1980,0,0,0) + gpsweek*7.D + gpsdays

IF KEYWORD_SET(JDout) THEN RETURN, jd

caldat, jd, mm, dd, yy, hh, mn, ss          ; Transforms Julian Days to Date/Time
RETURN, {yy:yy, mm:mm, dd:dd, hh:hh, mn:mn, ss:ss}

END
