


 /*
  * put your module comment here
  * formatted with JxBeauty (c) johann.langhofer@nextra.at
  */
 
// package com.kutsyy.util.nr;
 
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
 public class mnbrak {
	 /**
	  *  Description of the Field
	  */
	 public double ax, bx, cx, fa, fb, fc;
	 private final double GOLD = 1.618034;
	 private final int GLIMIT = 100;
	 private final double TINY = 1.0e-20;
	 private double ulim;
	 private double u;
	 private double r;
	 private double q;
	 private double fu;
	 private double dum;
 
 
	 /**
	  *  put your documentation comment here
	  */
	 public mnbrak() {
	 }
 
 
	 //#define SHFT(a,b,c,d) (a)=(b);(b)=(c);(c)=(d);
	 /**
	  *  Constructor for the mnbrak object
	  *
	  *@param  Ax	Description of Parameter
	  *@param  Bx	Description of Parameter
	  *@param  fun  Description of Parameter
	  */
	 public mnbrak(double Ax, double Bx, Function fun) {
		 mnbrak(Ax, Bx, fun);
	 }
 
 
 /**
  *  Description of the Method
  *
  *@param  Ax	Description of Parameter
  *@param  Bx	Description of Parameter
  *@param  fun  Description of Parameter
  */
 public void mnbrak(double Ax, double Bx, Function fun) {
	ax = Ax;
	bx = Bx;
	fa = fun.f(ax);
	fb = fun.f(bx);
	if (fb > fa) {
	   dum = ax;
	   ax = bx;
	   bx = dum;
	   dum = fb;
	   fb = fa;
	   fa = dum;
	}
	cx = bx + GOLD * (bx - ax);
	fc = fun.f(cx);
	while (fb > fc) {
	   r = (bx - ax) * (fb - fc);
	   q = (bx - cx) * (fb - fa);
	   u =
		  bx
			 - ((bx - cx) * q - (bx - ax) * r)
				/ (2.0 * (q - r < 0 ? -1 : 1) * Math.max(Math.abs(q - r), TINY));
	   ulim = bx + GLIMIT * (cx - bx);
	   if ((bx - u) * (u - cx) > 0.0) {
		  fu = fun.f(u);
		  if (fu < fc) {
			 ax = bx;
			 bx = u;
			 fa = fb;
			 fb = fu;
			 return;
		  } else if (fu > fb) {
			 cx = u;
			 fc = fu;
			 return;
		  }
		  u = cx + GOLD * (cx - bx);
		  fu = fun.f(u);
	   } else if ((cx - u) * (u - ulim) > 0.0) {
		  fu = fun.f(u);
		  if (fu < fc) {
			 bx = cx;
			 cx = u;
			 u = cx + GOLD * (cx - bx);
			 fb = fc;
			 fc = fu;
			 fu = fun.f(u);
		  }
	   } else if ((u - ulim) * (ulim - cx) >= 0.0) {
		  u = ulim;
		  fu = fun.f(u);
	   } else {
		  u = (cx) + GOLD * (cx - bx);
		  fu = fun.f(u);
	   }
	   ax = bx;
	   bx = cx;
	   cx = u;
	   fa = fb;
	   fb = fc;
	   fc = fu;
	}
 }
 }


 
		

 
