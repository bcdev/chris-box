function [varargout] = FCLSU(HIM,M,Delta)
% [Abundances,RMSE]=FCLSU(HIM,M,Delta)
% Fully Constrained Linear Spectral Unmixing
% M is the endmember matrix
% Delta: error = 1/1000 or 1/10000
% The dimensions of the matrix are l x p
% l is the number of bands
% p is the number of endmembers
% each endmember is located in a row of the matrix

if nargout==0, return, end

[ns,nl,nb] = size(HIM);
[l,p] = size(M);

N = zeros(l+1,p);
N(1:l,1:p) = Delta*M;
N(l+1,:) = ones(1,p);
s = zeros(l+1,1);

OutputImage = zeros(ns,nl,p);

for i = 1:ns
    %disp(i);
    for j = 1:nl
        s(1:l) = Delta*squeeze(HIM(i,j,:));
        s(l+1) = 1;
        if nargout==1,
          Abundances = lsqnonneg(N,s);
          OutputImage(i,j,:) = Abundances;
          %RMSE(i,j)= sum((squeeze(HIM(i,j,:))' - Abundances'*M').^2)/l;
        elseif nargout==2,
          [OutputImage(i,j,:), RMSE(i,j)]= lsqnonneg(N,s);
        end
    end
end
disp('Fully Constrained Linear Spectral Unmixing')

if nargout==1
  varargout{1}=OutputImage;
elseif nargout==2
  varargout{1}=OutputImage;
  varargout{2}=RMSE;
end

return
