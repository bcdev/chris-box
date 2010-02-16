function [X,F]=CBOX_vertical_striping(X,M,mode);
% Vertical striping is due to errors inherent to the instrument.
% Thermal fluctuations cause small variations in the factors (multiplicative effect).
% Inputs:
%  X is a 3D matrix with the hyperspectral image
%  M is a mask with 0 in the useful values and >0 in masked values.
%   Size of X and M must be equal in 2D. 
%   We can consider that the factors are constant during one orbit. One can take advantage of this fact to improve 
%   the estimation of the vertical stripping by considering the five images as a single hyperspectral image, 
%   which is formed by stacking the five multiangular images (both X and M) in the along-track direction. 
% Outputs:
%  X corrected hyperspectral image
%  F multiplicative correction factors

%The proposed correction method is based on the hypothesis that the vertical disturbance presents higher spatial 
%frequencies than the surface radiance. It models the noise pattern by suppressing the surface contribution 
%in the across-track in two different ways: first, avoiding the high frequency changes due to surface (SAD-spatial filter), 
%and then subtracting the low frequency profile.

[Nrow,Ncol,Nban]=size(X); %size of the input image

%thresSAD: Predefined nominal SAD threshold for each CHRIS acquisition mode
%Npix: Predefined cutoff frequency for each CHRIS acquisition mode. {'1'}=binned; {'2','3','4','5'}=unbinned;
switch mode
  case {'1'}, thresSAD=0.08;  Npix=27; %Half spatial resolution, full swath, 62 bands for LAND
  case {'2'}, thresSAD=0.05;  Npix=27; %Full spatial resolution, full swath, 18 bands for WATER
  case {'3'}, thresSAD=0.08;  Npix=27; %Full spatial resolution, full swath, 18 bands for LAND/AEROSOLS
  case {'4'}, thresSAD=0.08;  Npix=27; %Full spatial resolution, full swath, 18 bands for CHLOROPHYLL 
  case {'5'}, thresSAD=0.08;  Npix=27; %Full spatial resolution, half swath, 37 bands for LAND
end

%SAM is a map whit the differences in the horizontal direction that is used to avoid edges (surface spectral-class transitions).
SAD=CBOX_sad(X);
%To estimate the VS the threshold should assure 60% min and 80% max of useful pixels in the worst column (column with more edges)  
%thresSAD=0; %predefined nominal SAD threshold (can be 0 to eliminate always 60% of pixels in the worst column)
useful_min=0.60; useful_max=0.80; %percentage of useful pixels
useful_min=fix(useful_min*Nrow); useful_max=fix(useful_max*Nrow); %position in rows
value_useful_min=0; value_useful_max=0;
for c=1:Ncol
 value = sort(SAD(:,c),'ascend');
 value_useful_min=max(value_useful_min,value(useful_min));
 value_useful_max=max(value_useful_max,value(useful_max));
 %thresSAD=max(thresSAD,max(min(thresSAD,value(useful_max)),value(useful_min)));
end
thresSAD=min(max(thresSAD,value_useful_min),value_useful_max);
%Edge mask is generated with the tuned threshold
edge=zeros(Nrow,Ncol);
for r=1:Nrow  
  for c=1:Ncol
    if SAD(r,c)>thresSAD
      edge(r,c)=1;
    end
  end
end    

%Vertical striping (Slit+CCD): multiplicative noise in columns constant in rows for each band: X(r,c,b)=I(r,c,b)*vs(b,c)

%The only way to remove the edges is to work in the across-track spatial derivative domain, 
%where the homogeneous areas before and after the edge present values close to zero and the spikes of edge pixels 
%can be substituted interpolating before doing the integration in the along-track direction. 
%In this simple way, all surface contribution of high frequency to the integrated line profile is removed before
%the low pass filtering, and the estimated VS is independent of the surface patterns.

%The correction is performed individually for each band
p=zeros(1,Ncol);    %across-track profile
F=zeros(Nban,Ncol); %multiplicative correction factors
for b=1:Nban
  useful=zeros(1,Ncol);
  for r=1:Nrow  
    for c=2:Ncol
      %1. To apply logarithms in order to transform the multiplicative noise in additive noise
      %   lX = log(X(c)) = log(I(c)) + log(vs(c))
      %2. To transform the hyperspectral data-cube into the across-track spatial derivative domain,
      %   which is equivalent to high-pass filtering:
      %   dlog(X(c))/dc = I'(c)/I(c) + vs'(c)/vs(c)
      %   Derivate: dX/dc -> y(c)=y(c)-x(c-1) where y(c=1)=0 
      %3. The lines of each band are averaged in the along-track direction but avoiding the
      %   masked and the edge pixels found with the spatio-spectral edge detection:
      if edge(r,c)==0 & M(r,c,b)==0 & M(r,c-1,b)==0 
        p(c) = p(c) + (log(X(r,c,b))-log(X(r,c-1,b)));
        useful(c)=useful(c)+1;
      end
    end
  end
  %To obtain the average (mean) in the along-track direction we had to divide by the number of useful pixels per column
  for c=2:Ncol
    if useful(c)>0, 
      p(c)=p(c)/useful(c);
    else %if there are not useful pixels in the column we assume small surface changes between columns
      p(c)=p(c-1); 
    end     
  end
  %4. Integration in the across-track direction (cumulative sum in columns), which is equivalent to low-pass filtering:
  %   lVS = Int{dlX} = Int{vs'(c)/vs(c)} = log(vs(c)) + cte; 
  %   Integral: Int{x dc} -> y(c+1)=y(c)+x(c+1) where y(c=1)=x(c=1) 
  for c=2:Ncol
    p(c)=p(c-1)+p(c);
  end
  %5. To apply a LPF in the across-track direction in order to eliminate the high frequency
  %   variations (coming from the noise) and estimate the surface contribution:
  %   Int{dlX} = Int{I'(c)/I(c)} + Int{vs'(c)/vs(c)}= log(I(c)) + log(vs(c)) + cte; 
  %   lVS = Int{dlX} - LPF{Int{dlX}}
  s = smooth(p,Npix,'rloess')';  
  %6. To obtain the high frequency variations (considered as the noise) by subtracting the low frequencies
  pmean=0;
  for c=1:Ncol
    p(c)=p(c)-s(c); %lVS=lX-lI
    pmean=pmean+p(c);
  end
  % The error committed during the integration process consists in a constant value for each band. 
  % As vertical stripping is corrected independently for each band, the vertical striping in the logarithmic
  % domain should present zero mean (gain close to 1 in the radiance image).
  % Therefore, the offset errors are corrected subtracting the mean value:
  p=p-pmean/Ncol;
  %7. VS factors are obtained calculating the inverse of the logarithm: vs(n) = exp{log(vs(n))}
  % Finally, all the lines of each spectral band are corrected with the estimated correction factors I(r,c,b)=X(r,c,b)/vs(b,c)
  for c=1:Ncol
    F(b,c)=1/exp(p(c)); %multiplicative correction factor
    for r=1:Nrow  
      X(r,c,b)=X(r,c,b)*F(b,c);
    end
  end
end


return

