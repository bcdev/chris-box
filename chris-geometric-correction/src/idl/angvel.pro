; Given a series satellite positions in ECI and the corresponding times estimates the angular velocity
; Depends on the VECT_ANGLE function

Function AngVel, secs, xeci, yeci, zeci, degrees=deg, plot=pl

IF (N_PARAMS() NE 4) THEN begin
	print, '% Returns the Angular Velocity for a given 3D trajectory'
	print, '%'
	print, '% Usage: Result = AngVel(time, x, y, z [, /DEGREES])'
	print, '%'
	print, '% The result is an array with the angular velocity calculated from consecutive points of the trajectory'
	print, '% The last element is the mean angular velocity (calculated from the first and last position of the trajectory)'
	RETURN, -1
ENDIF

IF NOT KEYWORD_SET(deg) THEN deg=0

A = VECT_ANGLE(shift(xeci,-1), shift(yeci,-1), shift(zeci,-1), xeci, yeci, zeci, degrees=deg)
B = shift(secs,-1) - secs

AV = A / ABS(B)

RETURN, AV

END