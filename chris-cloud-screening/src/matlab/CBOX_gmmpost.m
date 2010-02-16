function [post, a] = CBOX_gmmpost(mix, x)
%Compute the class posterior probabilities and activations of a Gaussian mixture model.

ndata = size(x, 1);

% Compute the activations of a Gaussian mixture model.
a = zeros(ndata, mix.ncentres);  % Preallocate matrix
normal = (2*pi)^(mix.nin/2);
for j = 1:mix.ncentres
  diffs = x - (ones(ndata, 1) * mix.centres(j, :));
  % Use Cholesky decomposition of covariance matrix to speed computation
  c = chol(mix.covars(:, :, j));
  temp = diffs/c;
  a(:, j) = exp(-0.5*sum(temp.*temp, 2))./(normal*prod(diag(c)));
end
  
%Compute the class posterior probabilities of a Gaussian mixture model.
post = (ones(ndata, 1)*mix.priors).*a;
s = sum(post, 2);
if any(s==0)   %Some zero posterior probabilities
   % Set any zeros to one before dividing
   zero_rows = find(s==0);
   s = s + (s==0);
   post(zero_rows, :) = 1/mix.ncentres;
end
post = post./(s*ones(1, mix.ncentres));

end
