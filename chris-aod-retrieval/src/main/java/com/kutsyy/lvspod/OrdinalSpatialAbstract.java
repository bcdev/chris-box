/*
 * put your module comment here
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 *
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */

package com.kutsyy.lvspod;

import com.kutsyy.util.*;
import com.kutsyy.util.nr.*;
import cern.jet.random.engine.*;

/**
 *  Insert the type's description here. Created by <A
 *  href="http://www.kutsyy.com">Vadim Kutsyy</A> <BR>
 *
 *
 *@author     <A href="http://www.kutsyy.com">Vadim Kutsyy</A>
 *@created    December 1, 2000
 */
abstract class OrdinalSpatialAbstract
         implements Function, MvFunction {
    /**
     *  Description of the Field
     */
    public MersenneTwister rnd = new MersenneTwister(new java.util.Date());
    /**
     *  Description of the Field
     */
    public double phi;
    /**
     *  estimated cut off points
     */
    public double[] theta;
    /**
     *  estimated value of beta
     */
    public double[] beta;
    /**
     *  obesved data Y
     */
    public int[] Y = null;
    /**
     *  total number of call s to lL() during given maximization
     */
    public int itotal = 0;
    /**
     *  Description of the Field
     */
    public final double THETA_LIMIT = 2;
    /**
     *  identified maximum and minimum for maximuzation over Phi Can be change
     *  prier to MLE
     */
    public final double PHI_LIMIT = 0.9;
    /**
     *  Description of the Field
     */
    public final double THETA_BETWEEN = 0.05;
    /**
     *  True Value of X, used only when data is generated
     */
    public double[] TrueX;
    /**
     *  Variance matrix, used for simulations
     */
    public double[][] W = null;
    /**
     *  correlation coeffitient
     */
    protected boolean[] keepConstant
             = {
            false, false, false
            };
    /**
     *  y coordinated of the data in Y
     */
    protected int[] yLoc = null;
    /**
     *  x coordinated of the data in Y
     */
    protected int[] xLoc = null;
    /**
     *  definition of the neighborhood by default it is nearest neighbor"
     */
    protected int[][] NeighborDefinition = {
            {
            0, 0, 1, -1
            }, {
            1, -1, 0, 0
            }
            };
    /**
     *  covariates matrix
     */
    protected double[][] Z = null;
    /**
     *  Keep true value of beta, used for simulations only
     */
    protected double[] TrueBeta = null;
    /**
     *  Description of the Field
     */
    protected double[][] Sigma = null;
    /**
     *  Relative error of maximizaton
     */
    protected final double EPS = 1e-2;
    /**
     *  Keep infoemation about neighborhood structure
     */
    protected SpatialPoint[] Loc;
    /**
     *  numer of covarites
     */
    protected int K;
    /**
     *  number of data points
     */
    protected int N;
    /**
     *  number of leveles
     */
    protected int L;
    /**
     *  True Value of theta, used only when data is generated
     */
    protected double[] TrueTheta;
    /**
     *  True Value of phi, used only when data is generated
     */
    protected double TruePhi;
    /**
     *  used internaly for calculating Sigma[][] during simulation
     */
    protected double[][] M = null;
    /**
     *  used internaly, square rot of Sigma, used for simulations.
     */
    protected double[][] sqrtSigma = null;
    /**
     *  Identified over which parameters to maximize 0 - over all 1 - over all
     *  but phi 2 - over all but beta 3 - over theta only
     */
    protected int max_over = 0;
    /**
     *  identified if particular field is a time series Simulations are done
     *  differently
     */
    protected boolean TimeSeries = false;
    /**
     *  used internaly for keeping old beta
     */
    protected double[] betaKeep;
    /**
     *  used internaly for keeping old value of theta
     */
    protected double[] thetaKeep;
    /**
     *  used for internal computations, keeping value of lower limit for the
     *  neighborhood
     */
    protected double[] thetal;
    /**
     *  used for internal computations, keeping value of lower limit for all
     *  points
     */
    protected double[] thetaL;
    /**
     *  used for internal computations, keeping value of upper limit for the
     *  neighborhood
     */
    protected double[] thetau;
    /**
     *  used for internal computations, keeping value of upper limit for all
     *  points
     */
    protected double[] thetaU;
    /**
     *  used internaly for keeping old value of Phi
     */
    protected double phiKeep;
    /**
     *  used internaly for any double arrays of length N
     */
    protected double[] x;
    /**
     *  used internaly for calculation of maximum
     */
    protected brent Brent = new brent();
    /**
     *  used internaly for multivariate maximization
     */
    protected powell Powell = new powell();
    /**
     *  used for multivariate maximization directions
     */
    protected double[][] xi;
    /**
     *  Used internaly as point for maximization
     */
    protected double[] p;
    /**
     *  internaly identified if gradient is implemented
     */
    protected boolean gradient = false;
    double[] lambda = null;


    /**
     *  Insert the method's description here.
     *
     *@param  y  int[]
     */
    public OrdinalSpatialAbstract(int[] y) {
        this(y, null, null, null, null);
    }


    /**
     *  Insert the method's description here.
     *
     *@param  y  int[]
     *@param  z  double[][]
     */
    public OrdinalSpatialAbstract(int[] y, double[][] z) {
        this(y, null, null, z, null);
    }


    /**
     *  Insert the method's description here.
     *
     *@param  T  Description of Parameter
     */
    public OrdinalSpatialAbstract(OrdinalSpatialAbstract T) {
        this(T.Y, T.xLoc, T.yLoc, T.Z, T.NeighborDefinition);
    }


    /**
     *  Constructor for the OrdinalSpatialAbstract object
     *
     *@param  y             Description of Parameter
     *@param  xLocation     Description of Parameter
     *@param  yLocation     Description of Parameter
     *@param  z             Description of Parameter
     *@param  Neighborgood  Description of Parameter
     */
    public OrdinalSpatialAbstract(int[] y, int[] xLocation, int[] yLocation,
            double[][] z, int[][] Neighborgood) {
        Y = (int[]) y.clone();
        N = Y.length;
        if (xLocation != null && yLocation != null) {
            if (y.length != xLocation.length || y.length != yLocation.length) {
                throw new IllegalArgumentException("y, xLocation and yLocation must agree in length");
            }
            xLoc = (int[]) xLocation.clone();
            yLoc = (int[]) yLocation.clone();
            NeighborDefinition = Neighborgood;
        }
        else {
            xLoc = new int[N];
            yLoc = new int[N];
            for (int i = 0; i < N; i++) {
                xLoc[i] = i;
                yLoc[i] = 0;
            }
            NeighborDefinition = new int[2][1];
            NeighborDefinition[0][0] = -1;
            NeighborDefinition[1][0] = 0;
        }
        if (z != null) {
            K = z[0].length;
            beta = new double[K];
            if (N != z.length) {
                throw new IllegalArgumentException("z must agree in dimentions with y");
            }
            Z = z;
        }
        int m = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            m = Y[i] < m ? Y[i] : m;
        }
        if (m != 0) {
            for (int i = 0; i < N; i++) {
                Y[i] -= m;
            }
        }
        L = 0;
        for (int i = 0; i < N; i++) {
            L = Y[i] > L ? Y[i] : L;
        }
        loop :
        for (int l = 0; l < L; l++) {
            for (int i = 0; i < N; i++) {
                if (Y[i] == l) {
                    continue loop;
                }
            }
            L--;
            for (int i = 0; i < N; i++) {
                if (Y[i] > l) {
                    Y[i]--;
                }
            }
            l--;
        }
        phi = 0;
        theta = new double[L];
        for (int i = 0; i < N; i++) {
            if (Y[i] < L) {
                theta[Y[i]]++;
            }
        }
        for (int i = 1; i < L; i++) {
            theta[i] += theta[i - 1];
        }
        for (int i = 0; i < L; i++) {
            theta[i] = CdfInv.nor(theta[i] / N);
        }
        Loc = new SpatialPoint[N];
        for (int i = 0; i < N; i++) {
            Loc[i] = new SpatialPoint(i, xLoc[i], yLoc[i]);
        }
        for (int k = 0; k < NeighborDefinition[0].length; k++) {
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    if (xLoc[i] + NeighborDefinition[0][k] == xLoc[j] && yLoc[i]
                             + NeighborDefinition[1][k] == yLoc[j]) {
                        Loc[i].addNeighbor(Loc[j]);
                    }
                }
            }
        }
        define();
    }


    /**
     *  Insert the method's description here.
     */
    public OrdinalSpatialAbstract() {
    }


    /**
     *  Insert the method's description here.
     *
     *@param  y          Description of Parameter
     *@param  xLocation  Description of Parameter
     *@param  yLocation  Description of Parameter
     *@param  z          Description of Parameter
     */
    public OrdinalSpatialAbstract(int[] y, int[] xLocation, int[] yLocation,
            double[][] z) {
        if (y.length != xLocation.length || y.length != yLocation.length) {
            throw new IllegalArgumentException("y, xLocation and yLocation must agree in length");
        }
        Y = (int[]) y.clone();
        xLoc = (int[]) xLocation.clone();
        yLoc = (int[]) yLocation.clone();
        N = yLoc.length;
        if (z != null) {
            K = z[0].length;
            beta = new double[K];
            for (int k = 0; k < K; k++) {
                beta[k] = 0;
            }
            if (N != z.length) {
                throw new IllegalArgumentException("z must agree in dimentions with y");
            }
            Z = z;
        }
        int m = Integer.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            m = Y[i] < m ? Y[i] : m;
        }
        if (m != 0) {
            for (int i = 0; i < N; i++) {
                Y[i] -= m;
            }
        }
        L = 0;
        for (int i = 0; i < N; i++) {
            L = Y[i] > L ? Y[i] : L;
        }
        loop :
        for (int l = 0; l < L; l++) {
            for (int i = 0; i < N; i++) {
                if (Y[i] == l) {
                    continue loop;
                }
            }
            L--;
            for (int i = 0; i < N; i++) {
                if (Y[i] > l) {
                    Y[i]--;
                }
            }
            l--;
        }
        phi = 0;
        theta = new double[L];
        for (int i = 0; i < K; i++) {
            theta[i] = CdfInv.nor((i + 1.0) / (K + 1.0));
        }
        define();
    }


    /**
     *  put your documentation comment here
     *
     *@param  y                   Description of Parameter
     *@param  Phi                 Description of Parameter
     *@param  Theta               Description of Parameter
     *@param  loc                 Description of Parameter
     *@param  Beta                Description of Parameter
     *@param  z                   Description of Parameter
     *@param  neighborDefinition  Description of Parameter
     */
    public OrdinalSpatialAbstract(int[] y, double Phi, double[] Theta, SpatialPoint[] loc,
            double[] Beta, double[][] z, int[][] neighborDefinition) {
        this.NeighborDefinition = neighborDefinition;
        this.Y = y;
        this.phi = Phi;
        if (Theta != null && Theta.length > 0) {
            this.theta = (double[]) Theta.clone();
            L = theta.length;
        }
        this.Loc = loc;
        N = y.length;
        if (Beta != null && Beta.length > 0) {
            this.beta = (double[]) Beta.clone();
            this.Z = z;
            K = beta.length;
        }
        define();
    }


    /**
     *  return value of the likelihood for value phi defined by x. used by
     *  maximization only
     *
     *@param  x  new value of phi
     *@return    lL(phi=x)
     */
    public double f(double x) {
        phiKeep = phi;
        phi = x;
        double l = ll();
        phi = phiKeep;
        return -l;
    }


    /**
     *  put your documentation comment here
     *
     *@param  x
     *@return
     */
    public double f(double[] x) {
        int ii = 0;
        if (!keepConstant[0]) {
            phiKeep = phi;
            phi = x[0];
            ii = 1;
        }
        if (!keepConstant[1] && theta != null) {
            if (thetaKeep == null) {
                thetaKeep = new double[theta.length];
            }
            for (int i = 0; i < L; i++) {
                thetaKeep[i] = theta[i];
                theta[i] = x[i + ii];
            }
            ii += L;
        }
        if (!keepConstant[2] && beta != null) {
            if (betaKeep == null) {
                betaKeep = new double[beta.length];
            }
            for (int i = 0; i < K; i++) {
                betaKeep[i] = beta[i];
                beta[i] = x[i + ii];
            }
        }
        double f = ll();
        ii = 0;
        if (!keepConstant[0]) {
            phi = phiKeep;
            ii = 1;
        }
        if (!keepConstant[1] && theta != null) {
            for (int i = 0; i < L; i++) {
                theta[i] = thetaKeep[i];
            }
        }
        if (!keepConstant[2] && beta != null) {
            for (int i = 0; i < K; i++) {
                beta[i] = betaKeep[i];
            }
        }
        return -f;
    }


    /**
     *  put your documentation comment here
     *
     *@param  x
     *@param  g                                  Description of Parameter
     *@exception  UnsupportedOperationException  Description of Exception
     */
    public void g(double[] x, double[] g) throws UnsupportedOperationException {
        throw new IllegalArgumentException("Not Implemented yet");
//		if (x.length != K + L + 1) {
//			throw new IllegalArgumentException("wrong length of x");
//		}
//		if (x.length != g.length) {
//			g = new double[x.length];
//		}
//		phiKeep = phi;
//		phi = x[0];
//		if (L > 0) {
//			thetaKeep = (double[]) theta.clone();
//			theta = La.getArray(x, 1, L);
//		}
//		if (K > 0) {
//			betaKeep = (double[]) beta.clone();
//			beta = La.getArray(x, L + 1, x.length);
//		}
//		grad(g);
//		phi = phiKeep;
//		if (L > 0) {
//			theta = (double[]) thetaKeep.clone();
//		}
//		if (K > 0) {
//			beta = (double[]) betaKeep.clone();
//		}
    }


    /**
     *  put your documentation comment here
     *
     *@param  y
     */
    public void updateY(int[] y) {
        this.Y = y;
    }


    /**
     *  Insert the method's description here.
     *
     *@return    double
     */
    public double lL() {
        initializeL();
        double l = 0;
        int thisl;
        for (int k = NeighborDefinition[0].length; k >= 0; k--) {
            double[] lKeep = new double[(int) Math.pow(L + 1, k + 1)];
            boolean[] lNotDefined = new boolean[lKeep.length];
            java.util.Arrays.fill(lNotDefined, true);
            for (int i = 0; i < N; i++) {
                if (Loc[i].neighbors.length == k) {
                    thisl = Y[i] * (int) Math.pow(L + 1, k);
                    for (int i1 = 0; i1 < k; i1++) {
                        thisl += Y[Loc[i].neighbors[i1]] * Math.pow(L + 1, k -
                                i1 - 1);
                    }
                    if (lNotDefined[thisl]) {
                        lNotDefined[thisl] = false;
                        lKeep[thisl] = ll(i);
                    }

                    l += lKeep[thisl];
                }
            }
        }
        return l;
    }


    /**
     *  Insert the method's description here.
     *
     *@param  phi    double
     *@param  theta  double[]
     *@return        double
     */
    public double lL(double phi, double[] theta) {
        return lL(phi, theta, null);
    }


    /**
     *  Insert the method's description here.
     *
     *@param  Theta  Description of Parameter
     */
    public void mLE(double[] Theta) {
        mLE(Theta, null);
    }


    /**
     *  put your documentation comment here
     *
     *@param  Theta
     *@param  Beta
     */
    public void mLE(double[] Theta, double[] Beta) {
        keepConstant[0] = false;
        keepConstant[1] = false;
        if (Theta != null && Theta.length == theta.length) {
            theta = (double[]) Theta.clone();
            keepConstant[1] = true;
        }
        keepConstant[2] = false;
        if (Beta != null && Beta.length == beta.length) {
            beta = (double[]) Beta.clone();
            keepConstant[2] = true;
        }
        mLE();
        for (int i = 0; i < 3; i++) {
            keepConstant[i] = false;
        }
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     */
    public void mLE(double Phi) {
        mLE(Phi, null, null);
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     *@param  Theta
     */
    public void mLE(double Phi, double[] Theta) {
        mLE(Phi, Theta, null);
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     *@param  Theta
     *@param  Beta
     */
    public void mLE(double Phi, double[] Theta, double[] Beta) {
        phi = Phi;
        keepConstant[0] = true;
        if (Theta != null && theta != null && Theta.length == theta.length) {
            theta = (double[]) Theta.clone();
            keepConstant[1] = true;
        }
        else {
            keepConstant[1] = false;
        }
        if (beta != null && Beta != null && Beta.length == beta.length) {
            beta = (double[]) Beta.clone();
            keepConstant[2] = true;
        }
        else {
            keepConstant[2] = false;
        }
        mLE();
        for (int i = 0; i < 3; i++) {
            keepConstant[i] = false;
        }
    }


    /**
     *  put your documentation comment here
     */
    public void mLE() {
        itotal = 0;
        if (K == 0 && L == 0) {
            if (!keepConstant[0]) {
                Brent.brent(-PHI_LIMIT, phi, PHI_LIMIT, this, EPS/(double) N);
                phi = Brent.xmin;
            }
        }
        else {
            int ii = (keepConstant[0] ? 0 : 1) + (keepConstant[1] ? 0 : L) +
                    (keepConstant[2] ? 0 : K);
            if (p == null || p.length != ii) {
                p = new double[ii];
                xi = new double[p.length][p.length];
                for (int i = 0; i < p.length; i++) {
                    xi[i][i] = 1;
                }
            }
            else {
                for (int i = 0; i < p.length; i++) {
                    xi[i][i] = 1;
                    for (int j = i + 1; j < p.length; j++) {
                        xi[i][j] = xi[j][i] = 0;
                    }
                }
            }
            ii = 1;
            if (!keepConstant[0]) {
                p[0] = phi;
            }
            else {
                ii = 0;
            }
            if (!keepConstant[1]) {
                for (int i = 0; i < L; i++) {
                    p[i + ii] = theta[i];
                }
                ii += L;
            }
            if (!keepConstant[2]) {
                for (int i = 0; i < K; i++) {
                    p[i + ii] = beta[i];
                }
            }
            Powell.powell(p, xi, EPS / (double) N, this);
            ii = 1;
            if (!keepConstant[0]) {
                phi = p[0];
            }
            else {
                ii = 0;
            }
            if (!keepConstant[1]) {
                for (int i = 0; i < L; i++) {
                    theta[i] = p[i + ii];
                }
                ii += L;
            }
            if (!keepConstant[2]) {
                for (int i = 0; i < K; i++) {
                    beta[i] = p[i + ii];
                }
            }
        }
    }


    /**
     *  Insert the method's description here.
     *
     *@param  phi    double
     *@param  theta  double[]
     *@param  beta   double[]
     *@return        double
     */
    public double lL(double phi, double[] theta, double[] beta) {
        double phiKeep = this.phi;
        if (this.theta != null) {
            thetaKeep = (double[]) this.theta.clone();
        }
        else {
            thetaKeep = null;
        }
        if (this.beta != null) {
            betaKeep = (double[]) this.beta.clone();
        }
        else {
            betaKeep = null;
        }
        this.phi = phi;
        this.theta = theta;
        this.beta = beta;
        double l = lL();
        this.phi = phiKeep;
        if (thetaKeep != null) {
            this.theta = (double[]) thetaKeep.clone();
        }
        else {
            this.theta = null;
        }
        if (betaKeep != null) {
            beta = (double[]) betaKeep.clone();
        }
        else {
            this.beta = null;
        }
        return l;
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     *@return      Description of the Returned Value
     */
    public double testPhi(double Phi) {
        max_over = 1;
        return test(Phi, null);
    }


    /**
     *  put your documentation comment here
     *
     *@param  Beta
     *@return       Description of the Returned Value
     */
    public double testBeta(double[] Beta) {
        max_over = 2;
        return test(0, Beta);
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     *@param  Beta
     *@return       Description of the Returned Value
     */
    public double testPhiBeta(double Phi, double[] Beta) {
        max_over = 3;
        return test(Phi, Beta);
    }


    /**
     *  Gets the Eigenvalues attribute of the OrdinalSpatialAbstract object
     */
    protected void createEigenvalues() {
        if (lambda != null) {
            return;
        }
        createW();
        lambda=new double[N];
        (new cern.colt.matrix.linalg.EigenvalueDecomposition(
                new cern.colt.matrix.impl.DenseDoubleMatrix2D(W))
                ).getRealEigenvalues().toArray(lambda);
//        lambda = (new Jama.EigenvalueDecomposition(new Jama.Matrix(W))).getRealEigenvalues();
    }


    /**
     *  Description of the Method
     *
     *@param  i  Description of Parameter
     *@return    Description of the Returned Value
     */
    protected double ll(int i) {
        return 0;
    }


    /**
     *  Description of the Method
     */
    protected void initializeL() {
    }


    /**
     *  Description of the Method
     *
     *@param  g                                  Description of Parameter
     *@exception  UnsupportedOperationException  Description of Exception
     */
    protected void grad(double[] g) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("not implemented");
    }


    /**
     *  Insert the method's description here.
     *
     *@param  Phi  Description of Parameter
     */
    protected void createSigma(double Phi) {
        if (Sigma == null) {
            Sigma = new double[N][N];
            M = new double[N][N];
        }
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                Sigma[i][j] = Sigma[j][i] = 0;
            }
            for (int k = 0; k < Loc[i].neighbors.length; k++) {
                Sigma[i][Loc[i].neighbors[k]] = -Phi / (double) Loc[i].neighbors.length;
            }
            Sigma[i][i] = 1;
            M[i][i] = 1 - (Loc[i].neighbors.length > 0 ? Phi * Phi / (double) Loc[i].neighbors.length : 0);
        }
        Sigma = La.times(La.solve(Sigma), M);
        //Sigma = La.cholsl(La.times(Sigma));
        sqrtSigma = La.choldc(Sigma);
    }


    /**
     *  Description of the Method
     */
    protected void createW() {
        if (W != null) {
            return;
        }
        W = new double[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < Loc[i].neighbors.length; j++) {
                W[i][Loc[i].neighbors[j]] = 1 / (double) Loc[i].neighbors.length;
            }
        }
    }


    /**
     *  Insert the method's description here. Insert the method's description
     *  here. Description of the Method
     *
     *@return    Description of the Returned Value
     *@return    Description of the Returned Value
     *@return    Description of the Returned Value
     */
    protected double ll() {
        for (int i = 1; i < L; i++) {
            if (theta[i - 1] + THETA_BETWEEN >= theta[i]) {
                return -Double.MAX_VALUE / 2.0;
            }
        }
        for (int i = 0; i < L; i++) {
            if (theta[i] > THETA_LIMIT || theta[i] < -THETA_LIMIT) {
                return -Double.MAX_VALUE / 2.0;
            }
        }
        if (phi > PHI_LIMIT || phi < -PHI_LIMIT) {
            return -Double.MAX_VALUE / 2.0;
        }
        itotal++;
        //long t = System.currentTimeMillis();
        double l = lL();
        //		if (theta != null) {
        //			System.out.println(itotal + " " + (System.currentTimeMillis() - t) + " " + phi + " " + theta[0] + " " + theta[1] + " " + l);
        //	}
        return l;
    }


    /**
     *  Insert the method's description here. Creation date: (11/19/2000 2:20:40
     *  PM)
     */
    protected void createLoc() {
        Loc = new SpatialPoint[N];
        for (int i = 0; i < N; i++) {
            Loc[i] = new SpatialPoint(i, xLoc[i], yLoc[i]);
        }
        for (int k = 0; k < NeighborDefinition[0].length; k++) {
            for (int j = 0; j < N; j++) {
                for (int i = 0; i < N; i++) {
                    if (xLoc[i] + NeighborDefinition[0][k] == xLoc[j] && yLoc[i]
                             + NeighborDefinition[1][k] == yLoc[j]) {
                        Loc[i].addNeighbor(Loc[j]);
                    }
                }
            }
        }
    }



    /**
     *  put your documentation comment here
     */
    protected void define() {
    }


    /**
     *  Description of the Method
     */
    protected void makeThetaLU() {
        if (thetaL == null) {
            thetaL = new double[N];
            thetaU = new double[N];
        }
        for (int i = 0; i < N; i++) {
            if (Y[i] == 0) {
                thetaU[i] = (theta[0]);
                thetaL[i] = Double.NEGATIVE_INFINITY;
            }
            else if (Y[i] == L) {
                thetaU[i] = Double.POSITIVE_INFINITY;
                thetaL[i] = theta[L - 1];
            }
            else {
                thetaL[i] = theta[Y[i] - 1];
                thetaU[i] = theta[Y[i]];
            }
        }
        if (K > 0) {
            x = La.times(Z, beta);
            for (int i = 0; i < N; i++) {
                thetaL[i] += x[i];
                thetaU[i] += x[i];
            }
        }
    }


    /**
     *  put your documentation comment here
     *
     *@param  Phi
     *@param  Beta
     *@return       Description of the Returned Value
     */
    protected double test(double Phi, double[] Beta) {
        phiKeep = phi;
        for (int i = 0; i < L; i++) {
            thetaKeep[i] = theta[i];
        }
        for (int i = 0; i < K; i++) {
            betaKeep[i] = beta[i];
        }
        mLE();
        double lHigh = lL();
        phi = phiKeep;
        for (int i = 0; i < L; i++) {
            theta[i] = thetaKeep[i];
        }
        for (int i = 0; i < K; i++) {
            beta[i] = betaKeep[i];
        }
        int length = L + ((max_over == 0 || max_over == 1) ? 1 : 0) + ((max_over
                 == 0 || max_over == 2) ? 1 : K);
        p = new double[length];
        xi = new double[length][length];
        for (int i = 0; i < length; i++) {
            xi[i][i] = 1;
        }
        int over = ((max_over == 0 || max_over == 1) ? 1 : 0);
        if (over == 1) {
            p[0] = phi;
        }
        else {
            phi = Phi;
        }
        for (int i = 0; i < L; i++) {
            p[i + over] = thetaKeep[i];
        }
        if ((max_over == 0 || max_over == 2)) {
            for (int i = 0; i < K; i++) {
                p[i + over + L] = betaKeep[i];
            }
        }
        else {
            for (int i = 0; i < K; i++) {
                beta[i] = Beta[i];
            }
        }
        Powell.powell(p, xi, EPS, this);
        if (max_over == 0 || max_over == 1) {
            phi = p[0];
        }
        for (int i = 0; i < L; i++) {
            theta[i] = p[i + 1];
        }
        if (max_over == 0 || max_over == 2) {
            for (int i = 0; i < K; i++) {
                beta[i] = p[1 + L + i];
            }
        }
        double lLow = lL();
        phi = phiKeep;
        for (int i = 0; i < L; i++) {
            theta[i] = thetaKeep[i];
        }
        for (int i = 0; i < K; i++) {
            beta[i] = betaKeep[i];
        }
        max_over = 0;
        return lLow / lHigh;
    }
}

