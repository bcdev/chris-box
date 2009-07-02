; Transforms geodetic coordinates (spherical) to Earth Centered Fixed ECF cartesian coordinates (rectangular)
; Altitude h starts at the reference geoid and by default must be in meters (if in kilometers /KM keyword must be set)
; By default Latitude and Longitude are operated in degrees, in case they are passed in radians /RAD keyword must be set
;
; NOTE: accuracy might be improved for coordinates close to the XY plane or the Z axis by using alternaive trigonometric formulation in those cases
;

function ECF2GDT, iX, iY, iZ, KM=km_f, RAD=rad_f

;NOTA: Eliminar la singularidad en puntos del eje Z

IF N_PARAMS() NE 3 THEN begin
	print, '% Converts coordinates from ECF to Geocentric'
	print, '% Usage:'
	print, '%    Result = ECF2GEO(X,Y,Z [, /KM])'
	print, '%       set /KM if the units of X,Y,Z are in Km, otherwise X,Y,Z are considered to be in meters [m]'
	print, '%'
	print, '% Output is a structure {LAT, LON, ALT, Coord, Units}'
	RETURN, !values.f_nan
ENDIF

X = double(iX)
Y = double(iY)
Z = double(iZ)

IF KEYWORD_SET(km_f) THEN begin
	km = 1.D
    IF KEYWORD_SET(rad_f) THEN begin
    	ang_conv = 1.D
    	units = '2[rad] [km]'
    ENDIF ELSE begin
    	ang_conv = 180 / !dpi
    	units = '2[Deg] [km]'
    ENDELSE
ENDIF ELSE begin
	X = X / 1000.D
	Y = Y / 1000.D
	Z = Z / 1000.D
	km = 1000.D
    IF KEYWORD_SET(rad_f) THEN begin
    	ang_conv = 1.D
    	units = '2[rad] [m]'
    ENDIF ELSE begin
    	ang_conv = 180 / !dpi
    	units = '2[Deg] [m]'
    ENDELSE
ENDELSE

a = 6378.137D           ; Earth radius at equator of WGS84 ellipsoid (in km)
f = 1/298.257223563D	; WGS84 Earth flattening factor
b = a*(1-f)

b = b*sign(Z)

R = SQRT(X^2+Y^2)
E = (b*Z - (a^2 - b^2))/(a*R)
F = (b*Z + (a^2 - b^2))/(a*R)
P = 4*(E*F + 1)/3
Q = 2*(E^2 - F^2)
D = SQRT(P^3 + Q^2)
nu = (D-Q)^(1/3.D) - (D+Q)^(1/3.D)
G = (SQRT(E^2+nu) + E)/2
t = SQRT(G^2 + (F - nu*G)/(2*G - E)) - G

LATgdt = ATAN(a*(1 - t^2), 2*b*t)
LONgdt = ATAN(Y, X)
ALTgdt = (R - a*t) * COS(LATgdt) + (Z - b) * SIN(LATgdt)

IF X eq 0 AND Y eq 0 AND Z NE 0 THEN begin
    LATgdt = !dpi/2 * sign(Z)
    LONgdt = 0
    ALTgdt = abs(Z - b)
ENDIF

IF Z eq 0 THEN begin
    LATgdt = 0
    ALTgdt = R - a
ENDIF

RETURN, {LAT:LATgdt*ang_conv, LON:LONgdt*ang_conv, ALT:ALTgdt*km, Coord:'GDT', Units:units}

END