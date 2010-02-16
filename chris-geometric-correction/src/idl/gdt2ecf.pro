; Transforms geodetic coordinates (spherical) to Earth Centered Fixed ECF cartesian coordinates (rectangular)
; Altitude h starts at the reference geoid and by default must be in meters (if in kilometers /KM keyword must be set)
; By default Latitude and Longitude are operated in degrees, in case they are passed in radians /RAD keyword must be set
;

function GDT2ECF, iLat, iLon, iH, rad=rad, km=km

IF N_PARAMS() LT 2 THEN begin
	print, '% Converts Geodetic Coordinates (using the WGS84 geoid) to ECF Orthogonal Coordinates'
	print, '% usage: Result = GDT2ECF(Lat, Lon [, Alt] [, /RAD] [,/KM])'
	print, '%        lat, lon, alt are Latitude, Longitude and Altitude from reference geoid'
	print, '%        Lat and Lon must be in degrees unless /RAD is set'
	print, '%        Altitude must be in meter unless /KM is set'
	print, '%'
	print, '%        output is a structure {X, Y, Z, Coord, Units}'
	return, !values.f_nan
ENDIF

lat = iLat
lon = iLon

IF size(lat, /N_dim) GT 1 THEN begin
	lat = reform(lat)
	IF size(lat, /N_dim) GT 1 THEN begin
		print, '% ERROR: LAT must be a value or a vector.'
		return, !values.f_nan
	ENDIF
ENDIF
IF size(lon, /N_dim) GT 1 THEN begin
	lon = reform(lon)
	IF size(lon, /N_dim) GT 1 THEN begin
		print, '% ERROR: LON must be a value or a vector.'
		return, !values.f_nan
	ENDIF
ENDIF
IF N_ELEMENTS(lat) NE N_ELEMENTS(lon) THEN begin
	print, 'ERROR: LAT and LON must have the same size.'
	return, !values.f_nan
ENDIF

IF N_PARAMS() EQ 3 THEN begin
	h = iH
	IF SIZE(h, /N_DIM) GT 1 THEN BEGIN
		h = reform(h)
		IF SIZE(h, /N_DIM) GT 1 THEN RETURN, -5
	ENDIF
	IF (N_ELEMENTS(h) EQ 1) AND (N_ELEMENTS(lat) GT 1) THEN h = REPLICATE(h, N_ELEMENTS(lat))
	IF N_ELEMENTS(h) NE N_ELEMENTS(lat) THEN RETURN, -6
ENDIF ELSE h = lat * 0		; Esto es para q el vector Altura tenga el mismo tama�o q los otros vectores.

IF NOT KEYWORD_SET(rad) THEN begin
	lat = lat * !dPI / 180.D
	lon = lon * !dPI / 180.D
ENDIF

IF KEYWORD_SET(km) THEN h = h *1000.D

; ----WGS84----
f = 1 / 298.257223563D
a = 6378137.D
e = SQRT(2*f - f^2)
N = a / SQRT(1 - e^2 * sin(lat)^2)

X = (N+h)*cos(lat)*cos(lon)
Y = (N+h)*cos(lat)*sin(lon)
Z = ((1-e^2)*N + h)*sin(lat)

IF KEYWORD_SET(km) THEN RETURN, {X:X/1000, Y:Y/1000, Z:Z/1000, Coord:'ECF', Units:'[km]'}

return, {X:X, Y:Y, Z:Z, Coord:'ECF', Units:'[m]'}

END