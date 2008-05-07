;********************************************************************************************
;********************************************************************************************
;  CHRIS_AtmCor_LUT_beam_noDEM.pro
;
;  L. Guanter, version Feb/2008
;
;  Processes C/P images from Beam/noise_reduction output
;
;   Modes 1&5: smile per column along AC 
;   Modes 2,3%4: no smile, WV and SpecPol
;
;   Option of topographic/elevation correction with DEM disabled (GC non-available)
;
;********************************************************************************************
;********************************************************************************************

;********************************************************************************************
; DRIVER:
;
; 1. Loads series of images and reads metadata
; 2. Loads LUT
; 3. Generates s_norm_ini with wvl filters according to nominal wl_center 
;   (to be applied x_flt = x # s_norm_ini)
; 5. Runs AOT_retr
; 6. Launches CHRIS_AC_modeXXX
; 7. Writes output images
;********************************************************************************************

PRO CHRIS_AtmCor_LUT_beam_noDEM

common lut_inp, lut1, lut2, num_par, num_wvl_LUT, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

forward_function envi_get_data ; to be removed
forward_function zbrent
forward_function interpol_lut
forward_function filext
forward_function generate_filter

envi, /restore_base_save_files ; to be removed
envi_batch_init; to be removed

;********************************************************************************************
; INPUTS
path_act = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_Toolbox/' ;working directory
file_LUT = path_act + 'CHRIS_LUTs/CHRIS_LUT_formatted_1nm' ;LUT file
EM_file = path_act + 'endmembers_ground.prn' ;ancillary file
file_Isc = path_act + 'newkur_EnMAP.dat' ;ancillary file

img_IND = 3 ; data set to be processed from those in the list below

;++++++ USER inputs ++++++
SpecPol_flg = 1 ; =0: no spectral polishig, =1: spectral polishing based on EM, =2: spectral polishing based on low-pass filter (only if mode =1,5)
ady_flg = 1 ; =1, adjacency correction
aot550_val = 0 ; user-selected aot value; disables aot retrieval
wv_val = 0 ; user-selected wv value, to be used as a guess in wv_retrieval (mode 1,5)
cld_aot_thre = 0.02 ; probability threshold for AOT retrieval
cld_rfl_thre = 0.05 ; probability threshold for REFL (& CWV) retrieval
;+++++++++++++++++++++++++

;********************************************************************************************
case img_IND of

  1: begin ;*** Beijing1 ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_Yuchu/CHRIS_BG_070512_8223_41/' ; folder with .dim and .data
    img_file_arr = ['CHRIS_BG_070512_8223_41', 'CHRIS_BG_070512_8224_41','CHRIS_BG_070512_8225_41', 'CHRIS_BG_070512_8226_41', 'CHRIS_BG_070512_8227_41'] ; name of the input files, '_NR.dim' is added later
    img_cl_mask = ['','','','',''] ; files with probabilistic cloud mask, empty if this is not available
  end
  2: begin ;*** Beijing2 ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_Yuchu/CHRIS_BG_070625_859D_41/'
    img_file_arr = ['CHRIS_BG_070625_859D_41','CHRIS_BG_070625_859E_41','CHRIS_BG_070625_859F_41','CHRIS_BG_070625_85A0_41','CHRIS_BG_070625_85A1_41']
    img_cl_mask = ['','','','','']
  end
  3: begin ;*** Cabo de Gata ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_Domin/'
    img_file_arr = ['CHRIS_DG_070710_86BF_41', 'CHRIS_DG_070710_86C0_41', 'CHRIS_DG_070710_86C1_41', 'CHRIS_DG_070710_86C2_41', 'CHRIS_DG_070710_86C3_41']
    img_cl_mask = ['','','','','']
  end
  4: begin ;*** Chilbolton ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_vidhya/'
    img_file_arr = ['CHRIS_CL_060617_6E32_41', 'CHRIS_CL_060617_6E33_41', 'CHRIS_CL_060617_6E34_41', 'CHRIS_CL_060617_6E35_41', 'CHRIS_CL_060617_6E36_41']
    img_cl_mask = ['','','','','']
  end
  5: begin ;*** Venezuela #1 ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_sanchez/Venezuela Site/CHRIS_HD_070314_7E2C_41/'
    img_file_arr = ['CHRIS_HD_070314_7E2C_41', 'CHRIS_HD_070314_7E2D_41', 'CHRIS_HD_070314_7E2E_41', 'CHRIS_HD_070314_7E2F_41', 'CHRIS_HD_070314_7E30_41']
    img_cl_mask = ['','','','','']
  end
  6: begin ;*** Venezuela #2 ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_sanchez/Venezuela Site/CHRIS_HD_070324_7EF9_41/'
    img_file_arr = ['CHRIS_HD_070324_7EF9_41', 'CHRIS_HD_070324_7EFA_41', 'CHRIS_HD_070324_7EFB_41', 'CHRIS_HD_070324_7EFC_41', 'CHRIS_HD_070324_7EFD_41']
    img_cl_mask = ['','','','','']
  end
  7: begin ;*** Sudbury2 ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_AnitaSimic/'
    img_file_arr = ['CHRIS_SU_070810_88D6_41', 'CHRIS_SU_070810_88D7_41', 'CHRIS_SU_070810_88D8_41', 'CHRIS_SU_070810_88D9_41', 'CHRIS_SU_070810_88DA_41']
    img_cl_mask = ['CHRIS_SU_070810_88D6_41_NR_coi.bsq','CHRIS_SU_070810_88D7_41_NR_coi.bsq','CHRIS_SU_070810_88D8_41_NR_coi.bsq','CHRIS_SU_070810_88D9_41_NR_coi.bsq','CHRIS_SU_070810_88DA_41_NR_coi.bsq']
  end
  8: begin ;*** Los Monegros ***
    path_dat = '/misc/xx4/luis/CHRIS-PROBA/CHRIS_img/CHRIS_img_Thomas/'
    img_file_arr = ['CHRIS_LN_060416_69EB_41', 'CHRIS_LN_060416_69EC_41', 'CHRIS_LN_060416_69ED_41', 'CHRIS_LN_060416_69EE_41', 'CHRIS_LN_060416_69EF_41']
    img_cl_mask = ['CHRIS_LN_060416_69EB_41_clouds.bsq','CHRIS_LN_060416_69EC_41_clouds.bsq','CHRIS_LN_060416_69ED_41_clouds.bsq','CHRIS_LN_060416_69EE_41_clouds.bsq','CHRIS_LN_060416_69EF_41_clouds.bsq']
  end

endcase

if ~keyword_set(img_file_arr) then stop
num_img = n_elements(img_file_arr)

img_file_data_arr = path_dat + img_file_arr + '_NR.data/'
img_file_dim_arr = path_dat + img_file_arr + '_NR.dim'

; reads inputs from .dim
CHRIS_read_dim, img_file_dim_arr, mode, ncols, nrows, fza_arr, vza_arr, vaa_arr, sza_arr, saa_arr, hsf_arr, month, jday, gmt, wl_center, wl_fwhm 
;********************************************************************************************


;********************************************************************************************
;***** pre-calculations *****

daysxmonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31] ;data
pix_size_arr = [36., 17., 17., 17., 17.] ;data

px_size = pix_size_arr[mode-1]
doy = total(daysxmonth[0:month-1]) - daysxmonth[month - 1] + jday

phi_arr = abs(vaa_arr - saa_arr)
wh_180 = where(phi_arr gt 180., cnt_180)
if cnt_180 gt 0 then phi_arr[wh_180] = 360. - phi_arr[wh_180]

varsol, jday, month, dsol
dn2rad = dsol * dsol * 1.e-3
num_bd = n_elements(wl_center)

tot_pix = long(ncols)*nrows

if wv_val eq 0 then begin
  wv_max = 2.
  wv_min = 0.5
  arg_arr = findgen(365) * !pi / 364
  wv_doy = (wv_max - wv_min) * sin(arg_arr) + wv_min
  wv_val = wv_doy[doy]
endif
;********************************************************************************************


;********************************************************************************************
;***** reads data from disk *****
toa_img_arr = fltarr(num_img, ncols, nrows, num_bd, /nozero) ;radiance
cld_arr = fltarr(ncols, nrows, num_img) ; cloud_probability
msk_arr = bytarr(ncols, nrows, num_img) ; saturation mask

dum_img = lonarr(ncols, nrows) ; dummy for reading, radiance
dum_img2 = intarr(ncols, nrows) ; dummy for reading, saturation (ERROR: it appears in BEAM as int32!!!!)
dum_img3 = fltarr(ncols, nrows) ; ; dummy for reading, cloud probability

for ind_img = 0, num_img - 1 do begin

  for bd = 0, num_bd - 1 do begin
    openr, 1, img_file_data_arr[ind_img] + 'radiance_' + strtrim(bd+1, 2) + '.img', /swap_if_little_endian
    readu, 1, dum_img
    toa_img_arr[ind_img, *, *, bd] = dum_img * dn2rad
    close, 1

    openr, 1, img_file_data_arr[ind_img] + 'mask_' + strtrim(bd+1,2) + '.img', /swap_if_little_endian
    readu, 1, dum_img2
    close, 1
    wh = where(dum_img eq 3, cnt)
    if cnt gt 0 then msk_arr[i*tot_pix + wh] = 1
  endfor

  if img_cl_mask[ind_img] ne '' then begin
    openr, 1, path_dat + img_cl_mask[ind_img]
    readu, 1, dum_img3
    close, 1
    cld_arr[*, *, ind_img] = dum_img3
  endif

endfor
msk_arr = ~msk_arr
dum_img = 0 & dum_img2 = 0 & dum_img3 = 0
;********************************************************************************************

; reads atmospheric LUT
read_LUT_CHRIS_formatted, file_LUT, vza_gr, sza_gr, phi_gr, hsf_gr, aot_gr, cwv_gr, wvl_LUT

; generates spectral weights for resampling
s_norm_ini = generate_filter(wvl_LUT, wl_center, wl_fwhm)

;********************************************************************************************
; AOT retrieval
if aot550_val eq 0 then begin
  if mode ne 2 then $
    AOT_retr_land, toa_img_arr, ncols, nrows, wl_center, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, aot_gr, msk_arr, mode, wv_val, cld_aot_thre, aot550_arr $
  else begin
    AOT_retr_water, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, aot_gr, msk_arr, file_Isc, mode, wv_val, cld_aot_thre, aot550 
    aot550_arr = replicate(aot550, num_img)
  endelse
endif else aot550_arr = replicate(aot550_val, num_img)

; CWV/Refl retrieval
case mode of
  1: CHRIS_AC_mode15, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, cwv_gr, aot550_arr, wv_val, EM_file, wvl_LUT, SpecPol_flg, ady_flg, px_size, cld_aot_thre, cld_rfl_thre, img_file_arr, wv_arr, refl_img_arr, dwl_arr, cal_coef

  2: CHRIS_AC_mode234, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, aot550_arr, wv_val, ady_flg, px_size, cld_rfl_thre, refl_img_arr

  3: CHRIS_AC_mode234, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, aot550_arr, wv_val, ady_flg, px_size, cld_rfl_thre, refl_img_arr

  4: CHRIS_AC_mode234, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, aot550_arr, wv_val, ady_flg, px_size, cld_rfl_thre, refl_img_arr

  5: CHRIS_AC_mode15, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, cwv_gr, aot550_arr, wv_val, EM_file, wvl_LUT, SpecPol_flg, ady_flg, px_size, cld_aot_thre, cld_rfl_thre, img_file_arr, wv_arr, refl_img_arr, dwl_arr, cal_coef
endcase
;********************************************************************************************

; Outputs are written to file
lab_str = '_REFL'
if ady_flg eq 1  then lab_str = lab_str + '_ADJ'
if (mode eq 1 or mode eq 5) then lab_str = lab_str + '_WV'
if SpecPol_flg eq 1 and (mode eq 1 or mode eq 5) then lab_str = lab_str + '_POL'
if SpecPol_flg eq 2 and (mode eq 1 or mode eq 5) then lab_str = lab_str + '_POL_LP'
lab_str = lab_str + '.img'

for ind_img = 0, num_img - 1 do begin

  str_name = filext(img_file_arr[ind_img], /name)
  str_nm = strmid(str_name, 0, 23)

  image_name = path_dat + str_name + lab_str
  openw, 1, image_name
  writeu, 1, refl_img_arr[ind_img, *, *, *]
  close, 1
  desc_arr = 'Surface reflectance (0-10000)'
  envi_setup_head, fname=image_name, data_type=2, ns=ncols, nl=nrows, nb=num_bd, interleave=0, offset=0, wl=wl_center, descrip=desc_arr, fwhm = wl_fwhm, /write, /open

  if mode eq 1 or mode eq 5 then begin
    image_name = path_dat + str_name + '_WV.img'
    openw, 1, image_name
    writeu, 1, wv_arr[*, *, ind_img]
    close, 1
    desc_arr = 'Water vapor (gcm-2)'
    envi_setup_head, fname=image_name, data_type=4, ns=ncols, nl=nrows, nb=1, interleave=0, offset=0, descrip=desc_arr, /write, /open
  endif

endfor

END

;********************************************************************************************
;********************************************************************************************


;********************************************************************************************
; AOT_retr_land:
;
; Retrieves AOT from land targets (modes 1, 3, 4, 5)
;********************************************************************************************
; TODO - implement in Java
PRO AOT_retr_land, toa_img_arr, ncols, nrows, wl_center, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, aot_gr, msk_arr, mode, wv_val, cld_aot_thre, aot550_arr

common lut_inp, lut1, lut2, num_par, num_wvl_LUT, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

num_bd = n_elements(wl_center)
num_img = n_elements(toa_img_arr[*, 0, 0, 0])

ret_flg=0

num_min_pix = 100. / num_img

vis_lim = [420., 690.] ; wavelength range considered for the evaluation of the darkest pixel
wh_vis = where(wl_center ge vis_lim[0] and wl_center le vis_lim[1], num_bd_vis)

hsurf = mean(hsf_arr) 

;AOT calculated separately for the 5 angles
aot550_arr = fltarr(num_img)

for ind_img = 0, num_img - 1 do begin

; minimum radiance at vis_lim range is found from #num_min_pix pixels
min_arr = fltarr(num_min_pix, num_bd_vis)
dum = fltarr(ncols, nrows)
for i = 0, num_bd_vis - 1 do begin 
  dum = toa_img_arr[ind_img, *, *, wh_vis[i]] * msk_arr[*, *, ind_img]
  wh_no0 = where(dum ne 0.)
  dum_tmp = dum[wh_no0]
  ind_min=sort(dum_tmp)
  min_arr[*, i] = dum_tmp[ind_min[0:num_min_pix-1]]     
endfor 
 
; Top AOT thresholds estimated from min_arr (AOT/min_arr>Lpath)
dim_aot = n_elements(aot_gr)
cnt_neg = 0
j=0
; Loop with coarse step in AOT
while cnt_neg lt num_min_pix and j lt dim_aot do begin
  f_int = interpol_lut(vza_arr[ind_img], sza_arr[ind_img], phi_arr[ind_img], hsurf, aot_gr[j], wv_val)
  a = f_int # s_norm_ini
  lpw_vza = a[0, *] * 10000.
  cnt_neg = 0
  for k = 0, num_min_pix-1 do begin
    wh_neg = where(min_arr[k, *]-lpw_vza[wh_vis] le 0., cnt_cont)
    if cnt_cont gt 0 then cnt_neg = cnt_neg + 1L
  endfor
  j = j+1
endwhile
j_max = j - 2 > 0

; Loop with fine step in AOT
if j_max ne 0 then begin
  j=1
  stp = 0.005
  cnt_neg=0
  while cnt_neg lt num_min_pix and aot_gr[j_max] + j*stp le aot_gr[dim_aot-1] do begin
    aot_lp = aot_gr[j_max] + j*stp
    f_int = interpol_lut(vza_arr[ind_img], sza_arr[ind_img], phi_arr[ind_img], hsurf, aot_lp, wv_val)
    a = f_int # s_norm_ini
    lpw_vza = a[0, *] * 10000.
    cnt_neg = 0
    for k = 0, num_min_pix-1 do begin
      wh_neg = where(min_arr[k,  *]-lpw_vza[wh_vis] le 0., cnt_cont)
      if cnt_cont gt 0 then cnt_neg = cnt_neg + 1
    endfor
    j = j+1
  endwhile 
  aot_max = aot_lp - stp
endif else begin 
  aot550 = aot_gr[0] ; if j_max=0 (AOT_max=aot_gr[0])
  ret_flg=1
endelse

; Refinement in AOT calculation through endmemeber inversion (CHRIS_AOT_inv_land), when  AOT_max != aot_gr[0]
if ret_flg eq 0 then begin

  toa_img = reform(toa_img_arr[ind_img, *, *, *])
  vza_inp = vza_arr[ind_img]
  sza_inp = sza_arr[ind_img] 
  phi_inp = phi_arr[ind_img]

  tot_pix = long(ncols) * nrows
  wh_land = where(cld_arr[*, *, ind_img] le cld_aot_thre and msk_arr[*, *, ind_img] eq 1, cnt_land)
  toa_sub = fltarr(cnt_land, num_bd)
  for i = 0, num_bd - 1 do toa_sub[*, i] = toa_img[wh_land + tot_pix * i]
  toa_img = 0

  dem_mean = hsf_arr[ind_img]
  mus_mean = cos(sza_arr[ind_img]*!dtor)

  n_pts_gr = 10
  aot_min = aot_gr[0]
  aot_gr_inv = aot_min + (aot_max - aot_min) * findgen(n_pts_gr) / (n_pts_gr - 1)
  lpw_aot = fltarr(num_bd, n_pts_gr)
  egl_aot = fltarr(num_bd, n_pts_gr)
  sab_aot = fltarr(num_bd, n_pts_gr)
  for i = 0, n_pts_gr - 1 do begin ; extraction of AOT breakpoints from LUT
    f_int = interpol_lut(vza_arr[ind_img], sza_arr[ind_img], phi_arr[ind_img], dem_mean, aot_gr_inv[i], wv_val)
    a = f_int # s_norm_ini
    lpw_aot[*, i] = a[0, *] * 1.e+4 
    egl_aot[*, i] = (a[1, *] * mus_mean + a[2, *]) * 1.e+4
    sab_aot[*, i] = a[3, *]
  endfor

  ; call to routine performing end-member inversion
  CHRIS_AOT_inv_land, toa_sub, lpw_aot, egl_aot, sab_aot, dn2rad, wl_center, wl_fwhm, aot_gr_inv, aot550, valid_flg 

endif

aot550_arr[ind_img] = aot550

endfor

print, 'AOT@550 = ', aot550_arr

END

;********************************************************************************************
; AOT_retr_water:
;
; Retrieves AOT from water targets (mode 2)
;********************************************************************************************
; TODO - implement in Java
PRO AOT_retr_water, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini,fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, aot_gr, msk_arr, file_Isc, mode, wv_val, cld_aot_thre, aot550

common lut_inp, lut1, lut2, num_par, num_wvl_LUT, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

num_bd = n_elements(wl_center)
num_img = n_elements(toa_img_arr[*, 0, 0, 0])

; Solar irradiance is read and spectrally resampled, it is used in the L_toa->ro_toa conversion
a=''
num_lin_Isc = 21001
tmp= fltarr(2, num_lin_Isc)
openr, 1, file_Isc
readf, 1, a
readf, 1, a
readf, 1, tmp
close, 1
wvl_irr = 1.e+7 / tmp[0, *]
solirr_ini = tmp[1, *] * tmp[0, *]^2

s_norm_irr = generate_filter(wvl_irr, wl_center, wl_fwhm) 
solirr = solirr_ini # s_norm_irr

; Water pixels are extracted by means of red/nir thresholds
msk_wat_arr = fltarr(ncols, nrows, num_img)
for i = 0, num_img - 1 do begin
  wvl_red = 688. 
  wvl_nir = 780.
  wh_red = where(abs(wl_center-wvl_red) eq min(abs(wl_center-wvl_red))) & wh_red = wh_red[0]
  wh_nir = where(abs(wl_center-wvl_nir) eq min(abs(wl_center-wvl_nir))) & wh_nir = wh_nir[0]

  ro_toa_red = toa_img_arr[i, *, *, wh_red] * !pi / cos(sza_arr[i]*!dtor)/solirr[wh_red]
  ro_toa_nir = toa_img_arr[i, *, *, wh_nir] * !pi / cos(sza_arr[i]*!dtor)/solirr[wh_nir]

  msk_wat_arr[*, *, i] = (toa_img_arr[i, *, *, wh_red] gt toa_img_arr[i, *, *, wh_nir]) and (ro_toa_nir lt 0.1 and ro_toa_nir gt 0.01 and ro_toa_red gt 0.01)
endfor

num_min_pix = 100

vis_lim = [435., 690.]
wh_vis = where(wl_center ge vis_lim[0] and wl_center le vis_lim[1], num_bd_vis)

; minimum radiance at vis_lim range is found from #num_min_pix water pixels
min_arr = fltarr(num_min_pix, num_img, num_bd_vis)
dum = fltarr(ncols, nrows)
for ind_img = 0, num_img -1 do begin
  mask_prod = msk_arr[*, *, ind_img] * msk_wat_arr[*, *, ind_img]
  for i = 0, num_bd_vis - 1 do begin
    dum = toa_img_arr[ind_img, *, *, wh_vis[i]] * mask_prod
    wh_no0 = where(dum ne 0.)
    dum_tmp = dum[wh_no0]
    ind_min=sort(dum_tmp)
    min_arr[*, ind_img, i] = dum_tmp[ind_min[0:num_min_pix-1]]     
  endfor 
endfor

hsurf = mean(hsf_arr) 

; Top AOT thresholds estimated from min_arr (AOT/min_arr>Lpath)
lpw_vza = fltarr(num_img, num_bd)
dim_aot = n_elements(aot_gr)
cnt_neg = 0
j=0
; Loop with coarse step in AOT
while cnt_neg lt num_min_pix and j lt dim_aot do begin
  for i = 0, num_img - 1 do begin
    f_int = interpol_lut(vza_arr[i], sza_arr[i], phi_arr[i], hsurf, aot_gr[j], wv_val)
    a = f_int # s_norm_ini
    lpw_vza[i, *] = a[0, *] * 10000.
  endfor
  cnt_neg = 0
  for k = 0, num_min_pix-1 do begin
    wh_neg = where(min_arr[k, *, *]-lpw_vza[*, wh_vis] le 0., cnt_cont)
    if cnt_cont gt 0 then cnt_neg = cnt_neg + 1
  endfor
 print, cnt_neg, aot_gr[j]
  j = j+1
endwhile
j_max = j - 2 > 0

; Loop with fine step in AOT
if j_max ne 0 then begin
  j=1
  stp = 0.005
  cnt_neg=0
  while cnt_neg lt num_min_pix do begin
    for i = 0, num_img - 1 do begin
      aot_lp = aot_gr[j_max] + j*stp
      f_int = interpol_lut(vza_arr[i], sza_arr[i], phi_arr[i], hsurf, aot_lp, wv_val)
      a = f_int # s_norm_ini
      lpw_vza[i, *] = a[0, *] * 10000.
    endfor
    cnt_neg = 0
    for k = 0, num_min_pix-1 do begin
      wh_neg = where(min_arr[k, *, *]-lpw_vza[*, wh_vis] le 0., cnt_cont)
      if cnt_cont gt 0 then cnt_neg = cnt_neg + 1
    endfor
    j = j+1
  endwhile 
  aot_max = aot_lp - stp
endif else begin 
  aot550 = aot_gr[0]
  return
endelse

aot550 = aot_max

;stop

END

;********************************************************************************************
; CHRIS_AOT_inv_land:
;
; Refines AOT_max value calculated in AOT_retr_land by the inversion of the TOA radiance 
;    at 5 reference pixels
;********************************************************************************************
; TODO - implement in Java
PRO CHRIS_AOT_inv_land, toa_sub, lpw_aot, egl_aot, sab_aot, dn2rad, wl_center, wl_fwhm, aot_gr_inv, aot550, valid_flg

common fits_atm, aot_old
common fits, toa, chi_sq
common inversion, lpw, egl, sab, ro_veg, ro_sue, aot_gr_inv2, dim_aot_inv, num_pix, num_bd, ref_pix, wl_center_inv
common static, lpw_int, egl_int, sab_int

lpw = lpw_aot
egl = egl_aot
sab = sab_aot

num_bd = n_elements(wl_center)

wl_MER = [0.412545, 0.442401, 0.489744, 0.509700, 0.559634, 0.619620, 0.664640, 0.680902, 0.708426, 0.753472, 0.761606, 0.778498, 0.864833, 0.884849, 0.899860] *1000.
ro_veg_MER_1 = [0.0235,0.0267,0.0319,0.0342,0.0526,0.0425,0.0371,0.0369,0.0789,0.3561,0.3698,0.3983,0.4248,0.4252,0.4254] ;veg_verde_new
ro_veg_MER_2 = [0.0206,0.02984,0.0445,0.0498,0.0728,0.0821,0.0847,0.0870,0.1301,0.1994,0.2020,0.2074,0.2365,0.2419,0.2459] ;bosque
ro_veg_MER_3 = [0.0138,0.0158,0.0188,0.021,0.0395,0.0279,0.0211,0.0206,0.0825,0.2579,0.2643,0.2775,0.3201,0.3261,0.330700]
ro_sue_MER = [0.0490,0.0860,0.1071,0.1199,0.1679,0.2425,0.2763,0.2868,0.3148,0.3470,0.3498,0.3558,0.3984,0.4062,0.4120]

ro_veg1 = interpol(ro_veg_MER_1, wl_MER, wl_center) ;resampling to CHRIS bands
ro_veg2 = interpol(ro_veg_MER_2, wl_MER, wl_center)
ro_veg3 = interpol(ro_veg_MER_3, wl_MER, wl_center)
ro_sue = interpol(ro_sue_MER, wl_MER, wl_center)

ro_veg_all = [[ro_veg1], [ro_veg2], [ro_veg3]]

num_pix = 5
aot_gr_inv2 = aot_gr_inv
dim_aot_inv = n_elements(aot_gr_inv)

wh_red = where(wl_center gt 670.)
wh_nir = where(wl_center gt 785.)
ind_red = wh_red[0] & ind_nir = wh_nir[0]
rad_red = toa_sub[*, ind_red]
rad_nir = toa_sub[*, ind_nir]
ndvi_img = (rad_nir - rad_red) / (rad_nir + rad_red)
rad_red = 0 & rad_nir = 0

; vegetation / soil pixels separated into three categories (HI, ME, LO) by NDVI thresholds
index_hi = where(ndvi_img gt 0.4 and ndvi_img lt 0.90, cont_hi)    ;index_* : indices of pixels within the ndvi range
index_me = where(ndvi_img gt 0.10 and ndvi_img lt 0.4, cont_me)    ;cont_*  : number of pixels within the ndvi range
index_lo = where(ndvi_img gt 0.01 and ndvi_img lt 0.10, cont_lo)

max_cat = 100

; reference pixels are extracted
if (2 + cont_me lt num_pix) or (cont_hi lt 2) then valid_flg = 0 else begin

  cont_hi = cont_hi < max_cat
  cont_me = cont_me < max_cat
  cont_lo = cont_lo < max_cat

  valid_flg = 1

  pos_ref_hi = lonarr(cont_hi)

  ref_hi = lonarr(cont_hi)                                ;ref_* : indices of selected pixels
  ord_arr = reverse(sort(ndvi_img[index_hi]))
  for i = 0, cont_hi - 1 do ref_hi[i] = ord_arr[i]        
  pos_ref_hi = index_hi[ref_hi]
  ref_pix_hi = fltarr(cont_hi, num_bd)
  for i=0, cont_hi - 1 do ref_pix_hi[i, *] = toa_sub[pos_ref_hi[i], *]

  if cont_lo gt 0 then begin
    pos_ref_lo = lonarr(cont_lo)
    ref_lo = lonarr(cont_lo)
    ord_arr = sort(ndvi_img[index_lo])
    for i = 0, cont_lo - 1 do ref_lo[i] = ord_arr[i]
    pos_ref_lo = index_lo[ref_lo]
    ref_pix_lo = fltarr(cont_lo, num_bd)
    for i=0, cont_lo - 1 do ref_pix_lo[i, *] = toa_sub[pos_ref_lo[i], *]
  endif else ref_pix_lo = 0
  if cont_me gt 0 then begin
    pos_ref_me = lonarr(cont_me)
    ref_me = lonarr(cont_me)            
    ord_arr = reverse(sort(ndvi_img[index_me]))
    for i = 0, cont_me - 1 do ref_me[i] = ord_arr[i]        ; Se asigna pixeles con max(NDVI)
    pos_ref_me = index_me[ref_me]
    ref_pix_me = fltarr(cont_me, num_bd)
    for i=0, cont_me - 1 do ref_pix_me[i, *] = toa_sub[pos_ref_me[i], *]
  endif else ref_pix_me = 0

  ; set of reference pixels in the three categories and n_lim sets stored in ref_pix_all
  n_lim = (cont_hi / 2) < (cont_me / 3)
  ref_pix_all = fltarr(n_lim, num_pix, num_bd)
  for i = 0, n_lim - 1 do begin
    ref_pix_all[i, 0:1, *] = ref_pix_hi[2*i:2*i+1, *]
    if i lt cont_lo then begin
      ref_pix_all[i, 2:3, *] = ref_pix_me[2*i:2*i+1, *]
      ref_pix_all[i, 4, *] = ref_pix_lo[i, *]
    endif else ref_pix_all[i, 2:4, *] = ref_pix_me[2*i:2*i+2, *]
  endfor

;***************************************************

; Inversion is performed by Powell minimization routine for n_EM_veg * lim_ref_sets (number of endmembers * sets of reference pixels) combinations, 
;     and the AOT leading to the minimum of the Merit Function 'minim_TOA' (fmin) is selected
  n_EM_veg = 3 
  lim_ref_sets = 5

  n_ref_sets = n_elements(ref_pix_all[*, 0, 0])
  n_ref_sets = n_ref_sets < lim_ref_sets

  ftol = 1.e-4

  aot_arr = fltarr(n_ref_sets)
  fmin_arr = fltarr(n_EM_veg)
  aot_arr_aux = fltarr(n_EM_veg)
 
  wl_center_inv = 1000. / wl_center ;spectral weights for the inversion
  wh_nir_no = where(wl_center gt 850., complement = wh_vis, cnt_nir_no)
  if cnt_nir_no then wl_center_inv[wh_nir_no] = 0.
  if wl_center[0] le 440. then wl_center_inv[0] = 0.

  num_x = 2*num_pix+1

  for ind = 0, n_ref_sets - 1 do begin

    ref_pix = reform(ref_pix_all[ind, *, *])

    for ind_EM = 0, n_EM_veg - 1 do begin

      weight = [2., 2., 1.5, 1.5, 1.]

      P = fltarr(num_x)
      for i = 0, num_pix - 1 do begin
        ndvi_val = (ref_pix[i, ind_nir] - ref_pix[i, ind_red]) / (ref_pix[i, ind_nir] + ref_pix[i, ind_red])
        dum = 1.3 * ndvi_val + 0.25
        P[2*i] = dum  > 0.
        P[2*i+1] = (1. - dum) > 0.
       endfor

      P[10] = aot_gr_inv[dim_aot_inv / 2]
      aot_old = aot_gr_inv[dim_aot_inv / 2] - 0.05
 
      ftol = 1.e-7
      xi = fltarr(num_x, num_x)
      ind2 = indgen(num_x)
      for i = 0, num_x - 1 do xi[ind2[i], ind2[i]] = 1.

      ro_veg = ro_veg_all[*, ind_EM]
 
      POWELL, P, xi, ftol, fmin, 'minim_TOA', ITER = iter, ITMAX = 20000

      aot_arr_aux[ind_EM] = p[num_x-1]
      fmin_arr[ind_EM] = fmin 

    endfor

    wh_fmin = where(fmin_arr eq min(fmin_arr))
    aot_arr[ind] = aot_arr_aux[wh_fmin[0]]
    EM_code = wh_fmin[0] + 1

  endfor

  if n_ref_sets gt 1 then begin
    aot_mean = mean(aot_arr)
    aot_stddev = stddev(aot_arr)

    wh_stddev = where(aot_arr ge aot_mean - aot_stddev * 1.5 and aot_arr le aot_mean + aot_stddev * 1.5, cnt_wh_stddev)
    if cnt_wh_stddev gt 0 then begin
      aot550 = mean(aot_arr[wh_stddev])
      aot_stddev = stddev(aot_arr[wh_stddev])
    endif else vis = vis_mean
  endif else begin
    aot = aot_arr[0]
    aot_stddev = 0.
  endelse

endelse

END

;********************************************************************************************
; minimim_TOA:
;
; Returns the value of the Merit Function called by Powell for the AOT inversion
;********************************************************************************************
; TODO - implement in Java
FUNCTION minim_TOA, x
common fits, toa, chi_sq
common fits_atm, vis_old
common inversion, lpw, egl, sab, ro_veg, ro_sue, vis_gr, dim_vis, n_pix, num_bd, ref_pix, wl_center_inv
common static, lpw_int, egl_int, sab_int

wh = where(x lt 0., cont_neg)
if cont_neg le 0. and x[10] gt vis_gr[0] and x[10] lt vis_gr[dim_vis - 1] then begin

  weight = [2., 2., 1., 1., 1.5]

  vis = x[10] 
  if vis ne vis_old then begin
    wh = where(vis gt vis_gr)
    vis_inf = wh[n_elements(wh) - 1]

    delta = 1. / (vis_gr[vis_inf + 1] - vis_gr[vis_inf])

    lpw_int = ((lpw[*, vis_inf + 1] - lpw[*, vis_inf]) * vis + lpw[*, vis_inf] * vis_gr[vis_inf + 1] - $
                       lpw[*, vis_inf + 1] * vis_gr[vis_inf]) * delta

    egl_int = ((egl[*, vis_inf + 1] - egl[*, vis_inf]) * vis + egl[*, vis_inf] * vis_gr[vis_inf + 1] - $
                       egl[*, vis_inf + 1] * vis_gr[vis_inf]) * delta

    sab_int = ((sab[*, vis_inf + 1] - sab[*, vis_inf]) * vis + sab[*, vis_inf] * vis_gr[vis_inf + 1] - $
                       sab[*, vis_inf + 1] * vis_gr[vis_inf]) * delta
  endif

  toa = fltarr(n_pix, num_bd)
  chi_sq = fltarr(n_pix)
  for i = 0, n_pix - 1 do begin
    surf_refl = x[2 * i] * ro_veg + x[2 * i + 1] * ro_sue
    toa[i, *] = lpw_int + surf_refl * egl_int / !pi /(1 - sab_int * surf_refl)
    chi_sq[i] = weight[i] * total(wl_center_inv * (ref_pix[i,*] - toa[i,*]) ^ 2.) ;ref_pix ya corregidos con (1.e-4 * d * d)
  endfor

  minim = total(chi_sq)

  vis_old = vis

endif else minim = 5.e+8

return, minim

END

;********************************************************************************************
; CHRIS_AC_mode234:
;
; Retrieves surfae reflectance from modes 2, 3, 4
;
; - A Lambertian surface is assumed
; - No smile correction, WV retrieval and spectral polishing are performed 
;         due to the lack of the proper absorption features
;********************************************************************************************
; TODO - implement in Java
PRO CHRIS_AC_mode234, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, aot550_arr, wv_val, ady_flg, px_size, cld_rfl_thre, refl_img_arr

num_img = n_elements(toa_img_arr[*, 0, 0, 0])
num_bd = n_elements(wl_center)
dim_cwv = n_elements(cwv_gr)

tot_pix = long(ncols) * nrows

refl_img_arr = intarr(num_img, ncols, nrows, num_bd)

for ind_img = 0, num_img - 1 do begin

  refl_img = fltarr(ncols, nrows, num_bd)

  print, 'Reflectance Retrieval, image #', ind_img + 1 

  toa_img = reform(toa_img_arr[ind_img, *, *, *])
  vza_inp = vza_arr[ind_img]
  sza_inp = sza_arr[ind_img] & mus_inp = cos(sza_inp * !dtor)
  phi_inp = phi_arr[ind_img]
  hsf_inp = hsf_arr[ind_img]
  aot550 = aot550_arr[ind_img]

  tot_pix = long(ncols) * nrows ;Masking of invalid pixels
  wh_land = where(cld_arr[*, *, ind_img] le cld_rfl_thre and msk_arr[*, *, ind_img] eq 1, cnt_land)
  toa_sub = fltarr(cnt_land, num_bd)  
  for i = 0, num_bd - 1 do toa_sub[*, i] = toa_img[wh_land + tot_pix * i]
  refl_sub = fltarr(cnt_land, num_bd)  
  toa_img = 0

  a=interpol_lut(vza_inp, sza_inp, phi_inp, hsf_inp, aot550, wv_val) ;Call to LUT
  a_flt = a # reform(s_norm_ini)
  lpw_int = a_flt[0, *]*1.e+4
  egl_int = (a_flt[1, *] * mus_inp + a_flt[2, *])*1.e+4
  sab_int = a_flt[3, *]

  for bd = 0, num_bd - 1 do begin  ;Inversion of Lambertian equation
    xterm = !pi * (toa_sub[*, bd] - lpw_int[bd]) / (egl_int[bd])
    refl_sub[*, bd] = xterm / (1. + sab_int[bd] * xterm)
  endfor

  for i = 0, num_bd - 1 do refl_img[wh_land + i * tot_pix] = refl_sub[*, i]

  if ady_flg eq 1 then begin ; adjacency correction
    fac_dst = 1000. / px_size 
    a=interpol_lut(vza_inp, sza_inp, phi_inp, hsf_arr[ind_img], aot550, wv_val)
    rat_mean = reform(a[4, *]#s_norm_ini)
    for bd = 0, num_bd - 1 do begin
      refl_mean = smooth(refl_img[*, *, bd], fac_dst, /edge)
      refl_img[*, *, bd] = refl_img[*, *, bd] + rat_mean[bd] * (refl_img[*, *, bd] - refl_mean)
    endfor
  endif

  wh_black = where(refl_img lt 0., cnt_black)
  if cnt_black gt 0 then refl_img[wh_black] = 0. ; negative pixels are set to 0

  refl_img_arr[ind_img, *, *, *] = fix(refl_img * 10000.)

endfor

END


;********************************************************************************************
; CHRIS_AC_mode15:
;
; Retrieves surfae reflectance from modes 1, 5
;
; - A Lambertian surface is assumed
; - Smile correction and WV retrieval are performed 
; - Spectral polishing, under user command
;********************************************************************************************
; TODO - implement in Java
PRO CHRIS_AC_mode15, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, msk_arr, cwv_gr, aot550_arr, wv_val, EM_file, wvl_LUT, SpecPol_flg,  ady_flg, px_size, cld_aot_thre, cld_rfl_thre, img_file_arr, wv_arr, refl_img_arr, dwl_arr_sm, cal_coef

; call to smile processor, set of spectral shift values in the x-direction derived
; smile_processor, toa_img_arr, ncols, nrows, wl_center, wl_fwhm, s_norm_ini, fza_arr, vza_arr, sza_arr, phi_arr, hsf_arr, cld_arr, cld_aot_thre, wvl_LUT, aot550_arr[0], msk_arr, wv_val, dwl_arr_sm

num_img = n_elements(toa_img_arr[*, 0, 0, 0])
num_bd = n_elements(wl_center)
dim_cwv = n_elements(cwv_gr)

tot_pix = long(ncols) * nrows

; spectral weighting facotrs calculated column-wise in order to consider spectral shift
num_wvl_LUT = n_elements(wvl_LUT)
s_norm_sh = fltarr(ncols, num_wvl_LUT, num_bd)
for i = 0, ncols - 1 do s_norm_sh[i, *, *] =generate_filter(wvl_LUT, wl_center + dwl_arr_sm[i], wl_fwhm)

wv_arr =fltarr(ncols, nrows, num_img)
refl_img_arr = intarr(num_img, ncols, nrows, num_bd)

mat_tmp = fltarr(ncols, nrows)
for i = 0, ncols - 1 do mat_tmp[i, *] = i ; matrix of column indices

lpw_wv_col = fltarr(ncols, dim_cwv, num_bd) ; used in CWV retrieval, ncols to account for smile
edr_wv_col = fltarr(ncols, dim_cwv, num_bd)
edf_wv_col = fltarr(ncols, dim_cwv, num_bd)
sab_wv_col = fltarr(ncols, dim_cwv, num_bd)

;**********************************************************************************
; Atmospheric correction

for ind_img = 0, num_img - 1 do begin

  refl_img = fltarr(ncols, nrows, num_bd)

  toa_img = reform(toa_img_arr[ind_img, *, *, *])
  vza_inp = vza_arr[ind_img]
  sza_inp = sza_arr[ind_img] & mus_inp = cos(sza_inp * !dtor)
  phi_inp = phi_arr[ind_img]
  hsf_inp = hsf_arr[ind_img]
  aot550 = aot550_arr[ind_img]
  tot_pix = long(ncols) * nrows
  wh_land = where(cld_arr[*, *, ind_img] le cld_rfl_thre and msk_arr[*, *, ind_img] eq 1, cnt_land)
  toa_sub = fltarr(cnt_land, num_bd)
  for i = 0, num_bd - 1 do toa_sub[*, i] = toa_img[wh_land + tot_pix * i]
  toa_img = 0

  mat_arr = mat_tmp[wh_land]

  for i = 0, dim_cwv - 1 do begin
    a=interpol_lut(vza_inp, sza_inp, phi_inp, hsf_arr[ind_img], aot550, cwv_gr[i])
    for ind = 0, ncols - 1 do begin
      a_flt = a # reform(s_norm_sh[ind, *, *]) ; per-column resampling
      lpw_wv_col[ind, i, *] = a_flt[0, *]*1.e+4
      edr_wv_col[ind, i, *] = a_flt[1, *]*1.e+4
      edf_wv_col[ind, i, *] = a_flt[2, *]*1.e+4
      sab_wv_col[ind, i, *] = a_flt[3, *]
    endfor
  endfor

  ; CWV retrieval, column-wise
  ; reflectance (refl_sub) calculated simultaneously to CWV (wv_sub)
  CHRIS_retrieve_wv_no_dem, toa_sub, cos(sza_inp*!dtor), lpw_wv_col, edr_wv_col, edf_wv_col, sab_wv_col, wl_center, cwv_gr, wv_val, mat_arr, wv_sub, refl_sub

  wv_arr[wh_land + tot_pix * ind_img] = wv_sub

  wv_val = mean(wv_sub)
  for i = 0, num_bd - 1 do refl_img[wh_land + i * tot_pix] = refl_sub[*, i]

;   wh_black = where(refl_img lt 0., cnt_black)
;   if cnt_black gt 0 then refl_img[wh_black] = 0. ; negative pixels are set to 0


  ; Spectral polishing by comparison with vegetation and soil endmembers
  if SpecPol_flg ne 0 then begin

    num_lin_EM = 811
    tmp= fltarr(3, num_lin_EM)
    a = ''
    openr, 1, EM_file
    readf, 1, a
    readf, 1, tmp
    close, 1
    wvl_EM = reform(tmp[0, *])*1000.
    ro_veg_hyp = interpol(reform(tmp[1, *]), wvl_EM, wvl_LUT)
    ro_sue_hyp = interpol(reform(tmp[2, *]), wvl_EM, wvl_LUT)
    ro_EM = [ro_veg_hyp#s_norm_ini, ro_sue_hyp#s_norm_ini] ; endmembers are read from file
    tmp=0

    num_pix = 50

    wh_red = where(wl_center ge 670.)
    wh_nir = where(wl_center ge 785.)
    ind_red = wh_red[0] & ind_nir = wh_nir[0]

    ndvi_img = (refl_img[*, *, ind_nir] - refl_img[*, *, ind_red]) / (refl_img[*, *, ind_nir] + refl_img[*, *, ind_red]) ; spectral contrast discrimination by means of NDVI categories
    ndvi_min = 0.1

    wh_min = where(ndvi_img gt ndvi_min and cld_arr[*, *, ind_img] le cld_aot_thre and msk_arr[*, *, ind_img] eq 1 and refl_img[*, *, ind_nir] gt 0.2 and refl_img[*, *, ind_nir] le 0.75, cnt_min)
    if cnt_min gt 0. then ndvi_img = ndvi_img[wh_min]

    ndvi_ind = sort(ndvi_img)

    cal_ind = [ndvi_ind[0:(num_pix/2)-1], ndvi_ind[cnt_min-num_pix/2:cnt_min-1]]

    ref_pix = fltarr(num_pix, num_bd)
    tot_pix = long(ncols) * nrows
    for bn = 0, num_bd - 1 do ref_pix[*, bn] = refl_img[wh_min[cal_ind] + bn * tot_pix] ; reference pixels for recalibration stored in ref_pix

    refl_tmp = refl_img

    if SpecPol_flg eq 1 then begin

      ref_fit = fltarr(num_pix, num_bd)
      for i = 0, num_pix - 1 do begin
        res = regress(reform(ro_EM), reform(ref_pix[i, *]), yfit= tmp)
        ref_fit[i, *] = tmp ; smooth reference pixels calculated from endmember inversion
      endfor

      cal_coef = fltarr(num_bd)
      ref_rec = fltarr(num_pix, num_bd)
      weight = replicate(1.0, num_pix)
      A=1.0
      for i = 0, num_bd-1 do begin
        res = curvefit(ref_pix[*, i], ref_fit[*, i], weight, A, FUNCTION_NAME='gfunct'); mutiplicative calibration coefficients from comparison of real and smooth reference pixels
        cal_coef[i] = A  
      endfor
      wvl_lim = [694.7, 772.5] ; red-edge region, different treatment
      wh = where(wl_center ge wvl_lim[0] and wl_center le wvl_lim[1])
      cal_mean = smooth(cal_coef[wh], 5)
      cal_coef[wh] = 1. + (cal_coef[wh] - cal_mean)

      for bn = 0, num_bd - 1 do refl_img[*, *, bn] = refl_img[*, *, bn] * cal_coef[bn]

    endif else if SpecPol_flg eq 2 then begin

      filt_win = 5
      for ind_x = 0, ncols - 1 do for ind_y = 0, nrows - 1 do refl_img[ind_x, ind_y, *] = smooth(refl_img[ind_x, ind_y, *], filt_win, /edge)

    endif

    n_plots = 4
    ref_pix_plt = fltarr(n_plots, num_bd)
    ind_plt = fix(indgen(n_plots)* num_pix /n_plots)
    for bn = 0, num_bd - 1 do ref_pix_plt[*, bn] = refl_img[wh_min[cal_ind[ind_plt]] + bn * tot_pix] ; reference pixels for recalibration stored in ref_pix

    if ind_img eq 0 then begin  ; Check of polishing quality: USER to decide whether to perform the polishing or not
      loadct, 39
      for i = 0, n_plots - 1 do begin
        window, i, title = 'Spectral Polishing Test, #' + strtrim(i+1, 2)
        plot, wl_center, ref_pix[ind_plt[i], *], xtitle= 'Wavelength (nm)', ytitle= 'Reflectance'
        oplot, wl_center, ref_pix_plt[i, *], color=250
      endfor
      loadct, 0
      repeat begin
        print, '*** Perform Polishing? (y/n) >' 
        a = get_kbrd(1)
        if a eq 'n' then begin 
          refl_img = refl_tmp
          SpecPol_flg = 0
        endif
      endrep until a eq 'y' or a eq 'n'
    endif

    refl_tmp = 0

  endif


  if ady_flg eq 1 then begin
    fac_dst = 1000. / px_size 
    a=interpol_lut(vza_inp, sza_inp, phi_inp, hsf_arr[ind_img], aot550, wv_val)
    rat_mean = reform(a[4, *]#s_norm_ini)
    for bd = 0, num_bd - 1 do begin
       refl_mean = smooth(refl_img[*, *, bd], fac_dst, /edge)
       refl_img[*, *, bd] = refl_img[*, *, bd] + rat_mean[bd] * (refl_img[*, *, bd] - refl_mean)
    endfor
  endif

  refl_img_arr[ind_img, *, *, *] = fix(refl_img * 10000.)

endfor

;stop

END

;********************************************************************************************
; CHRIS_retrieve_wv_no_dem:
;
; Retrieves CWV and Reflectance simultaneously
; Works on a per-column basis to consider smile
;********************************************************************************************
; TODO - implement in Java
PRO CHRIS_retrieve_wv_no_dem, toa_sub, mus_inp, lpw_wv_col, edr_wv_col, edf_wv_col, sab_wv_col, wl_center, wv_gr, wv_val, mat_arr, wv_arr, refl_arr
common chi_sq_wv_refl, lpw_wvc2, egl_wvc2, sab_wvc2, toa_wv, refl_pix, wv_gr2, dim_wv, wv_p, wv_inf

cnt_land = n_elements(toa_sub[*, 0])
ncols = n_elements(lpw_wv_col[*, 0, 0, 0])

wv_gr2 = alog(wv_gr)
dim_wv = n_elements(wv_gr)

num_bd = n_elements(wl_center)

wvl_wv = [861., 921.] ; working spectral region
wvl_out = 892. ; border of 940-nm wv absorption feature

wh_wv = where(wl_center ge wvl_wv[0] and wl_center le wvl_wv[1], complement = wh_no_wv, cnt_wv)
lim_1 = wh_wv[0]
lim_2 = wh_wv[cnt_wv - 1]
wh_out = where(wl_center le wvl_out, cnt_out)
lim_out = wh_out[cnt_out - 1]

wv_lim = [alog(wv_gr[0] + 0.001), alog(wv_gr[dim_wv - 1] - 0.001)] ; limits for 1-D inversion, in logarithmic scale

wv_retr = wv_val
val = abs(wv_gr - wv_retr) 
wh = where(val eq min(val)) & ind_retr_wv = wh[0] ; CWV guess, from wv_val

wv_arr = fltarr(cnt_land)
refl_arr = fltarr(cnt_land, num_bd)

for ind_col = 0, ncols - 1 do begin ; for each column, in order to use updated spectral positions

  lpw_wv = reform(lpw_wv_col[ind_col, *, *])
  egl_wv = reform(edr_wv_col[ind_col, *, *] * mus_inp + edf_wv_col[ind_col, *, *])
  sab_wv = reform(sab_wv_col[ind_col, *, *])

  lpw_wvc = reform(lpw_wv[ind_retr_wv, wh_wv])
  egl_wvc = reform(egl_wv[ind_retr_wv, wh_wv])
  sab_wvc = reform(sab_wv[ind_retr_wv, wh_wv])

  wh_col = where(mat_arr eq ind_col, cnt_land_col)

  if cnt_land_col gt 0 then begin

    tmp = toa_sub[wh_col, *]
    rad_wv = tmp[*, wh_wv]
    refl_wv = fltarr(cnt_land_col, cnt_wv)
    lim_sp = lim_out - lim_1
    for j = 0, lim_sp do begin
      xterm = !pi * (rad_wv[*, j] - lpw_wvc[j]) / (egl_wvc[j])
      refl_wv[*, j] = xterm / (1. + sab_wvc[j] * xterm) ; estimation of reflectance with wv_val
    endfor

    wv_arr_col = fltarr(cnt_land_col)
    refl_arr_col = fltarr(cnt_land_col, num_bd)

    for ind = 0L, cnt_land_col - 1 do begin

      coef = linfit(wl_center[wh_wv[0:lim_sp]], refl_wv[ind, 0:lim_sp]) ; inter-/extrapolation between wv-free and wv-contaminated spectral regions
      refl_wv[ind, (lim_sp + 1):(cnt_wv - 1)] = coef[0] + coef[1] * wl_center[wh_wv[(lim_sp + 1):(cnt_wv - 1)]]

      lpw_wvc2 = lpw_wv[*, wh_wv]
      egl_wvc2 = egl_wv[*, wh_wv]
      sab_wvc2 = sab_wv[*, wh_wv]

      toa_wv  = reform(rad_wv[ind, *])
      refl_pix = reform(refl_wv[ind, *])

      wv_arr_col[ind] = zbrent(wv_lim[0], wv_lim[1], FUNC_NAME = 'chisq_CHRIS_WV_refl', MaX_Iter = 10000, Tolerance = 1.e-4) ;1-D inversion by zbrent
  
      lpw_int = (lpw_wv[wv_inf +  1, *] - lpw_wv[wv_inf, *]) * wv_p + lpw_wv[wv_inf, *]
      egl_int = (egl_wv[wv_inf +  1, *] - egl_wv[wv_inf, *]) * wv_p + egl_wv[wv_inf, *]
      sab_int = (sab_wv[wv_inf +  1, *] - sab_wv[wv_inf, *]) * wv_p + sab_wv[wv_inf, *]

      xterm = !pi * (toa_sub[wh_col[ind], *] - lpw_int) / (egl_int)
      refl_arr_col[ind, *] = xterm / (1. + sab_int * xterm) ; retrieval of reflectance with the retrieved CWV value

    endfor

    wv_arr[wh_col] = wv_arr_col
    refl_arr[wh_col, *] = refl_arr_col

  endif

endfor

wv_arr = exp(wv_arr)

END


;********************************************************************************************
; chisq_CHRIS_WV_refl:
;
; Provides Merit Function for WV inversion, called by zbrent
;********************************************************************************************
; TODO - implement in Java
FUNCTION chisq_CHRIS_WV_refl, wv
common chi_sq_wv_refl, lpw, egl, sab, toa_AHS, refl_pix, wv_gr, dim_wv, wv_p, wv_inf

if (wv gt wv_gr[0] and wv lt wv_gr[dim_wv - 1]) then begin

  wv = wv[0]
  wh = where(wv gt wv_gr, cnt)
  wv_inf = wh[cnt - 1]
  wv_p = (wv - wv_gr[wv_inf]) / (wv_gr[wv_inf + 1] - wv_gr[wv_inf])

  lpw_int = reform((lpw[wv_inf +  1, *] - lpw[wv_inf, *]) * wv_p + lpw[wv_inf, *])
  egl_int = reform((egl[wv_inf +  1, *] - egl[wv_inf, *]) * wv_p + egl[wv_inf, *])
  sab_int = reform((sab[wv_inf +  1, *] - sab[wv_inf, *]) * wv_p + sab[wv_inf, *])

  toa_sim = lpw_int + refl_pix * egl_int / (1. - sab_int * refl_pix) / !pi

  chisq = total(toa_AHS - toa_sim)
;stop
endif else chisq = 5000.

return, chisq
END

;********************************************************************************************
; generate_filter:
;
; Calculates spectral weighting factors used for resampling to CHRIS band setting
;********************************************************************************************
; TODO - implement in Java
Function generate_filter, wvl_M, wvl, wl_resol

num_wvl_M = n_elements(wvl_M)
num_wvl = n_elements(wvl)

s_norm_M = fltarr(num_wvl_M, num_wvl)
exp_max = 6.
exp_min = 2.
exp_arr = exp_max + (exp_min - exp_max) * findgen(num_wvl) / (num_wvl-1)
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

;********************************************************************************************
; read_LUT_CHRIS_formatted:
;
; Reads atmospheric LUT, stores it in memory and returns breakpoints and wvl array
;********************************************************************************************
PRO read_LUT_CHRIS_formatted, file_LUT, vza_arr, sza_arr, phi_arr, hsf_arr, aot_arr, cwv_arr, wvl

common lut_inp, lut1, lut2, num_par, num_bd, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

ndim = 6 ; vza, sza, hsf, aot, phi, cwv

read_var = 1
openr, 1, file_LUT
readu, 1, read_var
num_bd = read_var
wvl = fltarr(num_bd)
readu, 1, wvl

readu, 1, read_var
dim_vza = read_var
vza_arr = fltarr(dim_vza)
readu, 1, vza_arr

readu, 1, read_var
dim_sza = read_var
sza_arr = fltarr(dim_sza)
readu, 1, sza_arr

readu, 1, read_var
dim_hsf = read_var
hsf_arr = fltarr(dim_hsf)
readu, 1, hsf_arr

readu, 1, read_var
dim_aot = read_var
aot_arr = fltarr(dim_aot)
readu, 1, aot_arr

readu, 1, read_var
dim_phi = read_var
phi_arr = fltarr(dim_phi)
readu, 1, phi_arr

readu, 1, read_var
dim_cwv = read_var
cwv_arr = fltarr(dim_cwv)
readu, 1, cwv_arr

readu, 1, read_var
npar1 = read_var

readu, 1, read_var
npar2 = read_var

lut1 = fltarr(npar1, num_bd, 1, dim_phi, dim_aot, dim_hsf, dim_sza, dim_vza)
lut2 = fltarr(npar2, num_bd, dim_cwv, 1, dim_aot, dim_hsf, dim_sza, dim_vza)

readu, 1, lut1
readu, 1, lut2
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

;********************************************************************************************
; interpol_lut:
;
; Performs interpolation in the LUT for a given input vector (vza, sza, raa, elev, aot, cwv),
;   and returns matrix with spectral lpw, edir, edif, sab, t_rat
;********************************************************************************************
FUNCTION interpol_lut, avis, asol, phi, hsurf, aot, wv
common lut_inp, lut1, lut2, num_par, nm_bnd, xnodes, nm_nodes, ndim, lim, lut_cell, x_cell

;*** input vector
vtest = [avis, asol, hsurf, aot, phi, wv]

for i = 0, ndim-1 do begin
  wh = where(vtest[i] lt xnodes[i, *])
  lim[0, i] = wh[0] - 1
  lim[1, i] = wh[0]
endfor

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

; normalization of input vector
for i = 0, ndim - 1 do vtest[i] = (vtest[i] - xnodes[i, lim[0, i]]) / (xnodes[i, lim[1, i]] - xnodes[i, lim[0, i]])

; at this point: nodes with position x_cell and value lut_cell. Numbers from 0 to 63,
; in a binary order: 000000, 000001, 000010, ..., 111110, 111111 (0=inf, 1=sup)

;*** interpolation is build
; f(x,y) = V000000*f(x0,y0,z0,xx0,yy0,zz0)+V000001*f(x0,y0,z0,xx0,yy0,zz1)+...+V111111*f(x1,y1,z1,xx1,yy1,zz1)
; con V111111=(x-x0)(y-y0)(z-z0)(xx-xx0)(yy-yy0)(zz-zz0)

f_int = fltarr(num_par, nm_bnd)
for i = 0, nm_nodes - 1 do begin
  weight = abs(product(vtest - x_cell[*, nm_nodes - 1 -i]))
  for ind = 0, num_par - 1 do f_int[ind, *] = f_int[ind, *] + (weight * lut_cell[ind, i, *])
endfor

return, f_int

END

;********************************************************************************
;***
;***  varsol: IDL de 6S, da DSOL en forma de Richter
;***
;********************************************************************************
; TODO - implement in Java
PRO varsol, jday, month, dsol

if month gt 2 and month le 8 then J=31*(MONTH-1)-((MONTH-1)/2)-2+JDAY
if month le 2 then J=31*(MONTH-1)+JDAY
if month gt 8 then J=31*(MONTH-1)-((MONTH-2)/2)-2+JDAY
PI=2.*ACOS (0.)
OM=(.9856*FLOAT(J-4))*PI/180.
DSOL=1./((1.-.01673*COS(OM))^2)
DSOL = 1./DSOL^0.5
return
END

PRO gfunct, X, A, F, pder
  F = A[0] * X
  pder = X
END

FUNCTION ZBRENT, x1, x2, FUNC_NAME=func_name,    $
                         MAX_ITERATIONS=maxit, TOLERANCE=TOL
;+
; NAME:
;     ZBRENT
; PURPOSE:
;     Find the zero of a 1-D function up to specified tolerance.
; EXPLANTION:
;     This routine assumes that the function is known to have a zero.
;     Adapted from procedure of the same name in "Numerical Recipes" by
;     Press et al. (1992), Section 9.3
;
; CALLING:
;       x_zero = ZBRENT( x1, x2, FUNC_NAME="name", MaX_Iter=, Tolerance= )
;
; INPUTS:
;       x1, x2 = scalars, 2 points which bracket location of function zero,
;                                               that is, F(x1) < 0 < F(x2).
;       Note: computations are performed with
;       same precision (single/double) as the inputs and user supplied function.
;
; REQUIRED INPUT KEYWORD:
;       FUNC_NAME = function name (string)
;               Calling mechanism should be:  F = func_name( px )
;               where:  px = scalar independent variable, input.
;                       F = scalar value of function at px,
;                           should be same precision (single/double) as input.
;
; OPTIONAL INPUT KEYWORDS:
;       MAX_ITER = maximum allowed number iterations, default=100.
;       TOLERANCE = desired accuracy of minimum location, default = 1.e-3.
;
; OUTPUTS:
;       Returns the location of zero, with accuracy of specified tolerance.
;
; PROCEDURE:
;       Brent's method to find zero of a function by using bracketing,
;       bisection, and inverse quadratic interpolation,
;
; EXAMPLE:
;       Find the root of the COSINE function between 1. and 2.  radians
;
;        IDL> print, zbrent( 1, 2, FUNC = 'COS')
;
;       and the result will be !PI/2 within the specified tolerance
; MODIFICATION HISTORY:
;       Written, Frank Varosi NASA/GSFC 1992.
;       FV.1994, mod to check for single/double prec. and set zeps accordingly.
;       Converted to IDL V5.0   W. Landsman   September 1997
;       Use MACHAR() to define machine precision   W. Landsman September 2002
;-
        if N_params() LT 2 then begin
             print,'Syntax - result = ZBRENT( x1, x2, FUNC_NAME = ,'
             print,'                  [ MAX_ITER = , TOLERANCE = ])'
             return, -1
        endif

        if N_elements( TOL ) NE 1 then TOL = 1.e-3
        if N_elements( maxit ) NE 1 then maxit = 100

        if size(x1,/TNAME) EQ 'DOUBLE' OR size(x2,/TNAME) EQ 'DOUBLE' then begin
                xa = double( x1 )
                xb = double( x2 )
                zeps = (machar(/DOUBLE)).eps   ;machine epsilon in double.
          endif else begin
                xa = x1
                xb = x2
                zeps = (machar(/DOUBLE)).eps   ;machine epsilon, in single
           endelse

        fa = call_function( func_name, xa )
        fb = call_function( func_name, xb )
        fc = fb

        if (fb*fa GT 0) then begin
;                message,"root must be bracketed by the 2 inputs",/INFO
                return,xa
           endif

        for iter = 1,maxit do begin

                if (fb*fc GT 0) then begin
                        xc = xa
                        fc = fa
                        Din = xb - xa
                        Dold = Din
                   endif

                if (abs( fc ) LT abs( fb )) then begin
                        xa = xb   &   xb = xc   &   xc = xa
                        fa = fb   &   fb = fc   &   fc = fa
                   endif

                TOL1 = 0.5*TOL + 2*abs( xb ) * zeps     ;Convergence check
                xm = (xc - xb)/2.

                if (abs( xm ) LE TOL1) OR (fb EQ 0) then return,xb

                if (abs( Dold ) GE TOL1) AND (abs( fa ) GT abs( fb )) then begin

                        S = fb/fa       ;attempt inverse quadratic interpolation

                        if (xa EQ xc) then begin
                                p = 2 * xm * S
                                q = 1-S
                          endif else begin
                                T = fa/fc
                                R = fb/fc
                                p = S * (2*xm*T*(T-R) - (xb-xa)*(R-1) )
                                q = (T-1)*(R-1)*(S-1)
                           endelse

                        if (p GT 0) then q = -q
                        p = abs( p )
                        test = ( 3*xm*q - abs( q*TOL1 ) ) < abs( Dold*q )

                        if (2*p LT test)  then begin
                                Dold = Din              ;accept interpolation
                                Din = p/q
                          endif else begin
                                Din = xm                ;use bisection instead
                                Dold = xm
                           endelse

                  endif else begin

                        Din = xm    ;Bounds decreasing to slowly, use bisection
                        Dold = xm
                   endelse

                xa = xb
                fa = fb         ;evaluate new trial root.

                if (abs( Din ) GT TOL1) then xb = xb + Din $
                                        else xb = xb + TOL1 * (1-2*(xm LT 0))

                fb = call_function( func_name, xb )
          endfor

        message,"exceeded maximum number of iterations: "+strtrim(iter,2),/INFO

return, xb
END


;    output_string = filext(fname, path=path, name=name, ext=ext,
;    low=low, up=up)
;
; PARAMETERS:
;    fname    : string containing input filename
;
; KEYWORDS:
;    path     : path is selected from input filename
;    filpath  : path is selected without delimiter in the end
;    name     : name is selected from input filename (without extensions)
;    ext      : extensions are selected from input filename
;    ver      : version is selected from input filename (vms only)
;    low      : uppercase characters of the output string are
;               modified into lowercase characters
;    up       : lowercase characters of the output string are
;               modified into uppercase characters
;
; COMMON BLOCKS:
;    none
; PROCEDURE:
;    straight forward
; SIDE EFFECTS:
;    none
; RESTRICTIONS:
;    none
; COMMENTS:
;    none
; MODIFICATION HISTORY:
;    11/99  Beisl,DLR    Win/MAC file names may contain blanks
;
;-----------------------------------------------------------------------------

FUNCTION filext, fname, path=path, filpath = filpath, name=name, ext=ext, $
                 ver=ver, low=low, up=up

opsys = !version.os_family
case opsys of
  'Windows' : slash = '\'
  'vms'     : slash = ']'
  'MacOS'   : slash = ':'
  else      : slash = '/'
endcase

;Win/MAC file names may include spaces! Trim only leading and trailing blanks
IF opsys EQ 'Windows' OR opsys EQ 'MacOS' THEN fname = [strtrim(fname,2)] $
ELSE                                           fname = [strcompress(fname,/rem)]

nnam = n_elements(fname)

posi = intarr(4,nnam)
strout = strarr(nnam)

for inam = 0,nnam-1 do begin

  posi(0,inam) = -1
  repeat posi(0,inam) = posi(0,inam)+1 $   ;**** Searching position of last slash
        until min(strpos(fname(inam),slash,posi(0,inam))) eq -1

  posi(1,inam) = posi(0,inam)-1
  repeat posi(1,inam) = posi(1,inam)+1 $    $
                                ;**** Searching position of last '.'
        until min(strpos(fname(inam),'.',posi(1,inam))) eq -1

  posi(1,inam) = posi(1,inam)-1

  if opsys eq 'vms' then begin
    posi(2,inam) = posi(1,inam)
    repeat posi(2,inam) = posi(2,inam)+1 $    ;**** Searching position of ';'
        until min(strpos(fname(inam),';',posi(2,inam))) eq -1
    posi(3,inam) = strlen(fname(inam))
    posi(2,inam) = posi(2,inam)-1
  endif else posi(2,inam) = strlen(fname(inam))

  if posi(2,inam) eq posi(1,inam)   then posi(2,inam) = posi(3,inam)
  if posi(1,inam) eq posi(0,inam)-1 then posi(1,inam) = posi(2,inam)
  if posi(1,inam) eq -1 then posi(1,inam) = posi(2,inam)

; **** Definition of new output string ****

  if keyword_set(path) then strout(inam) = strout(inam) + $
                            strmid(fname(inam),           0,posi(0,inam))
  if keyword_set(filpath) then strout(inam) = strout(inam) + $
                            strmid(fname(inam),           0,posi(0,inam)-1)
  if keyword_set(name) then strout(inam) = strout(inam) + $
                            strmid(fname(inam),posi(0,inam),posi(1,inam)-posi(0,inam))
  if keyword_set(ext)  then strout(inam) = strout(inam) + $
                            strmid(fname(inam),posi(1,inam),posi(2,inam)-posi(1,inam))
  if keyword_set(ver)  then strout(inam) = strout(inam) + $
                            strmid(fname(inam),posi(2,inam),posi(3,inam)-posi(2,inam))
endfor

; **** depending on the keyword characters are modified into lower- or uppercase characters ****

if keyword_set(up)   then strout = strupcase (strout)
if keyword_set(low)  then strout = strlowcase(strout)

if nnam eq 1 then strout = strout(0)
if nnam eq 1 then fname = fname(0)

return, strout
end


;****************************

PRO CHRIS_read_dim, img_file_dim_arr, mode, ncols, nrows, fza_arr, vza_arr, vaa_arr, sza_arr, saa_arr, hsf_arr, month, jday, gmt, wl_center, wl_fwhm

n_img = n_elements(img_file_dim_arr)

fza_arr = fltarr(n_img)
vza_arr = fltarr(n_img)
vaa_arr = fltarr(n_img)
sza_arr = fltarr(n_img)
saa_arr = fltarr(n_img)
hsf_arr = fltarr(n_img)

par_arr = ['Calculated Image Centre Time', 'Fly-by Zenith Angle', 'Image Date', 'Number of Bands', 'Number of Ground Lines', 'Number of Samples', 'Observation Azimuth Angle', 'Observation Zenith Angle', 'Solar Zenith Angle', 'Solar Azimuth Angle', 'Target Altitude', 'Target Latitude', 'Target Longitude', 'CHRIS Mode']

n_par = n_elements(par_arr)

str_arr = strarr(n_par)

a = ''
openr, 1, img_file_dim_arr[0]
for ind_par = 0, n_par - 1 do begin
  repeat readf, 1, a until strpos(a, par_arr[ind_par]) ne -1 or eof(1) eq 1
  pos1 = strpos(a, '>')+1
  pos2 = strpos(a, '</')
  str_arr[ind_par] = strmid(a, pos1, pos2 - pos1)
  point_lun, 1, 0
endfor

dum_hhmm = strsplit(strmid(str_arr[0], 1, 8), ':', /extract)
hh = float(dum_hhmm[0])
mm = float(dum_hhmm[1])
ss = float(dum_hhmm[2])
gmt  = hh + mm / 60. + ss / 3600.
fza_arr[0] = float(str_arr[1])
mmdd = strsplit(str_arr[2], '-', /extract)
month = fix(mmdd[1])
jday  = fix(mmdd[2])
num_bd = fix(str_arr[3])
nrows = fix(str_arr[4])
ncols = fix(str_arr[5])
vaa_arr[0] = float(str_arr[6])
vza_arr[0] = float(str_arr[7])
sza_arr[0] = float(str_arr[8])
saa_arr[0] = float(str_arr[9])
hsf_arr[0] = float(str_arr[10])
lat = float(str_arr[11])
lon = float(str_arr[12])
mode = fix(str_arr[13])

wl_center = fltarr(num_bd)
wl_fwhm = fltarr(num_bd)


while(a ne '    <Image_Interpretation>' and EOF(1) eq 0) do readf, 1, a
for bd = 0, num_bd - 1 do begin

  for i  = 1, 8 do readf, 1, a

  readf, 1, a
  pos1 = strpos(a, '>')+1
  pos2 = strpos(a, '</')
  wl_center[bd] = float(strmid(a, pos1, pos2 - pos1))

  readf, 1, a
  pos1 = strpos(a, '>')+1
  pos2 = strpos(a, '</')
  wl_fwhm[bd] = float(strmid(a, pos1, pos2 - pos1))
  for i  = 1, 7 do readf, 1, a

endfor
close, 1

par_arr2 = ['Fly-by Zenith Angle', 'Observation Azimuth Angle', 'Observation Zenith Angle', 'Solar Zenith Angle', 'Solar Azimuth Angle', 'Target Altitude']
n_par2 = n_elements(par_arr2)
for ind_img = 1, n_img - 1 do begin

  openr, 1, img_file_dim_arr[ind_img]
  for ind_par = 0, n_par2 - 1 do begin
    repeat readf, 1, a until strpos(a, par_arr2[ind_par]) ne -1 or eof(1) eq 1
    pos1 = strpos(a, '>')+1
    pos2 = strpos(a, '</')
    str_arr[ind_par] = strmid(a, pos1, pos2 - pos1)
    point_lun, 1, 0
  endfor
  close, 1
  fza_arr[ind_img] = float(str_arr[0])
  vaa_arr[ind_img] = float(str_arr[1])
  vza_arr[ind_img] = float(str_arr[2])
  sza_arr[ind_img] = float(str_arr[3])
  saa_arr[ind_img] = float(str_arr[4])
  hsf_arr[ind_img] = float(str_arr[5])

endfor

hsf_arr = hsf_arr * 0.001

END