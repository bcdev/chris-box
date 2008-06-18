/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators.internal;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Min {

    public static final double GOLDEN = 0.3819660;

    public static class Bracket {
        public double lowerX;
        public double lowerF;
        public double upperX;
        public double upperF;
        public double minimumX;
        public double minimumF;
    }

    public static boolean bracket(UnivariateFunction f, double lowerX, double upperX, Bracket bracket) {
        return bracket(f, lowerX, upperX, bracket, 100);
    }

    public static boolean bracket(UnivariateFunction f, double lowerX, double upperX, Bracket bracket,
                                  int maxEvalCount) {
        bracket.lowerX = lowerX;
        bracket.lowerF = f.value(lowerX);

        bracket.upperX = upperX;
        bracket.upperF = f.value(upperX);

        return bracket(f, bracket, maxEvalCount);
    }

    private static boolean bracket(UnivariateFunction f, Bracket bracket, int maxEvalCount) {
        // todo - checking
        double lowerF = bracket.lowerF;
        double upperF = bracket.upperF;
        double lowerX = bracket.lowerX;
        double upperX = bracket.upperX;

        double minimumF;
        double minimumX;

        int evalCount = 0;

        if (upperF >= lowerF) {
            minimumX = (upperX - lowerX) * GOLDEN + lowerX;
            evalCount++;
            minimumF = f.value(minimumX);
        } else {
            minimumX = upperX;
            minimumF = upperF;
            upperX = (minimumX - lowerX) / GOLDEN + lowerX;
            evalCount++;
            upperF = f.value(upperX);
        }

        do {
            if (minimumF < lowerF) {
                if (minimumF < upperF) {
                    bracket.lowerX = lowerX;
                    bracket.upperX = upperX;
                    bracket.minimumX = minimumX;
                    bracket.lowerF = lowerF;
                    bracket.upperF = upperF;
                    bracket.minimumF = minimumF;
                    return true;
                } else if (minimumF > upperF) {
                    lowerX = minimumX;
                    lowerF = minimumF;
                    minimumX = upperX;
                    minimumF = upperF;
                    upperX = (minimumX - lowerX) / GOLDEN + lowerX;
                    evalCount++;
                    upperF = f.value(upperX);
                } else { // minimumF == upperF
                    upperX = minimumX;
                    upperF = minimumF;
                    minimumX = (upperX - lowerX) * GOLDEN + lowerX;
                    evalCount++;
                    minimumF = f.value(minimumX);
                }
            } else { // minimumF >= lowerF
                upperX = minimumX;
                upperF = minimumF;
                minimumX = (upperX - lowerX) * GOLDEN + lowerX;
                evalCount++;
                minimumF = f.value(minimumX);
            }
        } while (evalCount < maxEvalCount &&
                (upperX - lowerX) > Constants.SQRT_DBL_EPSILON * ((upperX + lowerX) * 0.5) + Constants.SQRT_DBL_EPSILON);

        bracket.lowerX = lowerX;
        bracket.upperX = upperX;
        bracket.lowerF = lowerF;
        bracket.upperF = upperF;

        bracket.minimumX = minimumX;
        bracket.minimumF = minimumF;

        return false;
    }

    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal) {
        return brent(f, bracket, relativeAccuracyGoal, 1.0E-10);
    }

    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal,
                                double absoluteAccuracyGoal) {
        return brent(f, bracket, relativeAccuracyGoal, absoluteAccuracyGoal, 100);
    }

    public static boolean brent(UnivariateFunction f, Bracket bracket, double relativeAccuracyGoal,
                                double absoluteAccuracyGoal, int maxIter) {
        // todo - checking
        double u;
        double v = bracket.lowerX + GOLDEN * (bracket.upperX - bracket.lowerX);
        double w = v;

        double d = 0.0;
        double e = 0.0;

        double fu;
        double fv = f.value(v);
        double fw = fv;

        for (int i = 0; i < maxIter; ++i) {
            final double a = bracket.lowerX;
            final double b = bracket.upperX;
            final double z = bracket.minimumX;

            final double fz = bracket.minimumF;

            final double lowerW = (z - a);
            final double upperW = (b - z);
            final double tolerance = Constants.SQRT_DBL_EPSILON * abs(z);

            final double midpoint = 0.5 * (a + b);

            double p = 0.0;
            double q = 0.0;
            double r = 0.0;

            if (abs(e) > tolerance) {
                r = (z - w) * (fz - fv);
                q = (z - v) * (fz - fw);
                p = (z - v) * q - (z - w) * r;
                q = 2.0 * (q - r);

                if (q > 0.0) {
                    p = -p;
                } else {
                    q = -q;
                }

                r = e;
                e = d;
            }
            if (abs(p) < abs(0.5 * q * r) && p < q * lowerW && p < q * upperW) {
                final double t2 = 2.0 * tolerance;

                d = p / q;
                u = z + d;

                if ((u - a) < t2 || (b - u) < t2) {
                    d = (z < midpoint) ? tolerance : -tolerance;
                }
            } else {
                e = (z < midpoint) ? b - z : -(z - a);
                d = GOLDEN * e;
            }
            if (abs(d) >= tolerance) {
                u = z + d;
            } else {
                u = z + ((d > 0.0) ? tolerance : -tolerance);
            }

            fu = f.value(u);

            if (fu <= fz) {
                if (u < z) {
                    bracket.upperX = z;
                    bracket.upperF = fz;
                } else {
                    bracket.lowerX = z;
                    bracket.lowerF = fz;
                }

                v = w;
                w = z;
                fv = fw;
                fw = fz;

                bracket.minimumX = u;
                bracket.minimumF = fu;
            } else {
                if (u < z) {
                    bracket.lowerX = u;
                    bracket.lowerF = fu;
                } else {
                    bracket.upperX = u;
                    bracket.upperF = fu;
                }
            }
            if (testInterval(bracket.lowerX, bracket.upperX, absoluteAccuracyGoal, relativeAccuracyGoal)) {
                return true;
            }
        }

        return false;
    }

    private static boolean testInterval(double lowerX, double upperX, double absoluteAccuracyGoal,
                                        double relativeAccuracyGoal) {
        final double minAbs;
        if (lowerX > 0.0 && upperX > 0.0 || lowerX < 0.0 && upperX < 0.0) {
            minAbs = min(abs(lowerX), abs(upperX));
        } else {
            minAbs = 0.0;
        }

        return abs(upperX - lowerX) < absoluteAccuracyGoal + relativeAccuracyGoal * minAbs;
    }
}
