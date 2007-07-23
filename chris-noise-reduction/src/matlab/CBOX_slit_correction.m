function [X]=CBOX_slit_correction(X,T,mode)
% This function corrects the vertical striping (VS) due to the entrance slit.
% VS is obtained from a previous characterization (LUT) and
%  is corrected of its dependence on temperature.
% Obtained factors are used to correct the image column values.
%
% Inputs: 
%   - X: CHRIS image values
%   - T: temperature (C) of the given CHRIS acquisition
%   - mode: CHRIS acquisition mode. '1'=binned; {'2','3','4','5'}=unbinned;

% Vertical stripping (VS) 
VS=CBOX_slit_VS(T,mode);

% Correction of VS due to slit (constant in image columns for all the bands) 
[Nrow,Ncol,Nban]=size(X);
for b=1:Nban
  for c=1:Ncol  
    for r=1:Nrow
      X(r,c,b)=X(r,c,b)/VS(c);
    end
  end
end

return




