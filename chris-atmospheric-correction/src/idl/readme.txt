  ***************************************************************************************

  Notes on 'CHRIS_AtmCor_LUT_beam_noDEM.pro'
  Atmospheric correction module for CHRIS/PROBA data to be implemented in the BEAM toolbox

  Luis Guanter, GFZ-Potsdam, 21-Feb-2008

  ****************************************************************************************

  - The code is intended to process C/P data in the 5 different acquisition modes.

  - The available/requested view angles (1 to 5) are processed with a single call to the code.

  - The input interface is designed to ingest data in BEAM format (.dim), after the noise-reduction module. The handling of probabilistic cloud masks is also implemented, but not yet tested against BEAM outputs.

  - The ingestion of a DEM and geolocation information for elevation/topographic corrected is not considered in this version, as no feedback from the geometric correction module has been yet received. An extension of the code can be performed whenever the GC module is defined and operational.

  - Aerosol loading (aerosol optical thickness, AOT) is calculated from the data, but an input value can also be input by the user ('aot550_val'). The use of the aerosol retrieval mode by W. Gray to provide this value is suggested. The general processing chain would then benefit for the sequential application of AOD_retrieval -> Atmospheric_Correction.

  - A spectral polishing procedure can be launch for modes 1 and 5. It displays the results of the polishing in order to let the user confirm that the polishing must be performed. 

  - ENVI routines are only used for data I/O, so ENVI libraries can be avoided within BEAM.

  - Outputs are currently reflectance images (scaled from 0-10000 for storage as 2byte integers) and water vapor maps for modes 1 & 5. The inclusion of other by-products/information is to be discussed. 
 
