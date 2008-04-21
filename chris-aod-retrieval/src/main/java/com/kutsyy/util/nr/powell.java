/*
 * put your module comment here
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */

package com.kutsyy.util.nr;

import com.kutsyy.util.*;

/**
 *  Title: Library for Ordinal Spatial Data Model Description: This library was
 *  created as part of my dissertation. Main part of the library concentrates on
 *  latent variable model, however Bayesian model also is implemented. Library
 *  also includes set of utilities that could be used alone (com.kutsyy.util).
 *  For more information please see my dissertation, or my website <a
 *  href="http://www.kutsyy.com">http://www.kutsyy.com</a> Copyright: Copyright
 *  (c) 2000 Company: The University of Michigan
 *
 *@author     Vadim Kutsyy
 *@created    December 2, 2000
 *@version    1.0
 */
public class powell {
	/**
	 *  Description of the Field
	 */
	public int iter;
	/**
	 *  Description of the Field
	 */
	public double fret;
	private int i;
	private int ibig;
	private int j;
	private double del;
	private double fp;
	private double fptt;
	private double t;
	private double[] pt;
	private double[] ptt;
	private double[] xit;
	private int n;
	private linmin Linmin = new linmin();
	private final int ITMAX = 200;


	/**
	 *  Constructor for the powell object
	 *
	 */
	public powell() {
	}


	/**
	 *  Constructor for the powell object
	 *
	 *@param  p     Description of Parameter
	 *@param  xi    Description of Parameter
	 *@param  ftol  Description of Parameter
	 *@param  func  Description of Parameter
	 */
	public powell(double[] p, double[][] xi, double ftol, MvFunction func) {
		powell(p, xi, ftol, func);
	}


	/**
	 *  Description of the Method
	 *
	 *@param  p                                 Description of Parameter
	 *@param  xi                                Description of Parameter
	 *@param  ftol                              Description of Parameter
	 *@param  func                              Description of Parameter
	 *@exception  IllegalMonitorStateException  Description of Exception
	 *@exception  IllegalArgumentException      Description of Exception
	 */
	public void powell(double[] p, double[][] xi, double ftol, MvFunction func)
			 throws IllegalMonitorStateException,
			IllegalArgumentException {
		if (p.length != xi.length || xi.length != xi[0].length) {
			throw new IllegalArgumentException("dimentions must agree");
		}
		if (n != p.length) {
			n = p.length;
			pt = new double[n];
			ptt = new double[n];
			xit = new double[n];
		}
		fret = func.f(p);
		for (j = 0; j < n; j++) {
			pt[j] = p[j];
		}
		for (iter = 1; true; ++iter) {
			fp = fret;
			ibig = 0;
			del = 0.0;
			for (i = 0; i < n; i++) {
				for (j = 0; j < n; j++) {
					xit[j] = xi[j][i];
				}
				fptt = fret;
				Linmin.linmin(p, xit, func);
				fret = Linmin.fret;
				if (Math.abs(fptt - fret) > del) {
					del = Math.abs(fptt - fret);
					ibig = i;
				}
			}
			if (2.0 * Math.abs(fp - fret) <= ftol * (Math.abs(fp) + Math.abs(fret))) {
				return;
			}
			if (iter == ITMAX) {
				throw new IllegalMonitorStateException("powell exceeding maximum iterations.");
			}
			for (j = 0; j < n; j++) {
				ptt[j] = 2.0 * p[j] - pt[j];
				xit[j] = p[j] - pt[j];
				pt[j] = p[j];
			}
			fptt = func.f(ptt);
			if (fptt < fp) {
				t = 2.0 * (fp - 2.0 * fret + fptt) * (fp - fret - del)*(fp - fret - del) -
						del * (fp - fptt)*(fp - fptt);
				if (t < 0.0) {
					Linmin = new linmin(p, xit, func);
					fret = Linmin.fret;
					for (j = 0; j < n; j++) {
						xi[j][ibig] = xi[j][n-1];
						xi[j][n-1] = xit[j];
					}
				}
			}
		}
	}
}

