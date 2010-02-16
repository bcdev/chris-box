; Transforms Earth Centered Inertial ECI cartesian coordinates to Earth Centered Fixed ECF
; Returns a structure with the transformed coordinates, a tag with the reference system and another with the units (if provided by keyword)

forward_function gst

function eci2ecf, jd, Xeci, Yeci, Zeci, VXeci, VYeci, VZeci, Units=units

; NOTE: No checks are done on the passed parameters!!!
; No matrix operations are used in order to process all values at once (otherwise FORs are needed in IDL)
; It is possible to pass a time vector together with a single 3D positions/velocity which will be transformed for all the times

; Matrix operations:
; [T] = [[ cos(Tg), sin(Tg), 0], $
;        [-sin(Tg), cos(Tg), 0], $
;        [    0,       0     1]]
; Recf = [T]*Reci 


; [TT] = [[-wE*sin(Tg),  wE*cos(Tg), 0], $
;         [-wE*cos(Tg), -wE*sin(Tg), 0], $
;         [     0,           0,      0]]
; Vecf = [T]*Veci + [TT]*Reci

IF N_PARAMS() LT 4 THEN begin
	print, '% Converts ECI coordinates to ECF'
	print, '% Usage: Result = eci2ecf(julday, Xeci, Yeci, Zeci [, VXeci, VYeci, VZeci] [units='<units>'])'
	print, '% Output: A structure containing XYZ and optionally VX, VY and VZ in ECF coordinates'
	RETURN, -1
ENDIF

x = N_ELEMENTS(Xeci)
y = N_ELEMENTS(Yeci)
z = N_ELEMENTS(Zeci)
t = N_ELEMENTS(jd)

IF (x NE y) OR (x NE z) THEN begin
	print, '% ERROR: All coordinate components must have the same size'
	RETURN, -1			; All position coordinates must be the same size
ENDIF
IF (t NE x) AND (x GT 1) AND (t GT 1) THEN begin
	print, '% ERROR: Time and Coordinates must be of the same size'
	print, '%        Unless one of them is of size 1.'
	RETURN, -1		; Time and position ust be the same size unless either of them is sized one y 3D deben ser iguales a menos q una de las dos sea de tama�o unidad
ENDIF

IF NOT keyword_set(units) THEN units='not set'

Tg = Gst(jd, /RAD)		; Rotation angle since Vernal Equinox

Xecf = reform(Xeci * cos(Tg.Tg) + Yeci * sin(Tg.Tg))
Yecf = reform(Xeci * (-sin(Tg.Tg)) + Yeci * cos(Tg.Tg))
Zecf = reform(Zeci)

IF (z EQ 1) AND (t GT 1) THEN Zecf = REPLICATE(Zecf, t)

IF N_PARAMS() EQ 7 THEN begin		; Transform velocities too if they are provided. NOTE: wE (Earth's rotation rate could be taken from a table of constants.)
	VXecf = reform(VXeci * cos(Tg.Tg) + VYeci * sin(Tg.Tg) - Tg.wE * sin(Tg.Tg) * Xeci + Tg.wE * cos(Tg.Tg) * Yeci)
	VYecf = reform(VXeci * (-sin(Tg.Tg)) + VYeci * cos(Tg.Tg) - Tg.wE * cos(Tg.Tg) * Xeci - Tg.wE * sin(Tg.Tg) * Yeci)
	VZecf = reform(VZeci)

	RETURN, {X:Xecf, Y:Yecf, Z:Zecf, VX:VXecf, VY:VYecf, VZ:VZecf, Coord:'ECF', Units:units}
ENDIF

RETURN, {X:Xecf, Y:Yecf, Z:Zecf, Coord:'ECF', Units:units}

END
