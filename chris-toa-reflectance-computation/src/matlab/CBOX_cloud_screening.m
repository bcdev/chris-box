function CBOX_cloud_screening(archivo)
     
     
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CHRIS READER AND NOISE REDUCTION TOOL 
%  Inputs:  HDF file
%
%Already implemented in VISAT

 [pathname,nombre,ext,vers_so] = fileparts(archivo);
 pathres=['result1' filesep nombre filesep];
 mkdir(pathres); 
      
 if any([findstr('.hdf',archivo) findstr('.zip',archivo)]) % HDF file
   [X,keywords_hdf,nombre,M_mask]=CHRIS2matlab(archivo,0);
   WlMid=keywords_hdf.WlMid; 
   BWidth=keywords_hdf.BWidth;
   Mode=keywords_hdf.Mode; 
   map_info=keywords_hdf.map_info; 
   ImageDate=keywords_hdf.ImageDate; 
   SolarZenithAngle=keywords_hdf.SolarZenithAngle;
   FZA=keywords_hdf.FZA; 
   ObservationZenithAngle=keywords_hdf.ObservationZenithAngle;
 else                                                 % ENVI file
   [X,keywords,nombre]=envi2matlab(archivo,0);
   WlMid=str2num(char(keywords.wavelength.value{:}))';
   BWidth=str2num(char(keywords.fwhm.value{:}))';
   map_info=keywords.map_info.value;
   eval(keywords.description.value{1});
   SolarZenithAngle; ObservationZenithAngle; ImageDate;
   Mode=num2str(Mode); FZA=num2str(FZA);
 end  
      
      
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% TOP-OF-ATMOSPHERE REFLECTANCE
%  Inputs: CHRIS product (corrected of noises)
%          WlMid,BWidth (wavelength and bandwidth of CHRIS channels)
%          Metadata (ImageDate, SolarZenithAngle)
%          Sun irradiance taken from Thuillier et al.
%
% The cloud screening module requires as input the CHRIS image corrected of noises.
% This corrected radiance must be pre-processed in order to estimate TOA
% reflectance. This allows us to remove in practice the dependence on particular illumination
% conditions (day of the year and angular configuration), since the method is intended to work
% under many situations.
      
 [Nrow,Ncol,Nban]=size(X); %size of the input image
      
 %Sun irradiance taken from Thuillier et al.
 [S_w_hr,S_irr_hr]=textread('Thuillier.prn','%f %f','headerlines',1);
 %It shall be corrected for the day of year (J)
 Y=str2num(ImageDate(1:4)); M=str2num(ImageDate(6:7)); D=str2num(ImageDate(9:10));
 J = datenum(Y,M,D)-datenum(Y,1,1)+1; % Julian day-of-year (DOY)
 %Earth-Sun Distance correction Factor
 e=0.01673; %Earth's orbit eccentricity
 factor=1./(1-e*cos(0.9856*(J-4)*pi/180))^2; %Vermonte97
 S_irr_hr=S_irr_hr*factor;
 %Convolution 
 S_irr=conv_spectral_channels(S_w_hr,S_irr_hr,WlMid,BWidth);
 %Thuillier Solar Irradiance stored in mW*m-2*nm-1. CHRIS radiance in uW*m-2*nm-1
 S_irr=S_irr*1000; 
      
 % TOA Apparent Reflectance
 mu=cos(SolarZenithAngle/180*pi);
 for i=1:Nban
   X(:,:,i)= (pi/mu/S_irr(i))*X(:,:,i);
 end  
 medida='TOA Apparent Reflectance';
            
 % Save TOA Reflectance Image (intermediate product)
 if 1
   keywords=crear_keywords; 
   keywords=add_keyword(keywords,'description',['''' medida ' of CHRIS/PROBA''; Mode=' Mode '; FZA=' FZA '; Imagename=''' nombre '''; SolarZenithAngle=' num2str(SolarZenithAngle) '; ObservationZenithAngle=' num2str(ObservationZenithAngle) '; ImageDate=''' ImageDate ''';']);
   keywords=add_keyword(keywords,'map_info',map_info); keywords=add_keyword(keywords,'wavelength',WlMid); keywords=add_keyword(keywords,'fwhm',BWidth);
   matlab2envi(X,[pathres nombre '_toa_ref.bsq'],keywords,'NoImClas');
 end  
      
      
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% FEATURE EXTRACTION FOR CLOUD SCREENING
%  Inputs: CHRIS TOA Reflectance 
%          WlMid,BWidth (wavelength and bandwidth of CHRIS channels)
%          Metadata (SolarZenithAngle,ObservationZenithAngle)
%          Effective atmospheric vertical transmittance (estimated from a high resolution curve)
%
% At this step, rather than working with the spectral bands only, physically-inspired features
% are extracted in order to increase the separability of clouds and surface covers. These
% features are extracted independently from the bands that are free from strong gaseous absorptions,
% and from the bands affected by the atmosphere.
      
 %Band Selection
 bandas=1:Nban;
 bands_rm=[];
 bands_rm=[bands_rm find( WlMid>590 & WlMid<600)];  % 590-600  nm - low atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>630 & WlMid<636)];  % 630-636  nm - low atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>648 & WlMid<658)];  % 648-658  nm - low atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>686 & WlMid<709)];  % 686-709  nm - low atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>716 & WlMid<741)];  % 716-741  nm - low atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>756 & WlMid<775)];  % 756-775  nm - O2 atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>792 & WlMid<799)];  % 792-799  nm - low atmospheric absorption    
 bands_rm=[bands_rm find( WlMid>808 & WlMid<840)];  % 808-840  nm - H20 atmospheric absorption 
 bands_rm=[bands_rm find( WlMid>885 & WlMid<985)];  % 885-985  nm - H20 atmospheric absorption   
 bands_rm=[bands_rm find( WlMid>400 & WlMid<440)];  % 400-440  nm - sensor noise
 bands_rm=[bands_rm find( WlMid>985 & WlMid<1010)]; % 985-1010 nm - calibration errors
 bandas=setdiff(bandas,bands_rm);
 bandasVIS=find( WlMid>400 & WlMid<700 ); 
 bandasNIR=setdiff(bandas,bandasVIS); 
 bandasVIS=setdiff(bandas,bandasNIR);  
      
 % Surface reflectance features      
 Deriv=spectralfeatures(X(:,:,bandas),WlMid(bandas),{'whitedif'});            
 Intens=spectralfeatures(X(:,:,bandas),WlMid(bandas),{'integral'});
 DerivVIS=spectralfeatures(X(:,:,bandasVIS),WlMid(bandasVIS),{'whitedif'});
 IntensVIS=spectralfeatures(X(:,:,bandasVIS),WlMid(bandasVIS),{'integral'});
 DerivNIR=spectralfeatures(X(:,:,bandasNIR),WlMid(bandasNIR),{'whitedif'});
 IntensNIR=spectralfeatures(X(:,:,bandasNIR),WlMid(bandasNIR),{'integral'});
      
 %Atmospheric absorptions     
 % m=1/mu=1/cos(illum)+1/cos(obs): Optical mass 
 if exist('ObservationZenithAngle'), mu=1/(1/cos(SolarZenithAngle/180*pi)+1/cos(ObservationZenithAngle/180*pi));
 else, mu=1/(1/cos(SolarZenithAngle/180*pi)); end
 %O2 atmospheric absorption
 W_out_inf=[738 755]; W_in=[755 770]; W_out_sup=[770 788];  W_max=[760.625]; %O2
 OP_O2=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);
 %H2O atmospheric absorption 
 W_out_inf=[865 890]; W_in=[895 960]; W_out_sup=[985 1100]; W_max=[944.376]; %H2O
 OP_H2O=mu*optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max);
      
 % Save Features (intermediate product)
 if 1  
   keywords_feat=crear_keywords;
   keywords_feat=add_keyword(keywords_feat,'description',['Features of CHRIS/PROBA Mode' Mode ' FZA' FZA ' ' nombre]);
   keywords_feat=add_keyword(keywords_feat,'map_info',map_info);
   keywords_feat=add_keyword(keywords_feat,'band names',{'Intens','Deriv','IntensVIS','DerivVIS','IntensNIR','DerivNIR','O2Absorp','H2OAbsorp'});
   matlab2envi(cat(3,Intens,Deriv,IntensVIS,DerivVIS,IntensNIR,DerivNIR,OP_O2,OP_H2O),[pathres nombre '_feat.bsq'],keywords_feat,'NoImClas');
 end  
      
      
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CLUSTERING OF FEATURES
%  Inputs: Extracted Features
%          Metadata (Mode)
%          Number of clusters
%
% Static thresholds applied to every pixel in the image can fail due to subpixel clouds, 
% sensor calibration, variation of the spectral response of clouds with cloud type and height, etc.
% In this context, the following step in our methodology considers the use of unsupervised 
% classification methods to find groups of similar pixels in the image.
%
% A probabilistic clustering algorithm is already being implemented for VISAT

 % Selection of features (can be selected by the user or using default sets for each CHRIS Mode)
 switch Mode
  case '1', xx=[IntensVIS(:),DerivVIS(:),IntensNIR(:),DerivNIR(:),OP_H2O(:)]; %samples in rows; features in columns
  case '2', xx=[IntensVIS(:),DerivVIS(:),IntensNIR(:),DerivNIR(:)]; %samples in rows; features in columns
  case '3', xx=[IntensVIS(:),DerivVIS(:),IntensNIR(:),DerivNIR(:)]; %samples in rows; features in columns
  case '4', xx=[IntensVIS(:),DerivVIS(:),IntensNIR(:),DerivNIR(:)]; %samples in rows; features in columns
  case '5', xx=[IntensVIS(:),DerivVIS(:),IntensNIR(:),DerivNIR(:),OP_H2O(:)]; %samples in rows; features in columns
 end

 % Region of Interest selection
 %Accurate ROI selection for all CHRIS Modes is not possible. For current version we work with the whole image. 
 %This step and references to 'pos_ok' could be removed to simplify the code.
 CoI_area=ones(Nrow,Ncol); 
 if exist('CoI_area'), 
   pos_ok=find(CoI_area(:)); 
   xx=xx(pos_ok,:);
 else, 
   pos_ok=1:Nrow*Ncol;  
 end

 %Data normalization [0,1] 
 [Ndata,Nfeat]=size(xx);
 for f=1:Nfeat
  norm_off=min(xx(:,f)); 
  norm_fac=max(xx(:,f))-norm_off;  
  norm_fac=norm_fac+(norm_fac==0); 
  xx(:,f) = ( xx(:,f) - norm_off ) ./ norm_fac; 
 end   
    
 %EM-Gaussian Maximum Likelihood (already implemented)
 Nclus=14; %Number of clusters
 gmm_mix = gmm(Nfeat, Nclus,'full');
 options(1)  = 0; options(14) = 30; options(5) = 1;
 gmm_mix = gmminit(gmm_mix, xx, options);
 gmm_mix = gmm_em(gmm_mix, xx, options);
 %Posterior probabilities
 post_prob = gmmpost(gmm_mix, xx);
 %Each pixel is assigned to the cluster with maximum posterior probability (pixels outide the ROI assigned to 0)
 [valor,w_k] = max(post_prob,[],2);
 W_k=zeros(1,Nrow*Ncol); 
 W_k(pos_ok)=w_k;
 ImClas=reshape(W_k,Nrow,Ncol);
 clusterlabel=unique(w_k)';

 figure; imagesc(ImClas); colorbar, title('Cluster Map'), 

 %Save Cluster Map (intermediate product)
 if 1
   keywords_class=crear_keywords; %keywords=add_keyword(keywords,keyword,values);
   keywords_class=add_keyword(keywords_class,'description',['''Clusters of CHRIS/PROBA''; Mode=' Mode '; FZA=' FZA '; Imagename=''' nombre '''; SolarZenithAngle=' num2str(SolarZenithAngle) '; ObservationZenithAngle=' num2str(ObservationZenithAngle) '; ImageDate=''' ImageDate ''';']);
   keywords_class=add_keyword(keywords_class,'map_info',map_info);
   keywords_class=add_keyword(keywords_class,'band names',['Image segmentation']); 
   keywords_class=add_keyword(keywords_class,'class names',cellstr(num2str(clusterlabel'))); 
   keywords_class.class_lookup.value=cellstr(num2str(reshape(fix(255*jet(Nclus+1)'),[],1)));
   matlab2envi(ImClas,[pathres nombre '_clus.bsq'],keywords_class,numel(clusterlabel));
 end
        
              
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CLUSTER SPECTRA 
%  Inputs: CHRIS TOA Reflectance 
%          Posterior probabilities
%
% Once clusters are determined in the previous step, the spectral signature of each cluster,
% is estimated as the average of the spectra of the cluster pixels. This step excludes
% those pixels with abnormally low membership values or posterior probability.

 x=reshape(X,Nrow*Ncol,Nban); %samples in rows and spectral bands in columns
 for i=clusterlabel
   mask_tmp=find(post_prob(:,i)>0.5);
   mask_tmp_ok=pos_ok(mask_tmp);  
     if isempty(mask_tmp_ok)
       esp_centres(i,1:size(X,3))=zeros(1,Nban);
     else
       esp_centres(i,1:size(X,3))=mean(x(mask_tmp_ok,:));
     end
 end

 figure; hplot=plot(WlMid,esp_centres); title('Cluster spectra'), xlabel('wavelength (nm)'), ylabel(medida), 
 legend(strcat(cellstr(num2str(clusterlabel')),': ',cellstr(num2str(esp_centres(clusterlabel,1),'%0.2g')),' [',cellstr(num2str(histc(ImClas(pos_ok),clusterlabel))),']'));


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CLUSTER LABELING
%  Inputs: CHRIS TOA Reflectance 
%          Extracted Features
%          Cluster Map
%          Spectral signature of each cluster
%
% Obtained clusters are labeled by the user into geo-physical classes taking into account: 
% - the thematic map with the distribution of the cluster in the scene 
% - the spectral signature of clusters
% - pixels with the closest spectrum to the spectral signature of clusters
% - image view of reflectance
% - false color image of the extracted features
%
% Implementation of this interactive tool should take advantage of VISAT Tools: image view, pins, spectrum, etc 
% (To be discussed in next meeting)
%
% ONLY IS NECESARY TO DISTINGUISH BETWEEN 'CLOUD' AND 'CLOUD-FREE', but offering more detailed class labels 
% can be useful for the user in further processing steps.
   
 classlabel={'background','bright clouds','clouds','cirrus','shadows','vegetation','soil','water','ice/snow'};
 Nlabels=length(classlabel); 
 clusterclass=zeros(1,Nclus);
 answer = inputdlg(classlabel,'Assign each cluster number to a cover type',ones(Nlabels,1));  
 clusterclass(str2num(char(answer(1))))=0; %background
 for i=2:Nlabels, 
   classclusters{i-1}=str2num(char(answer(i))); 
   clusterclass(str2num(char(answer(i))))=i-1; 
 end


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% CLOUD PROBABILITY
%  Inputs: Posterior probabilities
%          Cluster-class labels
%
% During the labeling process, it is possible to reject a given cluster if it contains pixels
% corresponding to both clouds and ground covers. In this case, after removing this components
% from the mixture of normal distributions, we perform again a MAP classification
% on the whole image to obtain the final cluster membership for each pixel in the image
% and its corresponding cluster label.
%
% Once all clusters have been related to a class with a geo-physical meaning, it is
% straightforward to merge all the clusters belonging to a cloud type. 
% A probabilistic cloud index, based on the clustering of the extracted features, can be computed 
% as the sum of the posteriors of the cloud-clusters.
        
 %Clusters identified as 'BACKGROUND' (e.g., outliers or mixtures of clouds and land covers) can be optionally rejected
 del=find(clusterclass==0); %'BACKGROUND'
 if ~isempty(del) %any(clusterclass==0), 
  %Wrong clusters are deleted
  Nclusdel=numel(del);
  Nclus=Nclus-Nclusdel;
  clusterclass(del)=[];	
  esp_centres(del,:)=[];
  %Gaussians of wrong clusters are deleted
  gmm_mix.ncentres=Nclus; gmm_mix.priors(del)=[]; gmm_mix.centres(del,:)=[]; gmm_mix.covars(:,:,del)=[];		
  %Probabilities are recomputed for the final set of clusters
  post_prob = gmmpost(gmm_mix, xx);
  [valor,w_k] = max(post_prob,[],2);
  W_k=zeros(1,Nrow*Ncol); 
  W_k(pos_ok)=w_k;
  ImClas=reshape(W_k,Nrow,Ncol);
 end
    
 %Posterior Probability Maps for each cluster 
 Xwk=zeros(Nrow*Ncol,Nclus); 
 Xwk(pos_ok,:)=post_prob; 
 Xwk=reshape(Xwk,[Nrow Ncol Nclus]);
 %Clusters labeled as cloud ('bright clouds','clouds','cirrus')
 CoI_clusters=find(esp_centres(:,1)>0.2)'  %example of a default automatic labeling
 CoI_clusters=find(ismember(clusterclass,[1 2 3]))  %labeled as cloud by the user 
 %Cloud Probability
 CoI_probability=sum(Xwk(:,:,CoI_clusters),3);
 
 h=figure; imagesc(CoI_probability), colorbar, axis equal, axis off, title('Cloud Probability')
    

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% FULLY CONSTRAINED LINEAR SPECTRAL UNMIXING (LSU) AND CLOUD ABUNDANCE 
%  Inputs: CHRIS TOA Reflectance 
%          Spectral signature of each cluster
%          Cluster-class labels
%
% In order to obtain a cloud abundance map for every pixel in the image, rather than flags
% or a binary classification, a spectral unmixing algorithm is applied to the multispectral image.  
%
% A fully constrained linear spectral unmixing algorithm is already implemented in VISAT

 % If user is not interested in a continued valued cloud abundance we can avoid the time-consuming LSU 
 op_unmixing     =1;     

 if ~op_unmixing
      
   % We substitute the cloud abundance by a binary cloud mask based on posterior probabilities
   CoI_abundance=CoI_probability>=0.5; 
      
 else %FULLY CONSTRAINED LINEAR SPECTRAL UNMIXING
      
   %Endmembers Selection
   % Only one endmember must be selected to represent clouds: 
   % we select the most bright and white endmember (maximum brightness/whiteness)
   [valor,indice]=max( spectralfeatures(esp_centres(CoI_clusters,bandasVIS),WlMid(bandasVIS),{'integral'}) ./ ...
      spectralfeatures(esp_centres(CoI_clusters,bandas),WlMid(bandas),{'whitedif'}) );
   esp_cloud=esp_centres(CoI_clusters(indice),:);
   %There are different approaches to determine the spectra of the different pure constituents in the image.
   %Once the cloud-endmember is selected, we use ATGP to select the rest of endmembers.
   %A faster (but less accurate option) is to select as endmembers the spectral signatures of found cluster
   enmembers=esp_centres;
   enmembers(CoI_clusters,:)=[];
   enmembers=[esp_cloud;enmembers];
      
   %SPECTRAL UNMIXING 
   % After the endmember selection, we apply the FCLSU to the image using all the available
   % spectral bands except bands particularly affected by atmospheric absorptions,
   % since the linear mixing assumption is not appropriate at those bands.
   [Abundances,RMSE]=FCLSU(X(:,:,bandas),enmembers(:,bandas)',0.0001/max(enmembers(:))); %already implemented
   CoI_abundance=Abundances(:,:,1); %the cloud endmember was the firs one
      
 end  
  
      
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%FINAL CLOUD PRODUCT
%  Inputs: Cloud Probability
%          Cloud Abundance 
%
% An improved cloud product map can be obtained when combining the Cloud Abundance
% and the Cloud Probability by means of a pixel-by-pixel multiplication

 CoI_result=CoI_abundance.*CoI_probability;
 h=figure; imagesc(CoI_result), colorbar, axis equal, axis off, title('Cloud Abundance')
      
 % Save cloud products
 if 1  
   keywords_coi=crear_keywords;
   keywords_coi=add_keyword(keywords_coi,'description',['Cloud Product of CHRIS/PROBA Mode' Mode ' FZA' FZA ' ' nombre]);
   keywords_coi=add_keyword(keywords_coi,'map_info',map_info);
   keywords_coi=add_keyword(keywords_coi,'band names',{'Cloud Product','Probability','Abundance'});
   matlab2envi(cat(3,CoI_result,CoI_probability,CoI_abundance),[pathres nombre '_coi.bsq'],keywords_coi,'NoImClas');
 end  
      
     
return

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [conv_spectrum,conv_wavelength]=conv_spectral_channels(wavelength,spectrum,WlMid,BWidth) 
%This function convolves the values of an spectral curve (spectrum,wavelength)
% by the spectral channels efficiency defined by (WlMid,BWidth)
%  spectrum,wavelength: high spectral resolution curve
%  WlMid,BWidth: wavelength and bandwidth of sensor channels

if length(wavelength)==size(spectrum,1)
  spectrum=spectrum'; %spectra in rows; wavelength in columns
end
k=0;
a = 4;    % Peaky-parameter.
lims=1;   %limits of the integral (conv by the window) in terms of % of BWidth
for i= 1:length(WlMid)
   % spectral range
   pos=find( (wavelength>(WlMid(i)-lims*BWidth(i))) & (wavelength<(WlMid(i)+lims*BWidth(i))) ); 
   points = wavelength(pos);
   % Bell-shaped window
   window = 1./(1+ abs(2*(points-WlMid(i))/BWidth(i)).^a);
   % convolution
   if ~isempty(window)
     k=k+1;
     conv_spectrum(:,k)=(spectrum(:,pos) * window) / sum(window); 
     conv_wavelength(k)=WlMid(i);
   end
end

return

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function y=spectralfeatures(x,w,feat_name)
% Computes features from the spectrum in x 
% x: hypercube or dataset (samples in rows) containing the spectra
% w (optional): wavelenght of each band 
% bands (optional): only uses indicated bands 
% feat_name: cellstr with the features to compute
%   'integral'
%      Spectral Integral: trapezoidal numerical integration (same units than x)
%      if w is uniform is like the norm
%   'whitedif'
%      Integral of the difference with white (flat cte spectrum, vector of ones)

%x with samples in rows x(1:ndata,1:ndims)
if ndims(x)==3 %3D image
  [NF,NC,ND]=size(x);
  x=reshape(x,NF*NC,ND);
  ndata=NF*NC;
  key_imagen=1;
elseif ndims(x)==2 %samples in rows
  [ndata,ND]=size(x);
  key_imagen=0;
else
  error('Wrong Input');
end

wRange=max(w)-min(w);
N=length(w);

switch char(feat_name)
 case {'integral'} %Spectral Integral (trapezoidal numerical integration), 
   y=trapz(w,x,2)/wRange;
 case {'whitedif'} %Integral of the difference with white (flat cte spectrum, vector of ones)
   norma=sqrt(sum(x.^2,2)); 
   normadiv=norma; 
   normadiv(find(norma==0))=1;
   y=trapz(w,abs((x./normadiv(:,ones(1,N)))-1/sqrt(N)),2)/wRange;
end     

if key_imagen
  y=reshape(y,NF,NC);
end

return

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function OP=optical_path(X,WlMid,BWidth,W_out_inf,W_in,W_out_sup,W_max)
% Estimation of the optical path from an atmospheric absorption band. 
% Note: contribution of illumination and observation geometries is not normalized (OP=mu*OP)
% Inputs: 
%   - X: CHRIS image values
%   - WlMid,BWidth: wavelength and bandwidth of CHRIS channels
%   - W_out_inf,W_in,W_out_sup,W_max: spectral channels located at the absorption band 
%                                     (outside and inside the absorption)

b_out_inf = find((WlMid-BWidth/2)>=W_out_inf(1) & (WlMid+BWidth/2)<=W_out_inf(2));
b_in      = find((WlMid-BWidth/2)>W_in(1) & (WlMid+BWidth/2)<W_in(2)); 
b_out_sup = find((WlMid-BWidth/2)>=W_out_sup(1) & (WlMid+BWidth/2)<=W_out_sup(2));
if ~isempty(b_in)
  b_in=find( abs(WlMid-W_max) == min(abs(WlMid(b_in)-W_max)) );
  %Effective atmospheric vertical transmittance, exp(-tau) estimated from a high resolution curve
  [ATM_w_hr,ATM_trans_hr]=textread('TOA_trans_NIR_hi.txt','%f %f','headerlines',1);
  ATM_trans=conv_spectral_channels(ATM_w_hr,ATM_trans_hr,WlMid(b_in),BWidth(b_in));
  %Interpolated spectrum at the absorption band is estimated from nearby channels
  if ~isempty(b_out_inf) & ~isempty(b_out_sup)
   L_out_inf=mean(X(:,:,b_out_inf),3); w_out_inf=mean(WlMid(b_out_inf));
   L_out_sup=mean(X(:,:,b_out_sup),3); w_out_sup=mean(WlMid(b_out_sup));
   L0=L_out_inf+((WlMid(b_in)-w_out_inf)/(w_out_sup-w_out_inf))*(L_out_sup-L_out_inf);
  elseif ~isempty(b_out_inf)
    L0=mean(X(:,:,b_out_inf),3);
  elseif ~isempty(b_out_sup)
    L0=mean(X(:,:,b_out_sup),3);
  end
  %Estimation of the optical path from an atmospheric absorption band. 
  %note: contribution of illumination and observation geometries is not normalized
  OP=1/log(ATM_trans)*log(X(:,:,b_in)./L0);
else
  OP=zeros(size(X(:,:,1)));
end

return
