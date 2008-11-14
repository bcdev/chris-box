; ********************************************************************************************
;
; 		NAME:			chris_pt_aod_lut_img.pro
; 
;		DESCRIPTION:	Obtains AOD at 550 nm from multi-angle PROBA/CHRIS observations
;                       using surface reflectance model of North, TGRS, 1999. This code
;                       uses lookup tables created using MODTRAN by Luis Guanter, 
;                       University of Valencia
;                       				 					  
; 		AUTHOR(S):		William Grey (1), Peter North (1) and Luis Guanter (2), 
;						(1) Swansea University
;                       (2) University of Valencia 	   
; 						    w.m.f.grey@swan.ac.uk 
;
; 		REQUIREMENTS:	
;                       User input card (see below) 
;                       MODTRAN LUTs of Luis Guanter
;
; 		DEVELOPMENT:	First written: 11 May 2007
;                       First Modification: July 2007
;                      		Single-look algorithm for land
;                       	Dark-pixel algorithm for water
;						Second Modification: October 2007
;							Uses MODTRAN LUTs so much quicker now.
;                       Third Modification: November 2007
;                           Reads in merged PROBA/CHRIS images.
;       
; 						$Log: chris_pt_aod_lut_img.pro,v $
; 						Revision 1.4  2008/03/14 15:31:08  ggwgrey
; 						Almost complete.  Inversion for AOD
; 						retrieval is still required.
;
; 						Revision 1.3  2008/03/14 14:01:08  ggwgrey
; 						Now writes out aerosol files, but does
; 						not do inversions yet
;
; 						Revision 1.2  2008/03/13 09:31:28  ggwgrey
; 						Small changes just for testing RCS.
;
; 						Revision 1.1  2008/03/13 09:23:41  ggwgrey
; 						Initial revision
;
;		TO COMPILE:		
;
; 		EXAMPLE: 		chris_pt_rad, '../eg_data/aatsr.dat'
;                       chris_pt_aod_lut_img, 'chris_input_card_v1.dat'
;
;		EXAMPLE INPUT CARD:
;       
;       chris_merged.data [inputfile]      # CHRIS megred image DIMAP directory
;       CHRIS_LUT_formatted_1nm            # Lookup table of atmospheric parameters         
;       372 374                            # Pixels Lines
;       5                                  # Select number of looks
;  		51.0 125.1 19.19 316.00 Nadir      # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 1
;		51.0 125.1 38.69 345.50 Plus35     # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 2
;  		51.0 125.1 36.88 212.23 Minus35    # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 3
;  		51.0 125.1 57.18 357.35 Plus55     # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 4
;  		51.0 125.1 55.71 203.37 Munus55    # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 5
;		4                                  # Number of channels
;  		0.549 0.05 b3   				   # WlMid, WlWidth, band number 1
;		0.671 0.11 b13  				   # WlMid, WlWidth, band number 2
;  		0.868 0.09 b25  				   # WlMid, WlWidth, band number 3
;  		0.987 0.11 b50  				   # WlMid, WlWidth, band number 4
;       1                                  # CHRIS Mode (1,2,3,4 or 5)
;		6, 0.1                             # Aerosol model, angstrom
;		2.5                                # Column water vapour cwv
;  		0.35                               # Ozone 
;  		0.0                                # Elevation
;       9                                  # Winsize
;       10                                 # Skip
;
;       Corresponding angles:
;       Image tag numbers, 0, 1,2,3,4 correspond to nominal FZA
;       given below. Maybe +-35/55 are wrong way round
;       but this is not important here, but needs checking.
;       angle=a0 #plus0
;		angle=a1 #plus35
;		angle=a2 #minus35
;		angle=a3 #plus55
;		angle=a4 #minus55
;
; *******************************************************************************************

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; The following are Luis Guanter's routines
; and are the same as those used for the atmospheric
; correction module, so there is no need to recode
; them into Java.

; ***********************************************************************************
; Luis Guanter, 9 October 2007
; ***********************************************************************************

; ******************************************************************
; Reads the LUT and the corresponding breakpoints and wavelengths
; 
;
; Inputs: avis, asol, phi, hsurf, aot, wv
;  
; ******************************************************************

PRO read_LUT_CHRIS_formatted, file_LUT, vza_arr, sza_arr, phi_arr, hsf_arr, aot_arr, cwv_arr, wvl

common lut_inp, lut1, lut2, num_par, num_bd, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

ndim = 6 ; vza, sza, hsf, aot, phi, cwv

read_var = 1
openr, 1, file_LUT
readu, 1, read_var
num_bd = read_var
wvl = fltarr(num_bd)
readu, 1, wvl ;wavelengths

readu, 1, read_var
dim_vza = read_var
vza_arr = fltarr(dim_vza)
readu, 1, vza_arr ;VZA breakpoints

readu, 1, read_var
dim_sza = read_var
sza_arr = fltarr(dim_sza)
readu, 1, sza_arr ;SZA breakpoints

readu, 1, read_var
dim_hsf = read_var
hsf_arr = fltarr(dim_hsf)
readu, 1, hsf_arr ;Elevation breakpoints

readu, 1, read_var
dim_aot = read_var
aot_arr = fltarr(dim_aot)
readu, 1, aot_arr ;AOT breakpoints

readu, 1, read_var
dim_phi = read_var
phi_arr = fltarr(dim_phi)
readu, 1, phi_arr ;relative azimuth breakpoints

readu, 1, read_var
dim_cwv = read_var
cwv_arr = fltarr(dim_cwv)
readu, 1, cwv_arr ;columnar water vapor break

;print, wvl
;print, vza_arr
;print, sza_arr
;print, hsf_arr
;print, aot_arr
;print, phi_arr
;print, cwv_arr


readu, 1, read_var
npar1 = read_var

readu, 1, read_var
npar2 = read_var

lut1 = fltarr(npar1, num_bd, 1, dim_phi, dim_aot, dim_hsf, dim_sza, dim_vza)
lut2 = fltarr(npar2, num_bd, dim_cwv, 1, dim_aot, dim_hsf, dim_sza, dim_vza)

readu, 1, lut1 ; LUT #1: VZA, SZA, PHI, HSF, AOT
readu, 1, lut2 ; LUT #2: VZA, SZA, HSF, AOT, CWV
close, 1

dim_arr1 = [dim_vza, dim_sza, dim_phi, dim_hsf, dim_aot, 1]
dim_arr2 = [dim_vza, dim_sza, 1, dim_hsf, dim_aot, dim_cwv]

dim_max = max(dim_arr1)
xnodes1 = fltarr(ndim, dim_max)
xnodes1[0, 0:dim_arr1[0]-1] = vza_arr
xnodes1[1, 0:dim_arr1[1]-1] = sza_arr
xnodes1[2, 0:dim_arr1[2]-1] = phi_arr
xnodes1[3, 0:dim_arr1[3]-1] = hsf_arr
xnodes1[4, 0:dim_arr1[4]-1] = aot_arr
xnodes1[5, 0:dim_arr1[5]-1] = 1

dim_max = max(dim_arr2)
xnodes2 = fltarr(ndim, dim_max)
xnodes2[0, 0:dim_arr2[0]-1] = vza_arr
xnodes2[1, 0:dim_arr2[1]-1] = sza_arr
xnodes2[2, 0:dim_arr2[2]-1] = 1
xnodes2[3, 0:dim_arr2[3]-1] = hsf_arr
xnodes2[4, 0:dim_arr2[4]-1] = aot_arr
xnodes2[5, 0:dim_arr2[5]-1] = cwv_arr

num_par = npar1 + npar2 ; lpw, edir, edif, sab, rat

dim_arr = fltarr(ndim)
dim_arr[0:3] = dim_arr1[[0,1,3,4]]
dim_arr[4] = dim_arr1[2]
dim_arr[5] = dim_arr2[5]
max_dim = max(dim_arr)
xnodes = fltarr(ndim, dim_arr1[0]) 
xnodes[0:3, *] = xnodes1[[0,1,3,4], *]
xnodes[4, *] = xnodes1[2, *]
xnodes[5, *] = xnodes2[5, *]
lim = fltarr(2, ndim)
nm_nodes = 2^ndim
lut_cell = fltarr(num_par, nm_nodes, num_bd)

x_cell = fltarr(ndim, nm_nodes)
cont = 0
for i = 0, 1 do $
  for j = 0, 1 do $
    for k = 0, 1 do $
      for ii = 0, 1 do $
        for jj = 0, 1 do $
          for kk = 0, 1  do begin

            x_cell[0, cont] = i
            x_cell[1, cont] = j
            x_cell[2, cont] = k
            x_cell[3, cont] = ii
            x_cell[4, cont] = jj
            x_cell[5, cont] = kk

            cont = cont + 1
          endfor


elip = 0.0001
vza_arr[0] = vza_arr[0] + elip & vza_arr[dim_arr[0] - 1] = vza_arr[dim_arr[0] - 1] - elip
sza_arr[0] = sza_arr[0] + elip & sza_arr[dim_arr[1] - 1] = sza_arr[dim_arr[1] - 1] - elip
hsf_arr[0] = hsf_arr[0] + elip & hsf_arr[dim_arr[2] - 1] = hsf_arr[dim_arr[2] - 1] - elip
aot_arr[0] = aot_arr[0] + elip & aot_arr[dim_arr[3] - 1] = aot_arr[dim_arr[3] - 1] - elip
phi_arr[0] = phi_arr[0] + elip & phi_arr[dim_arr[4] - 1] = phi_arr[dim_arr[4] - 1] - elip
cwv_arr[0] = cwv_arr[0] + elip & cwv_arr[dim_arr[5] - 1] = cwv_arr[dim_arr[5] - 1] - elip

;stop

END 


; ******************************************************************
; Returns an array (5, num_wvl) with the interpolated parameters
;  Order: path_radiance, edir*Tup/mus, edif*Tup, Sab, tdif_up/tdir_up
;
; Inputs: avis, asol, phi, hsurf, aot, wv
;  
; ******************************************************************
FUNCTION interpol_lut, avis, asol, phi, hsurf, aot, wv
common lut_inp, lut1, lut2, num_par, nm_bnd, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

;*** parameters are read
vtest = [avis, asol, hsurf, aot, phi, wv]

; hyper-cell vertices around interpolation point calculated
for i = 0, ndim-1 do begin
  wh = where(vtest[i] lt xnodes[i, *])
  lim[0, i] = wh[0] - 1
  lim[1, i] = wh[0]
endfor

; atmospheric parameters from each vertix are read
cont=0
for i = 0, 1 do $
  for j = 0, 1 do $
    for k = 0, 1 do $
      for ii = 0, 1 do $
        for jj = 0, 1 do $
          for kk = 0, 1 do begin

          lut_cell[0, cont, *] = lut1[0, *, 0, lim[jj,4], lim[ii,3], lim[k,2], lim[j,1], lim[i,0]]
          for ind = 1, num_par - 1 do lut_cell[ind, cont, *] = lut2[ind-1, *, lim[kk,5], 0, lim[ii,3], lim[k,2], lim[j,1], lim[i,0]]
          cont = cont + 1

          endfor 

; input vector is scaled to [0., 1.] for each dimension
for i = 0, ndim - 1 do vtest[i] = (vtest[i] - xnodes[i, lim[0, i]]) / (xnodes[i, lim[1, i]] - xnodes[i, lim[0, i]])


;*** Interpolation
; f(x,y) = V000000*f(x0,y0,z0,xx0,yy0,zz0)+V000001*f(x0,y0,z0,xx0,yy0,zz1)+...+V111111*f(x1,y1,z1,xx1,yy1,zz1)
; with V111111=(x-x0)(y-y0)(z-z0)(xx-xx0)(yy-yy0)(zz-zz0)

f_int = fltarr(num_par, nm_bnd)
for i = 0, nm_nodes - 1 do begin
  weight = abs(product(vtest - x_cell[*, nm_nodes - 1 -i]))
  for ind = 0, num_par - 1 do f_int[ind, *] = f_int[ind, *] + (weight * lut_cell[ind, i, *])
endfor

return, f_int

END

; ******************************************************************
; Returns weights for spectral convolution
;
; Inputs:
;  - wvl_M: 'hyperspectral' wavelengths
;  - wvl: final band positions (C/P)
;  - wl_resol: final spectral resolution (C/P)
;  
; ******************************************************************
FUNCTION generate_filter, wvl_M, wvl, wl_resol

num_wvl_M = n_elements(wvl_M)
num_wvl = n_elements(wvl)

s_norm_M = fltarr(num_wvl_M, num_wvl)
exp_max = 6.
exp_min = 2.
exp_arr = exp_max + (exp_min - exp_max) * findgen(num_wvl) / (num_wvl-1) ; changes Gaussian shape along the spectral range to simulate binning
c_arr = (1./(2.^exp_arr*alog(2.)))^(1./exp_arr)
for bd = 0, num_wvl - 1 do begin
  li1 = where (wvl_M ge (wvl[bd] - 2.* wl_resol[bd]) and wvl_M le (wvl[bd] + 2.* wl_resol[bd]), cnt)
  if (cnt gt 0) then begin
    tmp =abs(wvl[bd] - wvl_M[li1])/(wl_resol[bd] *c_arr[bd])
    s = exp(-(tmp^exp_arr[bd]))
    s_norm_M[li1, bd] = s / total(s)
  endif
endfor

return, s_norm_M

END

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        signfn
;
; AUTHOR;      Peter North, Swansea University
;
; DESCRIPTION: Finds the absoloue of a value based on a corresponding value.
; 
; DATE:        Unknown
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function signfn,a,b

	if (b lt 0.0) then return,(-1.0*abs(a)) $
		else return,abs(a)
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        brent
;
; AUTHOR;      Peter North, Swansea University, rewritten in IDL
;              from "Numerical in Recipes in C".
;
; DESCRIPTION: One-dimensional optimal mininisation routine
; 
; DATE:        Unknown
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function brent,ax,bx,cx,tol,xmin,f1dim
	
	itmax=25
	cgold=.3819660
	zeps=1.0e-6
	a=min([ax,cx])
	b=max([ax,cx])
	v=bx
	w=v
	x=v
	e=0.0
	fx=call_function(f1dim,x)
	fv=fx
	fw=fx
	for iter=1,itmax do begin
		xm=0.5*(a+b)
		tol1=tol*abs(x)+zeps
		tol2=2.0*tol1
		if (abs(x-xm) le (tol2-0.5*(b-a)) ) then goto,three

		if (abs(e) gt tol1) then begin
			r=(x-w)*(fx-fv)
			q=(x-v)*(fx-fw)
			p=(x-v)*q-(x-w)*r
			q=2.0*(q-r);
			if (q gt 0.0) then p=-1.0*p
			q=abs(q);
			etemp=e;
			e=d;
			if ((abs(p) ge abs(0.5*q*etemp)) or (p le q*(a-x)) $
 			or (p ge q*(b-x)) ) then goto,one
			d=p/q;
			u=x+d;
			if ( ((u-a) < tol2) or ((b-u) < tol2) ) then $
				 d=signfn(tol1,xm-x)
			goto,two
		endif
one:
		if (x ge xm) then  e=a-x else e=b-x
		d=cgold*e
two:
		if (abs(d) ge tol1) then u=x+d else u=x+signfn(tol1,d)
		fu=call_function(f1dim,u)

		if (fu le fx) then begin
			if (u ge x) then a=x else b=x
			v=w
			fv=fw
			w=x
			fw=fx
			x=u
			fx=fu
		endif else begin
			if (u lt x) then a=u else b=u
			if ((fu le fw) or (w eq x) ) then begin
				v=w
				fv=fw
				w=u
				fw=fu
			endif else begin
				if ((fu le fv) or (v eq x) or (v eq w) ) then begin
					v=u
					fv=fu
				endif
			endelse
		endelse
	endfor
	print,'Brent exceeds max iterations'
	
;	openw,8,'../brent_max_it'
;	printf,8,'Brent exceeds max iterations'
;	close,8
	
three:
	xmin=x
	return,fx

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        inv_MODTRAN_LUT3
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Interpolate LUT from breakpoint AOTs.
;               	  
; DATE:        March 18th 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro inv_MODTRAN_LUT3, tau
  
  common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 unit_fac = 1.e+4 ; Units conversion, from mW/cm2/sr/um to W/m2/sr/um=mW/m2/sr/nm
 
 ;lut_params equals nlooks * 5 (n atmospheric parameters) * nbands * ntau 
 
 ; Break points of the AOT in the LUT
 x=[0.05, 0.12, 0.20, 0.30, 0.40, 0.60]
 u=[tau]
 atm_param=fltarr(6)
 lut_params2=fltarr(nlooks,5,nbands)
 
 for param=0, 4 do begin
  for band=0, nbands-1 do begin
   for look=0, nlooks-1 do begin
    atm_param=lut_params1(look,param,band,*)
	lut_params2(look,param,band)=interpol(atm_param,x,u)
   endfor
  endfor
 endfor
                                        
 for i=0, nlooks-1 do begin  
  ro_s1 = !pi * ( rad_toa[*,i] / unit_fac - lut_params2(i,0,*) ) / (lut_params2(i,1,*) * cos(sol_zen(i)*!dtor) + lut_params2(i,2,*))
  ro_s = ro_s1 / ( 1.0 +  lut_params2(i,3,*) * ro_s1 )
  RR[*,i] = ro_s   ;# s_norm  
 endfor 
  
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        inv_MODTRAN_LUT2
;
; AUTHOR:      Luis Guanter, University of Valencia
;              Modified by William Grey, Swansea University
;
; DESCRIPTION: This is basically Luis' code for pulling out the 
;			   the atmospheric properties using the LUT reading
;			   and interpolation routines. This code reads in
;              the LUT in one go for efficiency. 
;               	  
; DATE:        March 17th 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro inv_MODTRAN_LUT2, tau

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 unit_fac = 1.e+4 ; Units conversion, from mW/cm2/sr/um to W/m2/sr/um=mW/m2/sr/nm
 
 ;lut_params equals nlooks * 5 (n atmospheric parameters) * nbands * ntau 
 
 tau_index = fix((tau - 0.05) / 0.01)
                                        
 for i=0, nlooks-1 do begin
  
  ro_s1 = !pi * ( rad_toa[*,i] / unit_fac - lut_params(i,0,*,tau_index) ) / (lut_params(i,1,*,tau_index) * cos(sol_zen(i)*!dtor) + lut_params(i,2,*,tau_index))
  ro_s = ro_s1 / ( 1.0 +  lut_params(i,3,*,tau_index) * ro_s1 )
  RR[*,i] = ro_s   ;# s_norm
    
 endfor 
  
end



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        erat_tau
;
; AUTHOR:      Peter North, Swansea University
;              Modifications by William Grey, Swansea University
;              
; DESCRIPTION: The simple ratio method, that does not take into account diffuse light.
;              Useful for debugging purposes as only 1D Brent minimisation is required. 
;              This approach is limited for CHRIS because there is no SWIR channel.
;
; DATE:        Last modified 17th March 2008. 
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function  erat_tau,tau

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1

 ;inv_MODTRAN_LUT, tau ; read in LUT on hoof (very slow)
 ;inv_MODTRAN_LUT3, tau ; read in and interpolate on hoof (quick)
 inv_MODTRAN_LUT2, tau ; Even quicker but quantised
 
 ;print, RR
 ;print,tau
 
 fmin=0
 
 for i=0,nbands-1 do for j=0,nlooks-1 do if (RR(i,j) lt 0.005)$
 then fmin=fmin+(RR(i,j)-0.005)*(RR(i,j)-0.005)*100000.0 
 
 if (fmin le 0.0) then begin
  fmina=RR(3,1)/RR(3,0)-RR(1,1)/RR(1,0)
  ;fmina=RR(2,1)/RR(3,0)-RR(0,1)/RR(0,0)
  fmin=fmina*fmina
 endif
	
 return,fmin

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        Brute force 
;
; AUTHOR:      William Grey, Swansea University
;              
; DESCRIPTION: Use brute force approach for 1D optimisation, 
;              searching through whole LUT.
;
; DATE:        Last modified 17th March 2008. 
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function brute_force, minval, maxval, intval, xmin, f1dim
 
 fxmin=1000.0
 
 for tau=minval, maxval-intval, intval do begin
  
  fx=call_function(f1dim,tau)
  
  if (fx lt fxmin) then begin
   xmin=tau
   fxmin=fx
  endif
  
  ;print, RR
  ;print,tau, xmin, fx, fxmin
   
 endfor
 
 return, fxmin

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        get_lut_subset
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Get LUT subset for CHRIS geometry, across full range of AOTs
;               	  
; DATE:        March 17th 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function get_lut_subset, minaot, maxaot, intaot 
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 ntau = fix((maxaot - minaot) / intaot)
 par_res1=fltarr(nlooks,5,nbands,ntau)
 
 j=0
 for tau=minaot, maxaot-intaot, intaot do begin
  
;  print, tau
  for i=0, nlooks-1 do begin
  
   phi = sol_az(i) - view_az(i) ; relative azimuth angle
   if phi lt 0.0   then phi =  -1.0 * phi
   if phi gt 180.0 then phi = 360.0 - phi
  
;   print, view_zen(i), sol_zen(i), phi, elev, tau, cwv
  
   par = interpol_lut(view_zen(i), sol_zen(i), phi, elev, tau, cwv) 
   par_res = par # s_norm ; convolution to C/P bandsetting
   ;print, par_res
   par_res1(i,*,*,j)=par_res
    
  endfor 
  j=j+1
 
 endfor
 
 return, par_res1
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        get_lut_subset2
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Get LUT subset for CHRIS geometry, across full AOT breakpoints.
;               	  
; DATE:        March 17th 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function get_lut_subset1
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 par_res1=fltarr(nlooks,5,nbands,6)
 tau=[0.05,0.12,0.20,0.30,0.40,0.59999] ; breakpoints for AOT in LUT.
 
 for j=0, 5 do begin
  
;  print, tau
  for i=0, nlooks-1 do begin
  
   phi = sol_az(i) - view_az(i) ; relative azimuth angle
   if phi lt 0.0   then phi =  -1.0 * phi
   if phi gt 180.0 then phi = 360.0 - phi
  
;   print, view_zen(i), sol_zen(i), phi, elev, tau, cwv
  
   par = interpol_lut(view_zen(i), sol_zen(i), phi, elev, tau(j), cwv) 
   par_res = par # s_norm ; convolution to C/P bandsetting
   ;print, par_res
   par_res1(i,*,*,j)=par_res
    
  endfor 
 
 endfor
 
 return, par_res1
 
end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod
;
; AUTHOR;      Peter North, Swansea University
;              Modifications by  William Grey to take in different number of
;              bands and channels
;
; DESCRIPTION: This is the core surface model as described in North et al (TGRS, 1999)
; 
; DATE:        Modifications performed May 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function EMOD, p
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 mval=fltarr(nbands,nlooks)
 WG=fltarr(nbands)
 for i=0,nbands-1 do  WG(i) = 1.0
   
 DF=0.3
 p6=0.35
 tot=0.0

 for i=0,nbands-1 do begin
  
  if (i eq 0) then WG(i) = 1.1 $	             ; special weighting for shortest wavelength
  else if (i eq (nbands-1)) then WG(i) = 0.9 $   ; ... and for longest
  else WG(i) = 1.0
  
  for j=0,nlooks-1 do begin
     		 
    dir=(1.0-DF*DD(i,j))*p(nbands+j)*p(i)
    g=(1.0-p6)*p(i)
    dif=(DF*DD(i,j)+g*(1.0-DF*DD(i,j)))*p6*p(i)/(1.0-g)
    mval(i,j)=(dir+dif)
    k=RR(i,j)-mval(i,j)
    tot=tot+WG(i)*k*k
 	 
  endfor
 endfor

 for j=0,(nlooks-1) do begin
  if (p(nbands+j) lt 0.2) then tot=tot+ (0.2-p(nbands+j))*(0.2-p(nbands+j))*1000.0
  if (p(nbands+j) gt 1.5) then tot=tot+ (1.5-p(nbands+j))*(1.5-p(nbands+j))*1000.0
 endfor
 
 return,tot

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod_tau
;
; AUTHOR;      Peter North, Swansea University
;              Modifications by  William Grey to take in different number of
;              bands and channels
;
; DESCRIPTION: This routine finds the optimal AOD at 550 through calls 
;              to a radiative transfer model and the surface model (emod)
; 
; DATE:        Modifications performed May 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function  EMOD_TAU, tau
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 ;inv_MODTRAN_LUT, tau ; read in LUT on hoof (very slow)
 inv_MODTRAN_LUT3, tau ; read in and interpolate on hoof (quick)
 ;inv_MODTRAN_LUT2, tau ; Even quicker but quantised
 
 fmin=0.0
 
 for i=0,nbands-1 do for j=0,nlooks-1 do if (RR(i,j) lt 0.005)$
 then fmin=fmin+(RR(i,j)-0.005)*(RR(i,j)-0.005)*100000.0 
 
 p=fltarr(nlooks+nbands)
 
 for iBand=0, nbands-1 do p(iBand) = 0.1					
 for iAngle=nbands,nbands+nlooks-1 do p(iAngle) = 0.4				;
				 
; p=[0.1,0.1,0.1,0.1,0.5,0.3]
 
 direction=fltarr(nbands+nlooks,nbands+nlooks)
 
 for i=0,nbands+nlooks-1 do direction(i,i)=1.0
  
 if (fmin le 0.0) then begin
  ftol=0.5e-3
  xi=transpose(direction)
  powell,p,xi,ftol,fmin,'emod' 
 endif
 
 ;print, 'tau: ',tau(0),' fmin: ',fmin

 return,fmin

end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod_tau_ocean
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: "Dark pixel method" for AOD retrieval over oceans.              
; 
; DATE:        May 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function  emod_tau_ocean, tau
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 WG=fltarr(nbands)
 for i=0,nbands-1 do WG(i)=1.0
 for i=0,nbands-1 do if (maxl(i) lt 0.6) then WG(i)=0.5

 rho_surf=fltarr(nbands)
 for i=0,nbands-1 do rho_surf(i)=0.0
 for i=0,nbands-1 do if (maxl(i) lt 0.6) then rho_surf(i)=0.005
 
 inv_MODTRAN_LUT, tau

 fmin1=0.0
 for i=0,nbands-1 do for j=0,nlooks-1 do if (RR(i,j) lt 0.0) then fmin1=fmin1+(RR(i,j)*RR(i,j))*100000.0 
 for i=0,nbands-1 do for j=0,nlooks-1 do fmin1=fmin1+ ((RR(i,j)-rho_surf(i)) * (RR(i,j)-rho_surf(i))) * WG(i)
 
 print, fmin1
 
 return,fmin1

end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod_fvc
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: This basically the same algorithm as that used by Guanter et al 
;              (IJRS, 2007) and von Hoyningen-Huene et al (2005, JGR).  This is 
;              useful in cases where only CHRIS observations from a single-view 
;              direction are available.            
; 
; DATE:        August 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function emod_fvc, p
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 mval=fltarr(nbands)

 tot=0.0

 for i=0,nbands-1 do begin
  
   mval(i) = (p(0) * spec_veg(i)) + (p(1) * spec_soil(i))
   k=RR(i,0)-mval(i)
   tot=tot+k*k
 	  
 endfor
  
 if (p(0) lt 0.0) then tot=tot+(p(0))*(p(0))*1000.0
 if (p(1) lt 0.0) then tot=tot+(p(0))*(p(0))*1000.0
 
 for i=0,nbands-1 do if (mval(i) lt 0.005) then tot=tot+(mval(i)-0.005)*(mval(i)-0.005)*100000.0
 for i=0,nbands-1 do if (mval(i) gt 0.995) then tot=tot+(mval(i)-0.005)*(mval(i)-0.005)*100000.0
 
 ;print, p
 
 return,tot

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod_tau_fvc
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: This basically the same algorithm as that used by Guanter et al 
;              (IJRS, 2007) and von Hoyningen-Huene et al (2005, JGR).  This is 
;              useful in cases where only CHRIS observations from a single-view 
;              direction are available.  Calculates surface reflectance on the basis            
;              of linear mixture modelling of soil and vegetation end spectra.
;
; DATE:        August 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function  emod_tau_fvc, tau
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 inv_MODTRAN_LUT, tau
 
 fmin=0.0
 
 for i=0,nbands-1 do for j=0,nlooks-1 do if (RR(i,j) lt 0.005)$
 then fmin=fmin+(RR(i,j)-0.005)*(RR(i,j)-0.005)*100000.0 
  				 
 p=[fcov,1.0-fcov]
 
 direction=fltarr(2,2)
 
 for i=0,2-1 do direction(i,i)=1.0
  
 if (fmin le 0.0) then begin
  ftol=0.5e-3
  xi=transpose(direction)
  powell,p,xi,ftol,fmin,'emod_fvc' 
  ;fmin=emod_fvc(p)
 endif
 
 print, 'tau: ',tau,' fmin: ',fmin 
 
 return,fmin

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        emod_tau_fvc
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: Identify whether pixels we are looking at are of the ocean or land
;              At 0.6 microns or greater water leaving  radiance is small
;           
; DATE:        August 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function  ocean_query

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1

 red=0.0
 nir=0.0
 
 t=0
 for i=0, nbands-1 do begin
  
  for j=0, nlooks-1 do begin
  
   tarr= (!pi * rad_toa(i,j)) / (cos(!dtor * sol_zen(j)) * spec_sol_irr(i))
   if ((WlMid(i) gt 0.6) and (tarr gt 0.2)) then t=1
  
  endfor
  
 endfor
 
 ocean=0 
 if ((t eq 0)) then ocean=1
 
 return, ocean

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        fcover
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: Calculate initial value of fractional cover
;              so that powell converges quicker. uses solar spectra to 
;              calculate reflectance. Based on Guanter et al (IJRS, 2007).
;           
; DATE:        August 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


function fcover

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1

 red=0.0
 nir=0.0
 flag_red=0
 flag_nir=0
 ndvi0=0.08    ; value from barrax site for soil
 ndviinf=0.98  ; value from barrax site for vegetation
 
 
 for i=0, nbands-1 do begin
  
  if ((WlMid(i) gt 600.0) and (WlMid(i) lt 700.0) and (flag_red eq 0)) then begin
  
   red=(!pi * rad_toa(i,0)) / (cos(!dtor * sol_zen(0)) * spec_sol_irr(i))
   ;red=rad_toa(i,0)
   flag_red=1
   
   ;print, red, rad_toa(i,0), spec_sol_irr(i)
   
  endif
  
  if ((WlMid(i) gt 800.0) and (WlMid(i) lt 880.0) and (flag_nir eq 0)) then begin
  
   nir=(!pi * rad_toa(i,0)) / (cos(!dtor * sol_zen(0)) * spec_sol_irr(i))
   ;nir=rad_toa(i,0)
   flag_nir=1
   
   ;print, nir, rad_toa(i,0), spec_sol_irr(i)
    
  endif 
   
 endfor 

 ndvi=0.0
 
 if ((nir gt 0.0) and (red gt 0.0)) then ndvi=(nir-red)/(nir+red)
 
 fcov = (ndvi - ndvi0) / (ndviinf - ndvi0)
; print, midl(0), midl(1), midl(2), midl(3) , ndvi, fcov 
 
; print, red, nir, ndvi
 
 
 if ((ndvi le 0.0) or (ndvi eq 1.0)) then ndvi=0.5
 
 return, ndvi

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        readinputcard
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: Reads input card from command line.  Input parameters are  
;              given at the start of this file, under example input card.						.		 
;
; DATE:        May 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro readinputcard, infile

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 openr,lun,infile, /get_lun

 nlooks=0
 nbands=0
 aeromodel=0
 cwv=0.0
 angstrom=0.0
 ozone=0.0
 elev=0.0
 mode=1
 pixels=0
 rows=0
 winsize=3
 skip=3
 multilooks=5
 
 readfilename=string(1000)
 
 col1=0.0
 col2=0.0
 col3=0.0
 col4=0.0
 col5=string(10)
 
 readf, lun, readfilename
 chrisimg = readfilename
 readf, lun,  readfilename
 file_LUT = readfilename
  
 readf, lun, pixels, rows
 readf, lun, nlooks
 
 sol_zen=fltarr(nlooks)
 view_zen=fltarr(nlooks)
 sol_az=fltarr(nlooks)
 view_az=fltarr(nlooks)
 view_angle=strarr(nlooks)
  
; print, multilooks
 
 for i=0, nlooks-1 do begin
 
  readf, lun, col1, col2, col3, col4, col5
  
  ; Luis' LUT does not like zero values.
  ; Therefore change to give non zero values. 
  ; This is a bit messy, but okay for 
  ; the time being.
  
  ; if col1 eq 0.0 then col1 = 1.e-2
  ; if col2 eq 0.0 then col2 = 1.e-2
  ; if col3 eq 0.0 then col3 = 1.e-2
  ; if col4 eq 0.0 then col4 = 1.e-2
 
  sol_zen(i)  = col1
  sol_az(i)   = col2
  view_zen(i) = col3
  view_az(i)  = col4
  view_angle(i) = col5
    
  ;print, sol_zen(i), sol_az(i), view_zen(i), view_az(i)
 
 endfor
 
 readf,lun, nbands
 ;print, nbands

 WlMid=fltarr(nbands)
 WlWidth=fltarr(nbands)
 channel=strarr(nbands)
 
 for i=0, nbands-1 do begin
 
  readf,lun, col1, col2, col5
  WlMid(i)=col1
  WlWidth(i)=col2
  channel(i)=col5
   
 endfor
  
 readf,lun,mode
 readf,lun,aeromodel, angstrom
 readf,lun,cwv   ; cwv = 2.2
 readf,lun,ozone
 readf,lun,elev
 readf,lun,winsize
 readf,lun,skip
 
 free_lun, lun 
 close,lun
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        readspec
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: Read End spectra files for soil and vegetation 
;               	  
; DATE:        August 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro read_spec

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1

  openr,lun1,'dat/spec_soil.dat', /get_lun
  openr,lun2,'dat/spec_veg.dat', /get_lun
  
  full_spec_soil=fltarr(70)
  full_spec_veg=fltarr(70)
  full_WlMid=fltarr(70)
  
  nspec=70
  
  for i=0, nspec-1 do begin
  
   readf,lun1, col1, col2
   full_WlMid(i)=col1*1000.0 ; convert from mocrons to nm
   full_spec_soil(i)=col2  
   
   readf,lun2, col1, col2
   full_spec_veg(i)=col2
    
  endfor
  
  close,lun1,lun2
  free_lun, lun1, lun2
  
  for i=0, nbands-1 do begin
   mindiff=100.0
   for j=0, nspec-1 do begin
    
	diff=abs(WlMid(i) - full_WlMid(j))
	
	if  (diff lt mindiff) then begin
	
	 spec_veg(i)=full_spec_veg(j)
	 spec_soil(i)=full_spec_soil(j)
	 mindiff=diff
	 
    endif
  
   endfor
  
  endfor
  
; print, full_spec_veg
; print, ' '
; print, full_spec_soil
; print, ' ' 
; print, full_WlMid
; print, ' '
; print, WlMid
; print, ' '
; print, spec_veg
; print, ' ' 
; print, spec_soil

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        read_sol_irr
;
; AUTHOR;      William Grey, Swansea University
;
; DESCRIPTION: Read Spectral solar irradiance
;               	  
; DATE:        October 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro read_sol_irr

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1

  openr,lun1,'dat/6s_solirr.dat', /get_lun

  full_spec_sol=fltarr(1501)
  full_WlMid=fltarr(1501)
  
  nspec=1501
  
  for i=0, nspec-1 do begin
  
   readf,lun1, col1, col2
   full_WlMid(i)=col1
   full_spec_sol(i)=col2  
       
  endfor
  
  close,lun1
  free_lun, lun1
  
  for i=0, nbands-1 do begin
   mindiff=100.0
   for j=0, nspec-1 do begin
    
	diff=abs(WlMid(i) - full_WlMid(j))
	
	if  (diff lt mindiff) then begin
	
	 spec_sol_irr(i)=full_spec_sol(j)
	 mindiff=diff
	 
    endif
  
   endfor
  
  endfor
 
; print, WlMid
; print, spec_sol_irr
   
end  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        inv_MODTRAN_LUT
;
; AUTHOR;      Luis Guanter, University of Valencia
;              Modified by William Grey, Swansea University
;
; DESCRIPTION: This is basically Luis' code for pulling out the 
;			   the atmospheric properties using the LUT reading
;			   and interpolation routines. Modified for calculating
;			   surface reflectance and diffuse fraction. 
;               	  
; DATE:        October 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro inv_MODTRAN_LUT, tau

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 unit_fac = 1.e+4 ; Units conversion, from mW/cm2/sr/um to W/m2/sr/um=mW/m2/sr/nm
 
 for i=0, nlooks-1 do begin
  
  phi = sol_az(i) - view_az(i) ; relative azimuth angle
  if phi lt 0.0   then phi =  -1.0 * phi
  if phi gt 180.0 then phi = 360.0 - phi
  
;  print, view_zen(i), sol_zen(i), phi, elev, tau, cwv
  
  par = interpol_lut(view_zen(i), sol_zen(i), phi, elev, tau, cwv) 

; par[0, *] --> path_radiance
; par[1, *] --> Edir(ground)*T_up/cos(SZA)
; par[2, *] --> Edif(ground)*T_up
; par[3, *] --> Sph. Albedo
; par[4, *] --> tdif_up / tdir_up
  
  par_res = par # s_norm ; convolution to C/P bandsetting
 
 
  fdiff = par[2, *] / (par[2, *] + (par[1, *] * cos(sol_zen(i))))
  DD[*,i] = fdiff # s_norm
  
  ; Example, derivation of TOA radiance for a constant albedo of 0.5
  ;par = interpol_lut(vza, sza, phi, hsf, aot, cwv)
  ;ro_s = 0.5
  ;toa = par[0, *] + (par[1, *] * cos(sza*!dtor) + par[2, *]) * ro_s / !pi / (1. - par[3, *] * ro_s)
  ;toa_res = toa # s_norm
  
  ; Eqs below from the 6S manual.  Allows us to calculate 
  ; surface reflectance from TOA reflectance, by rearranging
  ; the above equation. 
  

   ro_s1 = !pi * ( rad_toa[*,i] / unit_fac - par_res[0, *] ) / (par_res[1, *] * cos(sol_zen(i)*!dtor) + par_res[2, *])
   ro_s = ro_s1 / ( 1.0 +  par_res[3, *] * ro_s1 )
   RR[*,i] = ro_s   ;# s_norm
   ;print, RR[*,i] 
   ;print, rad_toa[*,i]
   
 endfor 

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        PrintVerbose
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Routione for printig out lots of stuff. 
;               	  
; DATE:        October 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro PrintVerbose

 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
; print, rad_toa  
 print, ''
 print, 'Input parameters:'
 print, nlooks,                  format='("Number of views      = ",i0)'
 print, strcompress(view_angle), format='("View angles          = ",5(A,4X))'
 print, sol_zen,                 format='("Solar zenith angles  = ",5(F7.3,4X))'
 print, view_zen,                format='("View zenith angles   = ",5(F7.3,4X))'
 print, sol_az,                  format='("Solar azimuth angles = ",5(F7.3,4X))'
 print, view_az,                 format='("View azimuth angles  = ",5(F7.3,4X))'
 print, nbands,                  format='("Number of bands      = ",i0)'
 print, strcompress(channel),    format='("Band numbers         = ",5(A5,4X))'
 print, WlMid,                   format='("Wavelengths          = ",5(F7.2,4X))'  
 print, WlWidth,                 format='("Band widths          = ",5(F7.2,4X))'  
 print, pixels,                  format='("Pixels               = ",i0)' 
 print, rows,                    format='("Rows                 = ",i0)'
 print, skip,                    format='("Pixel skip           = ",i0)'
 print, winsize,                 format='("Window size          = ",i0)'
 print, strcompress(chrisimg),   format='("CHRIS image          = ",A)'
 print, strcompress(file_LUT),   format='("Lookup table         = ",A)'
 print,  mode,                   format='("CHRIS image mode     = ",i0)'
 print,  elev,                   format='("Elevation            = ",F7.3)'
 print,  ozone,                  format='("Ozone (cm-atm)       = ",F7.3)'
 print,  cwv,                    format='("water vapour(g/cm2)  = ",F7.3)'
 print,  aeromodel,              format='("Aerosol model        = ",i0)'
 print,  angstrom,               format='("Angstrom exponent    = ",F7.3)'
 
end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        interpol_reg_2d_grid
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Performing block interpolation between sparsely points. 
;               	  
; DATE:        Unknown
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function interpol_reg_2d_grid, grid, xsize, ysize, edge, skip
  
 ; Interpolating regualr grid 
  
 xpixs = fix(xsize/skip)
 ypixs = fix(ysize/skip)
 grid_interpolate=fltarr(xpixs,ypixs)
 grid_out = fltarr(xsize,ysize)
  
 for x=edge,xpixs*skip,skip do $
  for y=edge,ypixs*skip,skip do $
   grid_interpolate(fix(x/skip),fix(y/skip))=grid(x,y) 
 
 for x=0,xpixs*skip-1 do $
  for y=0,ypixs*skip-1 do $
   grid_out(x,y)=grid_interpolate(fix(x/skip),fix(y/skip))
      
  return, grid_out 
     
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        select_angles
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Find out for which angles data are available and where they are not. 
;               	  
; DATE:       6 November
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function select_angles, rad_toa_img, pixels, yout, nbands, multilooks
 
 nangles=bytarr(pixels,yout)
 
 for x=0,pixels-1 do begin
  for y=0,yout-1 do begin
   for i=0,multilooks-1 do begin
 	
	bitflag = 2^i ; 1, 2, 4, 8, 16 	
	if (n_elements(where(rad_toa_img(x,y,*,i))) eq nbands) then nangles(x,y) = nangles(x,y) or bitflag
	 	 		
   endfor 	
  endfor
 endfor
 
 return, nangles
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        tiltang
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: calculate the tilt angle for sun glint threshild with is
;              considered as:
;
;              - Glint-free region (tilt angle > 23 deg).
;              - tilt angles between 15 to 23 deg).
;              - Glint region (tilt angles < 15 deg)
;              These numbers are for ATSR-2 from Zavody et al. 
;              (2000 J. Atmos. and Ocean. Tech.).
; 
; INPUT:       sza - solar zenith angle in degrees
;              vza - view zenith angle in degrees
;              phi - relative azimuth angle in degrees
; 
; OUTPUT:      tilt - tiltangle  in degrees   
;                        	  
; DATE:       21 November 2007
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

function tiltang, sza, vza, phi

 ; zx and zy are the slope 
 ; vectors (see Cox and Munk, 1954)
 
 zx = -1.0 * sin(!dtor*phi) / (cos(!dtor*sza) + cos(!dtor*vza))
 zy = (sin(!dtor*sza) + sin(!dtor*vza) * cos(!dtor*phi)) / (cos(!dtor*sza) + cos(!dtor*vza))
 
 tantilt=sqrt(zx*zx+zy*zy)
 tilt=atan(tantilt)
	
 return, !radeg*tilt 

end


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        calc_aod_img_stats
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Calculates basic statistics across AOD output images
;               	  
; DATE:        Last modified 13 March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro calc_aod_img_stats, aopt, chrisimg
 
 statfile=strcompress(chrisimg + '/aot_stats.dat', /remove_all)
 openw, lun, statfile, /get_lun 
 
 for i=0,2 do begin
 
  aopt_img=aopt(*,*,i)
  aot_stats=moment(aopt_img(where (aopt_img ge 0.01, count)))
  min_aot=min(aopt_img(where (aopt_img ge 0.01, count)))
  max_aot=max(aopt_img(where (aopt_img ge 0.01, count)))
  
  print, ' '
  if i eq 0 then print, format='("Summary of statistics for AOD at 550 nm")'
  if i eq 1 then print, format='("Summary of statistics for AOD at 440 nm")'
  if i eq 2 then print, format='("Summary of statistics for AOD at 670 nm")'
 
  print, format='("***************************************")'
  print, count, 			  format='("Number of samples  = ",I0)'
  print, aot_stats(0),  	  format='("Mean               = ",F7.3)'
  print, sqrt(aot_stats(1)),  format='("Standard deviation = ",F7.3)'
  print, min_aot,             format='("Minimum            = ",F7.3)'
  print, max_aot,             format='("Maximum            = ",F7.3)'
  
  printf, lun, ' '
  if i eq 0 then printf, lun, format='("Summary of statistics for AOD at 550 nm")'
  if i eq 1 then printf, lun, format='("Summary of statistics for AOD at 440 nm")'
  if i eq 2 then printf, lun, format='("Summary of statistics for AOD at 670 nm")'
 
  printf, lun, format='("***************************************")'
  printf, lun, count,              format='("Number of samples  = ",I0)'
  printf, lun, aot_stats(0),       format='("Mean               = ",F7.3)'
  printf, lun, sqrt(aot_stats(1)), format='("Standard deviation = ",F7.3)'
  printf, lun, min_aot,            format='("Minimum            = ",F7.3)'
  printf, lun, max_aot,            format='("Maximum            = ",F7.3)'
  
 endfor
 
 print, ' '
 
 close, lun
 free_lun, lun

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        create_envi_header
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Creates ENVI header
;               	  
; DATE:        Last modified: 14th March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


pro create_envi_header, infile, samples, lines, bands, datatype

 openw,u,infile, /get_lun
 printf,u,'ENVI'
 printf,u, samples, format='("samples = ",i)'
 printf,u, lines, format='("lines = ",i)'  
 printf,u, bands, format='("bands = ",i)'  
 printf,u,datatype, format='("data type = ",i)'
 printf,u,'byte order = 0'
 close,u
 free_lun,u
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        printusage
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Prints usage information with an 
;              example input card.
;               	  
; DATE:        31st March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro printusage

 print, '   Usage: chris_pt_aod_lut_img, chris_input_card'
 print, ' '
 print, '   EXAMPLE INPUT CARD:'
 print, '   chris_merged.data [inputfile]   # CHRIS megred image DIMAP directory'
 print, '   CHRIS_LUT_formatted_1nm         # Lookup table of atmospheric parameters' 
 print, '   372 374                         # Pixels Lines'
 print, '   5                               # Select number of looks'
 print, '   51.0 125.1 19.19 316.00 Nadir   # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 1'
 print, '   51.0 125.1 38.69 345.50 Plus35  # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 2'
 print, '   51.0 125.1 36.88 212.23 Minus35 # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 3'
 print, '   51.0 125.1 57.18 357.35 Plus55  # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 4'
 print, '   51.0 125.1 55.71 203.37 Munus55 # SOLZN, SOLAZ, VIEWZN, VIEWAZ,  view 5'
 print, '   4                               # Number of channels'
 print, '   0.549 0.05 b3                   # WlMid, WlWidth, band number 1'
 print, '   0.671 0.11 b13                  # WlMid, WlWidth, band number 2'
 print, '   0.868 0.09 b25                  # WlMid, WlWidth, band number 3'
 print, '   0.987 0.11 b50                  # WlMid, WlWidth, band number 4'
 print, '   1                               # CHRIS Mode (1,2,3,4 or 5)'
 print, '   6  0.1                          # Aerosol model, angstrom'
 print, '   2.5                             # Column water vapour cwv'
 print, '   0.35                            # Ozone' 
 print, '   0.0                             # Elevation'
 print, '   9                               # Winsize'
 print, '   10                              # Skip'
 print, ' '

end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        write_output_aot_imgs 
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Writes output images of AOT, create 
;              corresponding ENVI header files,
;              closes files and tidies up.
;               	  
; DATE:        18th March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


pro write_output_aot_imgs, outfilename, img
  
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 edge=winsize/2
 openw, lun, strcompress(chrisimg + '/' + outfilename + '.img', /remove_all),    /get_lun 
 img_interpol=interpol_reg_2d_grid(img,pixels,rows,edge,skip)
 writeu, lun, img_interpol 
 create_envi_header, strcompress(chrisimg + '/' + outfilename + '.hdr', /remove_all), pixels, rows, 1, 4
 
 close, lun
 free_lun, lun
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        write_output_var_imgs 
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Writes output images of reflectance
;              variance in window.
;               	  
; DATE:        18th March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


pro write_output_var_imgs, outfilename, img
  
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 edge=winsize/2
 openw, lun, strcompress(chrisimg + '/' + outfilename + '.img', /remove_all),    /get_lun 
 
 for i=0, (nbands*nlooks)-1 do begin
  img1=img(*,*,i)
  img_interpol=interpol_reg_2d_grid(img1,pixels,rows,edge,skip)
  writeu, lun, img_interpol
 endfor
 
 create_envi_header, strcompress(chrisimg + '/' + outfilename + '.hdr', /remove_all), pixels, rows, nbands*nlooks, 4
 
 close, lun
 free_lun, lun
 
end

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; NAME:        chris_pt_aod_lut
;
; AUTHOR:      William Grey, Swansea University
;
; DESCRIPTION: Main routine. Mainly controls reading and writing file I/0.
;               	  
; DATE:        Last modified 31 March 2008
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

pro chris_pt_aod_lut_img_v2, infile
 
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ; Unforgivable I know for using such
 ; nasty constructs as common blocks!
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 common info, DD, rad_toa, RR, sol_zen, view_zen, sol_az, view_az, WlMid, WlWidth, spec_sol_irr,$
	    mode, elev, aeromodel, ozone, cwv, nlooks, nbands, fcov, spec_veg, spec_soil, s_norm, angstrom,$
        pixels, rows, winsize, skip, chrisimg, file_LUT, channel, view_angle, lut_params, lut_params1
 
 ; Do not report exceptions. 
 ; (e.g. Floating-point underflow errors) 
 ; Supresses error messages!
 
 !EXCEPT=0 
 
 ;;;;;;;;;;;;;;;
 ; Start timer
 ;;;;;;;;;;;;;;;
 
 tstart=systime(1) 
 
 ; Print usage message and exit, if the
 ; number of command line arguments is not 1.
 
 if (n_params() ne 1 ) then begin
  printusage
  return
 endif 
 
 ; Test if input card exists.
 ; If not exit.
 
 if (FILE_TEST(infile,/REGULAR) eq 0) then begin
  print, 'Input card ' + infile + ' does not exist. Choose a different filename.'
  return
 endif
 
 
 ;;;;;;;;;;;;;;;;;;;;;;;;;;
 ; Read in input card from
 ; the command line.
 ;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 readinputcard, infile
 PrintVerbose

 edge=winsize/2
 box=winsize*winsize
 
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ; Set up Arrays. 
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 RR=fltarr(nbands,nlooks)           ; Surface Reflectance
 DD=fltarr(nbands,nlooks)           ; Diffuse fraction
 rad_toa=fltarr(nbands,nlooks)      ; CHRIS TOA radiances
 spec_veg=fltarr(nbands)            ; Vegetation spectra
 spec_soil=fltarr(nbands)           ; Soil spectra
 spec_sol_irr=fltarr(nbands)        ; Solar irradiance spectra
  
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ; Read in vaious ancillary files
 ; into memory (LUT, surface spectra, solar irradiance)  
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 print, ' ' 
 print, 'Reading in Lookup table of atmospheric parameters ....'
 print, file_LUT

 ;file_LUT=/geog/home/research/ggwgrey/links/laplaceb/CHRIS_LUT/CHRIS_LUT_formatted_1nm
 
 if (FILE_TEST(file_LUT,/REGULAR) eq 0) then begin
  print, 'LUT ' + file_LUT + ' does not exist.'
  return
 endif
 
 read_LUT_CHRIS_formatted, file_LUT, vza_gr, sza_gr, phi_gr, hsf_gr, aot_gr, cwv_gr, wvl_LUT 
 s_norm = generate_filter(wvl_LUT, WlMid, WlWidth)  ; Width is assumed to be FWHM  
 
 ;read_spec
 ;read_sol_irr
 
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ; Create arrays for reading input and output
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; 
 
 ; Input CHRIS radiances [input file], read in whole image.  
 ; Maybe costly memory-wise but much simpler to code up,
 ; than breaking up into blocks. The maximum image size
 ; in memory will be equal to: 
 ; bands * pixels * lines * float * looks
 ; 62 * 400 * 400 * 4 * 5 = 200 Mb
 ; Quite large but we use only a few of the bands. 
 ; A tessaract is a 4D hypercube. 
 
 rad_toa_tessaract=fltarr(pixels,rows,nbands,nlooks) 
 rad_toa_img=fltarr(pixels,rows)

 ; AOD image. b1=550 nm and extrapolated to 440 nm (b2) and (670 nm)
 ; based on Angstrom exponent [output file]
 
 aopt=fltarr(pixels,rows,3)
 aopterr=fltarr(pixels,rows) ; Corresponding best fit error image [output file]
 varimg=fltarr(pixels,rows,nbands*nlooks)
 
 print, ' ' 
 print, 'Reading images into memory....'
 for i=0, nlooks-1 do begin
  for j=0, nbands-1 do begin
   radimg=strcompress(chrisimg + '/radiance_' + channel(j) + view_angle(i) +'.img', /remove_all)
   
   if (FILE_TEST(radimg,/REGULAR) eq 0) then begin
    print, 'Image ' + radimg + ' does not exist.'
    return
   endif
   
   print, radimg
   openr, lun, radimg, /get_lun
   readu, lun, rad_toa_img
   close, lun
   free_lun, lun
   rad_toa_tessaract(*,*,j,i) = rad_toa_img
  endfor
 endfor
  
 ;openw, lun, strcompress(chrisimg +'/cheese.img', /remove_all), /get_lun
 ;writeu, lun, rad_toa_tessaract 
 ;close, lun
 ;free_lun, lun 
 
 
 print, ''
 print, 'Reading in subset of LUT in one go for efficiency'
 
 ;lut_params equals nlooks * 5 (n atmospheric parameters) * nbands * ntau
 lut_params=get_lut_subset(0.05, 0.60, 0.01)
 ;tau_index = fix((0.5 - 0.05) / 0.01)
 ;print, lut_params(*,*,*,tau_index)
 
 lut_params1=get_lut_subset1()
 ;lut_params2=interpol_lut3(lut_params1, 0.5, nbands, nlooks)
 ;print, lut_params2
 
 
 ;openw,u,'cheese', /get_lun
 ;printf, u, lut_params
 ;close, u
 ;free_lun, u
 
 ;help, lut_params
 ;print, lut_params
 
  
 print, ' '
 print, 'Performing model inversion....'
    
 for x=edge,pixels-edge-1,skip do begin
    
  print, format='($, %"\b\b\b\b%d%%")',(100.0 * x / pixels)
   	  
   for y=edge,rows-edge-1,skip do begin

    
   flag=1	 

   for m=0, nbands-1 do begin
	for n=0, nlooks-1 do begin
	 
	 rad_toa(m,n)=total(rad_toa_tessaract(x-edge:x+edge,y-edge:y+edge, m, n)) / box
	   
	 ; If there is large standard deviation 
	 ; within window we assume heterogeneity
	 ; or over a surface boundary and we cannot 
	 ; reliably retreive AOD using this approach.
	 ; Also do not retrieve if we have any 
	 ; cloud within the window.
	 
	 ; This identifes dark water pixels 
	 ; where the algorithm does not work.
	 ; so we ignore these pixels.
	 
	 ;print, rad_toa
	 
	 nwaterpixels=n_elements(where(rad_toa le 25.0, count))
	 ;print, count
	   
	 stats = moment(rad_toa_tessaract(x-edge:x+edge,y-edge:y+edge,m,n))
	 varimg(x,y,m*nlooks+n)=stats[1]
	 nelementsinbox=n_elements(where(rad_toa_tessaract(x-edge:x+edge,y-edge:y+edge,m, n)))
	   
	 if ( ( nelementsinbox ne box ) or ( stats[1] ge 100.0 ) or ( count ne 0 ) ) then flag = 0
     
	endfor	 
   endfor
   
   if flag eq 1 then begin
	 	 	 	 
    ; find fractional coverage
    ; useful for initialising single-look
	; AOD algorithm
	 
    ; fcov=fcover()
	 
	; Identify whether pixels correspond
	; to land or water
	 
    ; ocean=ocean_query()
	 
	; Initialise parameters
	    
	 mval=0.0  ; aopterr
     xi=[.1]
     tol=0.01  ; 0.01
     p=0.1     ; aopt
	 
	 ;if ((x eq edge) and (y eq edge)) then 
	 
	 ;mval=brute_force(0.05,0.60,0.01,p,'erat_tau')
	 ;mval=brute_force(0.05,0.60,0.01,p,'emod_tau')
	 ;mval=brent(0.05,0.10,0.6,tol,p,'erat_tau')
	 mval=brent(0.05, 0.10, 0.6, tol, p, 'emod_tau')
	 
	 ; call AOD inversion
	 
;	 if (ocean  eq 1) then mval=brent(0.05,0.10,0.6,tol,p,'emod_tau_ocean')
      
;	 if ((ocean eq 0) and (nlooks gt 1)) then mval=brent(0.05, 0.10, 0.6, tol, p,'emod_tau')
;    if ((ocean eq 0) and (nlooks eq 1)) then mval=brent(0.05, 0.10, 0.6, tol, p,'emod_tau_fvc') 
;	 if (nlooks eq 0) then p=0.0
	  
	; sometimes we get negative reflectances where surface is dark.
	; even though the algorithm penalises instances where we get 
	; calculated negative reflectances during iteration.
	; We do not want to include the corresponding retrieved 
	; AOD in the final output.
	
	inv_MODTRAN_LUT, p
	n=n_elements(where(RR ge 0.0))
	
	; Interpolate AOD at 440 and 670 nm based
    ; on the aerosol models angstrom exponent value
	
    
	if ((n eq (nbands * nlooks)) and (mval lt 0.001)) then begin	
	 b=p/(550^angstrom)
     aopt(x, y, 0)= p 
     aopt(x, y, 1)= b * ( 440 ^ angstrom)
     aopt(x, y, 2)= b * ( 670 ^ angstrom)
	 aopterr(x, y)= mval
    endif
	 
   endif	 
  endfor   
 endfor
 
 print, format='($, %"\b\b\b\b%d%%")',100.0
 print, '' 
 print, ''
 print, 'Calculating image statistics ....'
 calc_aod_img_stats, aopt, chrisimg
 
 
 print, 'Writing output files ....'

 write_output_aot_imgs, 'aot550' , aopt(*,*,0)
 write_output_aot_imgs, 'aot440' , aopt(*,*,1) 
 write_output_aot_imgs, 'aot670' , aopt(*,*,2)
 write_output_aot_imgs, 'aot550_err' ,aopterr
 ;write_output_var_imgs, 'ref_var', varimg
  
 print, ''
 print, 'Runtime: ', (systime(1)-tstart), ' seconds ',  (systime(1)-tstart)/60, ' minutes'
 
end


pro chris_pt_aod_run

  chris_pt_aod_lut_img_v2, '/geog/data/laplace/beta/ggwgrey/CHRIS/input_cards/chris_input_card_Solar_Village_030912.dat'

end
