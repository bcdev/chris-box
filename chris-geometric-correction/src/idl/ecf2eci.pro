; Transforms Earth Centered Fixed ECF cartesian coordinates to Earth Centered Inertial ECI
; Returns a structure with the transformed coordinates, a tag with the reference system and another with the units (if provided by keyword)

forward_function gst

function ecf2eci, jd, Xecf, Yecf, Zecf, VXecf, VYecf, VZecf, units=units

; NOTA: No checks are done on the passed parameters!!!
; No matrix operations are used in order to process all values at once (otherwise FORs are needed)
; It is possible to pass a time vector together with a single 3D positions/velocity which will be transformed for all the times

; Matrix operations:
; [T] = [[ cos(Tg), sin(Tg), 0], $
;        [-sin(Tg), cos(Tg), 0], $
;        [    0,       0     1]]
; Reci = Transpose([T])*Recf 


; [TT] = [[-wE*sin(Tg),  wE*cos(Tg), 0], $
;         [-wE*cos(Tg), -wE*sin(Tg), 0], $
;         [     0,           0,      0]]
; Veci = Transpose([T])*Vecf + Transpose([TT])*Recf

IF N_PARAMS() LT 4 THEN begin
	print, '% Converts ECF coordinates to ECI'
	print, '% Usage: Result = ecf2eci(julday, Xecf, Yecf, Zecf [, VXecf, VYecf, VZecf] [units='<units>'])'
	print, '% Output: A structure containing XYZ and optionally VX VY VZ in ECI coordinates'
	RETURN, -1
ENDIF

x = N_ELEMENTS(Xecf)
y = N_ELEMENTS(Yecf)
z = N_ELEMENTS(Zecf)
t = N_ELEMENTS(jd)

IF (x NE y) OR (x NE z) THEN begin
	print, '% ERROR: All coordinate components must have the same size'
	RETURN, -1			; All coords must have the same size
ENDIF
IF (t NE x) AND (x GT 1) AND (t GT 1) THEN begin
	print, '% ERROR: Time and Coordinates must be of the same size'
	print, '%        Unless one of them is of size 1.'
	RETURN, -1		; T and X,Y,Z must have the same size, unless one of them has size 1, which then is propagated by the other.
ENDIF

; Transform scalars into 1-element vectors
IF x EQ 1 THEN Xecf = Xecf[0]
IF y EQ 1 THEN Yecf = Yecf[0]
IF z EQ 1 THEN Zecf = Zecf[0]
IF t EQ 1 THEN jd = jd[0]

IF NOT keyword_set(units) THEN units='not set'

Tg = Gst(jd, /RAD)		; Rotation angle since Vernal Equinox

Xeci = Xecf * cos(Tg.Tg) + Yecf * (-sin(Tg.Tg))
Yeci = Xecf * sin(Tg.Tg) + Yecf * cos(Tg.Tg)
Zeci = Zecf

IF (z EQ 1) AND (t GT 1) THEN Zeci = REPLICATE(Zeci, t)	; In case that transformation is done for a set of times (t GT 1) then the transformed Z coordinate must be replicated to get a vector of the same elements than T

IF N_PARAMS() EQ 7 THEN begin		; Transform velocities too if they are provided. NOTE: wE (Earth's rotation rate could be taken from a table of constants.)
	VXeci = VXecf * cos(Tg.Tg) + VYecf * (-sin(Tg.Tg)) - Tg.wE * sin(Tg.Tg) * Xecf - Tg.wE * cos(Tg.Tg) * Yecf
	VYeci = VXecf * sin(Tg.Tg) + VYecf * cos(Tg.Tg) + Tg.wE * cos(Tg.Tg) * Xecf - Tg.wE * sin(Tg.Tg) * Yecf
	VZeci = VZecf

	RETURN, {X:Xeci, Y:Yeci, Z:Zeci, VX:VXeci, VY:VYeci, VZ:VZeci, Coord:'ECI', Units:units}
ENDIF 

RETURN, {X:Xeci, Y:Yeci, Z:Zeci, Coord:'ECI', Units:units}

END