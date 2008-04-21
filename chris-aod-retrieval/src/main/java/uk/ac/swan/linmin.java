//package com.kutsyy.util.nr;
// import com.kutsyy.util.*;

package uk.ac.swan;

/**
  *  Title: com.kutsyy.util Description: Mathematical and Statistical Utilities.
  *  Requires <a href="http://tilde-hoschek.home.cern.ch/~hoschek/colt/index.htm">
  *  Colt</a> and <a href="http://math.nist.gov/javanumerics/jama/">Jama</a>
  *  libraries. Copyright: Copyright (c) 2000 Company: The University of Michigan
  *
  *@author     <a href="http://www.kutsyy.com">Vadim Kutsyy</a>
  *@created    December 1, 2000
  *@version    1.0
  */
 public class linmin
		  implements Function {
	 /**
	  *  Description of the Field
	  */
	 public double fret;
	 int ncom;
	 double[] pcom, xicom;
	 double[] xt;
	 private boolean mv = false;
	 private MvFunction fun;
	 private mnbrak Mnbrak = new mnbrak();
	 private brent Brent = new brent();
	 // private 	 double[] fc;
	 private int j;
	 private double xx;
	 private double xmin;
	 private double fx;
	 private double fb;
	 private double fa;
	 private double bx;
	 private double ax;
	 private int n;
	 private final static double TOL = 2.0e-4;
 
 
	 /**
	  *  put your documentation comment here
	  */
	 public linmin() {
	 }
 
 
 
	 /**
	  *  Constructor for the linmin object
	  *
	  *@param  p   Description of Parameter
	  *@param  xi  Description of Parameter
	  *@param  f   Description of Parameter
	  */
	 public linmin(double[] p, double xi[], MvFunction f) {
		 linmin(p, xi, f);
	 }
 
 
	 /**
	  *  Description of the Method
	  *
	  *@param  p							 Description of Parameter
	  *@param  xi							 Description of Parameter
	  *@param  f							 Description of Parameter
	  *@exception  IllegalArgumentException  Description of Exception
	  */
	 public void linmin(double[] p, double xi[], MvFunction f) throws IllegalArgumentException {
		 if (p.length != xi.length) {
			 throw new IllegalArgumentException("dimentions must agree");
		 }
		 if (n != p.length) {
			 n = p.length;
			 pcom = new double[n];
			 xicom = new double[n];
		 }
		 fun = f;
		 ncom = n;
		 for (j = 0; j < n; j++) {
			 pcom[j] = p[j];
			 xicom[j] = xi[j];
		 }
		 ax = 0.0;
		 xx = 1.0;
		 Mnbrak.mnbrak(ax, xx, this);
		 ax = Mnbrak.ax;
		 xx = Mnbrak.bx;
		 bx = Mnbrak.cx;
		 fa = Mnbrak.fa;
		 fx = Mnbrak.fb;
		 fb = Mnbrak.fc;
		 Brent.brent(ax, xx, bx, this, TOL);
		 fret = Brent.fx;
		 xmin = Brent.xmin;
		 for (j = 0; j < n; j++) {
			 xi[j] *= xmin;
			 p[j] += xi[j];
		 }
	 }
 
 
	 /**
	  *  put your documentation comment here
	  *
	  *@param  x
	  *@return
	  */
	 public double f(double x) {
		 return f1dim(x);
	 }
 
 
	 /**
	  *  put your documentation comment here
	  *
	  *@param  x
	  *@return
	  */
	 private double nrfunc(double[] x) {
		 return fun.f(x);
	 }
 
 
	 /**
	  *  put your documentation comment here
	  *
	  *@param  x
	  *@return
	  */
	 private double f1dim(double x) {
		 if (xt==null||xt.length != ncom) {
			 xt = new double[ncom];
		 }
		 for (int j = 0; j < ncom; j++) {
			 xt[j] = pcom[j] + x * xicom[j];
		 }
		 return fun.f(xt);
	 }
 }
