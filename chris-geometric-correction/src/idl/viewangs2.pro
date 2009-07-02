; Calcula los angulos de vision desde el sistema de coords LHLV
; A partir de Dr. T.S. Kelso (Celestrak)

function ViewAngs2, TgtX, TgtY, TgtZ, SatX, SatY, SatZ, TgtLat

IF N_PARAMS() NE 6 THEN begin
	print, '% Calculate the View Angles in the LHLV coordinate system (Local Horizon, Local Vertical)'
	print, '% usage: Result = ViewAngs(TgtX, TgtY, TgtZ, SatX, SatY, SatZ)'
	print, '%'
	print, '% Returns the structure {AZI, ZEN, RANGE, Units}'
	RETURN, -1
ENDIF

; REFERENCE: T.S.Kelso, Trakstar Code
; NOTA: La vertical local se obtiene de la latitud geodetica calculada a partir de la posicion TGT

RangX = SatX - TgtX
RangY = SatY - TgtY
RangZ = SatZ - TgtZ
Rango = SQRT(RangX^2+RangY^2+RangZ^2)
gdt = ecf2gdt(TgtX, TgtY, TgtZ, /km)         ; Calcula la Latitud geodetica del Tgt (punto donde se define la vertical Local)
sin_lat = SIN(gdt.Lat * !dpi / 180)
cos_lat = COS(gdt.Lat * !dpi / 180)
sin_theta = TgtY / SQRT(TgtX^2+TgtY^2)
cos_theta = TgtX / SQRT(TgtX^2+TgtY^2)

top_s = sin_lat * cos_theta * RangX $
	+ sin_lat * sin_theta * RangY $
	- cos_lat * RangZ
top_e = -sin_theta * RangX $
	+ cos_theta * RangY
top_z = cos_lat * cos_theta * RangX $
	+ cos_lat * sin_theta * RangY $
	+ sin_lat * RangZ

AZI = ATAN(-top_e/top_s)
AZI = AZI + (top_s GT 0)*!dpi + (top_s LT 0)*2*!dpi
ZEN = !dpi/2 - ASIN(top_z/Rango)

RETURN, {AZI:(AZI MOD (2*!DPI)), ZEN:ZEN, RangX:RangX, RangY:RangY, RangZ:RangZ, RANGE:Rango, Coord:'LHLV', Units:'2[rad] 4[m]'}

END
