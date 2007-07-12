function SAD=CBOX_sad(X)
% Calculates the Spectral Angle Distance between neighbours in the horizontal direction (zeros in the first column)
%  'X' is a 3D matrix with the hyperspectral image
%  The result is similar to a spatial derivative filter (edge detection) but taking into account the SAD between neighbours.


[Nrow,Ncol,Nban]=size(X); %sixe of the input image

SAD=zeros(Nrow,Ncol);

  %spatio-spectral derivative filter in rows
  for r=1:Nrow  
    norma_old=0;   %norm of the spectrum of the previous pixel (left)
    SAD(r,1)=0;    %first column is zero
    for c=2:Ncol
      %Summation of the bands to calculate the norm and the scalar product of the spectra
      norma=0; prod_esc=0; 
      for b=1:Nban
       norma=norma+X(r,c,b)^2; 
       prod_esc=prod_esc+X(r,c-1,b)*X(r,c,b); %scalar product of horizontal neighbours
      end
      norma=sqrt(norma);                      %norm of each pixel
      cosine=prod_esc/norma_old/norma;        %cosine of the spectra
      SAD(r,c)=real(acos(cosine));            %spectral angle distance
      norma_old=norma;                        %next neighbour
    end
  end


return

