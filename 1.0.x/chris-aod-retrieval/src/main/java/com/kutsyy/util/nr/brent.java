/*
 * put your module comment here
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */

package com.kutsyy.util.nr;

import com.kutsyy.util.*;

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
public class brent {
	/**
	 *  Description of the Field
	 */
	public double xmin;
	/**
	 *  Description of the Field
	 */
	public double fx;
	private int iter;
	private double a;
	private double b;
	private double d = 0;
	private double etemp;
	private double fu;
	private double fw;
	private double fv;
	private double p;
	private double q;
	private double r;
	private double tol1;
	private double tol2;
	private double u;
	private double v;
	private double w;
	private double x;
	private double xm;
	private double e = 0.0;
	private final static int ITMAX = 100;
	private final static double CGOLD = 0.3819660;
	private final static double ZEPS = 1.0e-10;


	/**
	 *  put your documentation comment here
	 */
	public brent() {
	}


	/**
	 *  put your documentation comment here
	 *
	 *@param  ax                         Description of Parameter
	 *@param  bx                         Description of Parameter
	 *@param  cx                         Description of Parameter
	 *@param  fun                        Description of Parameter
	 *@param  tol                        Description of Parameter
	 *@exception  IllegalStateException  Description of Exception
	 */
	public brent(double ax, double bx, double cx, Function fun, double tol) throws IllegalStateException {
		brent(ax, bx, cx, fun, tol);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  ax                         Description of Parameter
	 *@param  bx                         Description of Parameter
	 *@param  cx                         Description of Parameter
	 *@param  fun                        Description of Parameter
	 *@param  tol                        Description of Parameter
	 *@exception  IllegalStateException  Description of Exception
	 */
	public void brent(double ax, double bx, double cx, Function fun, double tol) throws IllegalStateException {
		xmin = Double.NaN;
      e=0;
		a = (ax < cx ? ax : cx);
		b = (ax > cx ? ax : cx);
		x = w = v = bx;
		fw = fv = fx = fun.f(x);
		for (iter = 0; iter < ITMAX; iter++) {
			xm = 0.5 * (a + b);
         tol1 = tol * Math.abs(x)+ ZEPS;
			tol2 = 2.0 * tol1  ;
			if (Math.abs(x - xm) <= (tol2 - 0.5 * (b - a))) {
				xmin = x;
				return;
			}
			if (Math.abs(e) > tol1) {
				r = (x - w) * (fx - fv);
				q = (x - v) * (fx - fw);
				p = (x - v) * q - (x - w) * r;
				q = 2.0 * (q - r);
				if (q > 0.0) {
					p = -p;
				}
				q = Math.abs(q);
				etemp = e;
				e = d;
				if (Math.abs(p) >= Math.abs(0.5 * q * etemp) || p <= q * (a - x) ||
						p >= q * (b - x)) {
                  e = (x >= xm ? a - x : b - x);
					d = CGOLD * e;
				}
				else {
					d = p / q;
					u = x + d;
					if (u - a < tol2 || b - u < tol2) {
						d = (xm - x >= 0 ? Math.abs(tol1) : -Math.abs(tol1));
					}
				}
			}
			else {
				d = CGOLD * (e = (x >= xm ? a - x : b - x));
			}
			u = (Math.abs(d) >= tol1 ? x + d : x + (d < 0 ? -Math.abs(tol1) : Math.abs(tol1)));
			fu = fun.f(u);
			if (fu <= fx) {
				if (u >= x) {
					a = x;
				}
				else {
					b = x;
				}
				v = w;
				w = x;
				x = u;
				fv = fw;
				fw = fx;
				fx = fu;
			}
			else {
				if (u < x) {
					a = u;
				}
				else {
					b = u;
				}
				if (fu <= fw || w == x) {
					v = w;
					w = u;
					fv = fw;
					fw = fu;
				}
				else if (fu <= fv || v == x || v == w) {
					v = u;
					fv = fu;
				}
			}
		}
		xmin = x;
		throw new IllegalStateException("Too many iterations in brent");
	}
}

