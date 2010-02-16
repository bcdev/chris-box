function mix=CBOX_em_clustering(x,ncentres,niters_km,niters_em)

[ndata, nin] = size(x);

% Initialise centres  from data
%centres = randn(ncentres, nin);
perm = randperm(ndata);
perm = perm(1:ncentres);
% Assign first ncentres (permuted) data points as centres
centres = x(perm, :);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Use kmeans algorithm to set centres
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Max. number of iterations.
niters = niters_km;

% Matrix to make unit vectors easy to construct
id = eye(ncentres);

% Main loop of algorithm
for n = 1:niters
  
  % Calculate posteriors based on existing centres
  % squared distance between centres and data.
  d2 = (ones(ncentres, 1) * sum((x.^2)', 1))' + ones(ndata, 1) * sum((centres.^2)',1) - 2.*(x*(centres'));
  % rounding errors occasionally cause negative entries in d2
  if any(any(d2<0)), d2(d2<0) = 0; end
  % Assign each point to nearest centre
  [minvals, index] = min(d2', [], 1);
  post = id(index,:);

  num_points = sum(post, 1);
  % Adjust the centres based on new posteriors
  for j = 1:ncentres
    if (num_points(j) > 0)
      centres(j,:) = sum(x(find(post(:,j)),:), 1)/num_points(j);
    end
  end

end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Initialisation of the Gaussian mixture model
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

mix.centres=centres;
[mix.ncentres mix.nin]=size(mix.centres);

% Set priors depending on number of points in each cluster
cluster_sizes = max(sum(post, 1), 1);  % Make sure that no prior is zero
mix.priors = cluster_sizes/sum(cluster_sizes); % Normalise priors

% Initialise covariance matrices 
%covars = repmat(eye(nin), [1 1 ncentres]);
for j = 1:mix.ncentres
  % Pick out data points belonging to this centre
  c = x(find(post(:, j)),:);
  diffs = c - (ones(size(c, 1), 1) * mix.centres(j, :));
  mix.covars(:,:,j) = (diffs'*diffs)/(size(c, 1));
  % Add Identity to rank-deficient covariance matrices
  if rank(mix.covars(:,:,j)) < mix.nin
  mix.covars(:,:,j) = mix.covars(:,:,j) + eye(mix.nin);
  end
end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% EM algorithm for Gaussian mixture model.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Check covariance matrix (reset to its initial value if singular)
check_covars = 1;	% Ensure that covariances don't collapse
MIN_COVAR = eps;	% Minimum singular value of covariance matrix
init_covars = mix.covars;

% Max. number of iterations.
niters = niters_em;

% Main loop of algorithm
for n = 1:niters
  
  % Calculate posteriors based on old parameters
  [post, act] = CBOX_gmmpost(mix, x);
  
  % Adjust the new estimates for the parameters
  new_pr = sum(post, 1);
  new_c = post' * x;
  
  % Now move new estimates to old parameters
  mix.priors = new_pr ./ ndata;
  mix.centres = new_c ./ (new_pr' * ones(1, mix.nin));
  for j = 1:mix.ncentres
    diffs = x - (ones(ndata, 1) * mix.centres(j,:));
    diffs = diffs.*(sqrt(post(:,j))*ones(1, mix.nin));
    mix.covars(:,:,j) = (diffs'*diffs)/new_pr(j);
  end
  if check_covars % Ensure that no covariance is too small
    for j = 1:mix.ncentres
      if min(svd(mix.covars(:,:,j))) < MIN_COVAR
        mix.covars(:,:,j) = init_covars(:,:,j);
      end
    end
  end
  
end

