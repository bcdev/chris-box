; GST calculates the Greenwich sideral time for a given epoch (date and time)
; The algorithm does not have into account Nutation (a minor correction)

function GST, DateTime, RADIANS=rad

IF N_PARAMS() EQ 0 then begin
	print, '% GST calculates the Greenwich sideral time for a given epoch (date and time)'
	print, '% Usage:'
	print, '%    Result = GST(DateTime) [, /RADIANS]'
	print, '%'
	print, '% DateTime parameter required, in JulDay (Float or Double) or "YYYY-MM-DD hh:mm:ss" format (String)'
	print, '% The result is given in degrees, unless the /RADIANS keyword is set'
	print, '%'
	print, '% The calculation is based on the 1992 Astronomical Almanac, page B6.'
	RETURN, !values.f_nan
ENDIF

pt = size(datetime, /type)

CASE 1 OF
	(pt eq 4) OR (pt eq 5): begin      ; Case datetime is float or double
		caldat, datetime, mm, dd, yy, hh, mn, ss
		JD = julday(mm, dd, yy, 0, 0, 0)  ; Julian Day
		dt = (hh*60. + mn)*60. + ss       ; Number of seconds elpased in the day
	END
	(pt eq 7): begin                   ; Case datetime is string
		IF (strpos(datetime,'-') NE 4) OR (strpos(datetime,'-',/reverse_search) NE 7) OR (strpos(datetime,':') NE 13) OR (strpos(datetime,':',/reverse_search) NE 16) THEN begin
			print, '% Date-Time must be in "YYYY-MM-DD hh:mm:ss[.ss]" format'
			RETURN, !values.f_nan		; Return NaN in case datetime variable type is string but the value is not correctly formatted
		ENDIF		               		; NOTE: It should be checked that the numbers correspond to valid values
		n = strlen(datetime)
		ss = strmid(datetime,17,2)
		IF (n GT 19) AND (strmid(datetime,19,1) EQ '.') THEN ss0=strmid(datetime,17,n-17)
		mn = strmid(datetime,14,2)
		hh = strmid(datetime,11,2)
		dd = strmid(datetime,8,2)
		mm = strmid(datetime,5,2)
		yy = strmid(datetime,0,4)
		JD = julday(mm, dd, yy, 0, 0, 0)  ; Julian Day
		dt = (hh*60. + mn)*60. + ss       ; Number of seconds elpased in the day
	END
	ELSE: begin
		print, '% Wrong type. Datetime must be Float, Double or String in "YYYY-MM-DD hh:mm:ss.ss" format.'
		RETURN, !values.f_nan
	END
ENDCASE

; ----  Reference:  The 1992 Astronomical Almanac page B6, after Dr. T. S. Kelso

wE = 1.00273790934D
TU = (jd - 2451545.0D) / 36525.D				; Reference Epoch: 2000-1-1 12:00:00
GST0 = 24110.54841 + TU * (8640184.812866D + TU * (0.093104 - TU * 6.2D-6))	; [s]
GST = (GST0 + 1.00273790934D * dt) MOD 86400			; GST in seconds

;IF KEYWORD_SET(rad) THEN begin
	GST0 = (GST0 * !dpi /43200.D) MOD (2.D * !dpi)
	GST = (GST * !dpi / 43200.D) MOD (2.D * !dpi)
	wE = wE * !dpi /43200.D
	RETURN, {Tg:GST, Tg0:GST0, wE:wE, unit:'rad'}
;ENDIF ELSE begin
;	GST0 = (GST0 * 180.D / 43200.D) MOD 360.
;	GST = (GST * 180.D / 43200.D) MOD 360.
;	wE = wE * 180.D / 43200.D
;	RETURN, {Tg:GST, Tg0:GST0, wE:wE, unit:'deg'}
;ENDELSE

RETURN, !values.f_nan       ; it should never arrive to this point

END


PRO GST_TEST

PRINT, GST(2454790.125D)
PRINT, GST('2008-11-19 15:00:00')

END

; Output from GST_TEST
;
;{       4.9569015       1.0191589   7.2921159e-05 rad}
;{       4.9569015       1.0191589   7.2921159e-05 rad}
