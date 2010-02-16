  ***************************************************************************************

  Notes on 'CHRIS_AtmCor_LUT_v2_BC.pro'
  Atmospheric correction module for CHRIS/PROBA data to be implemented in the BEAM toolbox

  Luis Guanter, GFZ-Potsdam, 7-May-2008

  ****************************************************************************************

  - The code is intended to process C/P data in the 5 different acquisition modes.

  - The available/requested view angles (1 to 5) are processed with a single call to the code.

  - The input interface is designed to ingest data in BEAM format (.dim), after the noise-reduction module. The handling of probabilistic cloud masks is also implemented, but not yet tested against BEAM outputs.

  - The ingestion of a DEM and geolocation information for elevation/topographic corrected is not considered in this version, as no feedback from the geometric correction module has been yet received. An extension of the code can be performed whenever the GC module is defined and operational.

  - Aerosol loading (aerosol optical thickness, AOT) is calculated from the data, but an input value can also be input by the user ('aot550_val'). The use of the aerosol retrieval mode by W. Gray to provide this value is suggested. The general processing chain would then benefit for the sequential application of AOD_retrieval -> Atmospheric_Correction.

  - A spectral polishing procedure can be launch for modes 1 and 5. It displays the results of the polishing in order to let the user confirm that the polishing must be performed. 

  - ENVI routines are only used for data I/O, so ENVI libraries can be avoided within BEAM.

  - For modes 1, 3 and 5 the user has to choose the "fast version", performing CWV retrieval for some pixels and applying the mean value for the processing of the complete image, or the "slow version", which performs CWV retrieval and surface reflectance pixel-wise for the complete image.

  - Outputs are currently reflectance images (scaled from 0-10000 for storage as 2byte integers) and water vapor maps for modes 1, 3 & 5 (under user request). Some sort of .log file with the summary of warnings and by-products should be generated. 

  - The user-inputs are (lines 53-60): 
 
     	SpecPol_flg: =1: no spectral polishig, =1: spectral polishing based on EM (dialog with the user, must accept or not in view of the results)

	ady_flg = 0 ; =1, adjacency correction

	aot550_val = 0 ; user-selected aot value; disables aot retrieval

	wv_val = 0 ; user-selected wv value, to be used as a guess in wv_retrieval (mode 1,5)

	wv_map_flg = 0 ; =0: no wv map generated in modes 1,3,5, but wv_mean is calculated from a subset of 50 pixels; =1, wv map is generated

	cld_rfl_thre = 0.05 ; probability threshold for REFL (& CWV) retrieval
