function Line_Sphr_i, m, R, P0, R0, COMPLEX=cplx

; NOTE: Cannot process packets of vectors

IF N_PARAMS() LT 2 THEN begin
	print, '% Calculate the intersection points of a Line with an Spheroid'
	print, '% Usage:'
	print, '%   Result = Line_Sphr_i(DV, R [, L0] [, S0])'
	print, '% Inputs:'
	print, '%   DV - Director Vector of the Line [Lx, Ly, Lz].'
	print, '%   R  - Radius of the Spheroid [Rx, Ry, Rz], if a scalar, it gives the SPHERE case.'
	print, '%   L0 - A point of the Line, if empty the line passes through the origin.'
	print, '%   S0 - The center coodinates of the Spheroid, if empty the spheroid is centered at the origin.'
	print, '% Output:'
	print, '%   Array 3x2 with the 3D coordinates of the two insertection points, it returns first the closest to L0'
	print, '%     [[X_near,Y_near,Z_near],[X_far,Y_far,Z_far]]'
	print, '%   If no intersection found, a message is issued and the returned vectors contain NaN values'
	RETURN, -1
ENDIF
m = DOUBLE(m)
R = DOUBLE(R)
IF SIZE(P0, /TYPE) EQ 0 THEN P0 = 0.D
IF SIZE(R0, /TYPE) EQ 0 THEN R0 = 0.D
; NOTA: Falta hacer las comprobaciones sobre las dimensiones y tipos de los parametros

C = [TOTAL((P0-R0)^2 / R^2) -1, 2 * TOTAL(m * (P0-R0) / R^2), TOTAL(m^2 / R^2)]

t = FZ_ROOTS(C, /DOUBLE, EPS = 1D-9)

IF TOTAL(ABS(IMAGINARY(t))) THEN begin
	IF NOT KEYWORD_SET(cplx) THEN begin
		print, '% Line_Sphr_i: Given line and spheroid do NOT intersect'
		RETURN, make_array(3,2, value=!values.d_nan)
	ENDIF ELSE begin
		print, '% Line_Sphr_i: Solution in the COMPLEX domain'
		RETURN, [[m * t[0] + P0],[m * t[1] + P0]]
	ENDELSE
ENDIF

IF NOT KEYWORD_SET(cplx) THEN t = REAL_PART(t)
P1 = m * t[0] + P0
P2 = m * t[1] + P0

Range = min([SQRT(TOTAL((R0-P1)^2)), SQRT(TOTAL((R0-P2)^2))], mn_ndx)		; Finds which result is closer to L0
IF mn_ndx EQ 0 THEN RETURN, [[P1],[P2]] ELSE RETURN, [[P2],[P1]]

END

; Line Equation:
;   X = A�t + X0
;   Y = B�t + Y0
;   Z = C�t + Z0

; Espheroide Equation:
;   (x-x0)^2   (y-y0)^2   (z-z0)^2
;   -------- + -------- + -------- = 1
;     a^2        b^2        c^2

; Intersection:
;  A^2   B^2   B^2           A�(X0-x0)   B�(Y0-y0)   C�(Z0-z0)       (X0-x0)^2   (Y0-y0)^2   (Z0-z0)^2
; (--- + --- + ---)�t^2 + 2�(--------- + --------- + ---------)�t + (--------- + --------- + --------- - 1) = 0
;  a^2   b^2   c^2              a^2         b^2         c^2             a^2         b^2         c^2
