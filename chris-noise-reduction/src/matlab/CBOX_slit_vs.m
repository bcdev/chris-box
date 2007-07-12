function VS=CBOX_slit_VS(T,mode)
% This function provides an estimation of the vertical stripping (VS) affecting a given CHRIS image.
% The VS is obtained from a characterization of the vertical stripping pattern (LUT) and
%  this curve is corrected of its dependence on temperature: 
%   - Shift in columns as a function of temperature (linear regression). 
%   - Change in factor values as a function of temperature (linear regression). 
%
% Inputs: 
%   - T: temperature (C) of the given CHRIS acquisition
%   - mode: CHRIS acquisition mode. '1'=binned; {'2','3','4','5'}=unbinned;
%
% The characterization of the vertical stripping was obtained by correcting 24 Mode2 datasets 
%  with the method proposed in:
%   “Modelling spatial and spectral systematic noise patterns on CHRIS/PROBA hyperspectral data” 
%    L. Gómez-Chova, L. Alonso, L. Guanter, G. Camps-Valls, J. Calpe, and J. Moreno. 
%    Image and Signal Processing for Remote Sensing XII, edited by Lorenzo Bruzzone, 
%    Proceedings of SPIE Vol. 6365, 63650Z (12 pages), 2006. 

%Vertical stripping pattern at subpixel resolution (@T=5.51C)
%LUT reading 
fid=fopen('vertical_striping_lut.txt', 'r'); 
[header] = fscanf(fid,'%s',[2]); 
[LUT] = fscanf(fid,'%f',[2 148707]); 
fclose(fid);
VS_pixels=LUT(1,:);  %CCD pixels
VS_mean=LUT(2,:);    %VS(Tref)

%Dependence on Temperature (T)
%Parameters of the linear regression (y=m*x+n)
P2VSg =[0.13045510094294   0.28135856882126];  %Change in factor values as a function of T 
P2VSs =[-0.12107994955864   0.65034734426230]; %Shift in columns as a function of T
VS_gain=P2VSg(1)*T+P2VSg(2);  %Gain factor (Change in factor values as a function of T)
VS_shift=P2VSs(1)*T+P2VSs(2); %CCD pixels (Shift in columns as a function of T)

%[T,VS_gain,VS_shift]

%Correction of VS dependence on temperature
VS_mean=(VS_mean-1)*VS_gain+1; %VS_gain=VS(T)/VS(Tref);
VS_pixels=VS_pixels-VS_shift;  %CCD pixels

%CHRIS acquisition mode. {'1'}=binned; {'2','3','4','5'}=unbinned;
switch mode
  case {'1'}
     ppc=2; %pixel per column (we will combine values of contiguous CCD columns)
     col_ini=2+2+2+1;
     %Ncol=374;
     Ncol=372;  
  case {'2','3','4'}
     ppc=1; %pixel per column
     col_ini=4+4+4+1;
     Ncol=744;
  case '5'
     ppc=1; %pixel per column
     col_ini=4+4+4+1;
     Ncol=370;
end

%Average of the VS values between image pixel limits
VS=zeros(1,Ncol);
imax=length(VS_pixels);
c=1; n=0;
for i=1:imax    
  if c<=Ncol
    if (VS_pixels(i)>0.5) & (VS_pixels(i)<(c*ppc+0.5)) %values between pixel limits 
      VS(c)=VS(c)+VS_mean(i); n=n+1; %sum of the values between pixel limits
    end
    if i==imax %last sample of the modeled VS (LUT)
      VS(c)=VS(c)/n;                 %average of the values between pixel limits
    elseif (VS_pixels(i+1)>=(c*ppc+0.5))   %limit of the pixel
      VS(c)=VS(c)/n;                 %average of the values between pixel limits
      c=c+1; n=0; %prepare counters to the next pixel column
    end 
  end
end

return
