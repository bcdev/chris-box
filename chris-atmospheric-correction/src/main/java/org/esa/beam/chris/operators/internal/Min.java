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

    private static final double GOLDEN = 0.3819660;
    private static final double SQRT_DBL_EPSILON = Math.sqrt(1.0 / (0x00000001L << 52));


    public static class Bracket {
        double leftX;
        double leftF;
        double rightX;
        double rightF;
        double centerX;
        double centerF;
    }

    public static boolean findBracket(UnivariateFunction f, double a, double b, Bracket bracket, int maxEvalCount) {
        bracket.leftX = a;
        bracket.leftF = f.value(a);

        bracket.rightX = b;
        bracket.rightF = f.value(b);

        return findBracket(f, bracket, maxEvalCount);
    }

    public static boolean findBracket(UnivariateFunction f, Bracket bracket, int maxEvalCount) {
        // todo - checking
        double leftF = bracket.leftF;
        double rightF = bracket.rightF;
        double centerF;
        double leftX = bracket.leftX;
        double rightX = bracket.rightX;
        double centerX;

        int evalCount = 0;

        if (rightF >= leftF) {
            centerX = (rightX - leftX) * GOLDEN + leftX;
            evalCount++;
            centerF = f.value(centerX);
        } else {
            centerX = rightX;
            centerF = rightF;
            rightX = (centerX - leftX) / GOLDEN + leftX;
            evalCount++;
            rightF = f.value(rightX);
        }

        do {
            if (centerF < leftF) {
                if (centerF < rightF) {
                    bracket.leftX = leftX;
                    bracket.rightX = rightX;
                    bracket.centerX = centerX;
                    bracket.leftF = leftF;
                    bracket.rightF = rightF;
                    bracket.centerF = centerF;
                    return true;
                } else if (centerF > rightF) {
                    leftX = centerX;
                    leftF = centerF;
                    centerX = rightX;
                    centerF = rightF;
                    rightX = (centerX - leftX) / GOLDEN + leftX;
                    evalCount++;
                    rightF = f.value(rightX);
                } else { // centerF == rightF
                    rightX = centerX;
                    rightF = centerF;
                    centerX = (rightX - leftX) * GOLDEN + leftX;
                    evalCount++;
                    centerF = f.value(centerX);
                }
            } else { // centerF >= leftF
                rightX = centerX;
                rightF = centerF;
                centerX = (rightX - leftX) * GOLDEN + leftX;
                evalCount++;
                centerF = f.value(centerX);
            }
        } while (evalCount < maxEvalCount &&
                (rightX - leftX) > SQRT_DBL_EPSILON * ((rightX + leftX) * 0.5) + SQRT_DBL_EPSILON);

        bracket.leftX = leftX;
        bracket.rightX = rightX;
        bracket.centerX = centerX;
        bracket.leftF = leftF;
        bracket.rightF = rightF;
        bracket.centerF = centerF;

        return false;
    }

    public static boolean brent(UnivariateFunction f, Bracket bracket, int maxIter,
                                double absoluteAccuracyGoal,
                                double relativeAccuracyGoal) {
        // todo - checking
        double u;
        double v = bracket.leftX + GOLDEN * (bracket.rightX - bracket.leftX);
        double w = v;

        double d = 0.0;
        double e = 0.0;

        double fu;
        double fv = f.value(v);
        double fw = fv;

        for (int i = 0; i < maxIter; ++i) {
            final double a = bracket.leftX;
            final double b = bracket.rightX;
            final double z = bracket.centerX;

            final double fz = bracket.centerF;

            final double lowerW = (z - a);
            final double upperW = (b - z);
            final double tolerance = SQRT_DBL_EPSILON * abs(z);

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
                    bracket.rightX = z;
                    bracket.rightF = fz;
                } else {
                    bracket.leftX = z;
                    bracket.leftF = fz;
                }

                v = w;
                w = z;
                fv = fw;
                fw = fz;

                bracket.centerX = u;
                bracket.centerF = fu;
            } else {
                if (u < z) {
                    bracket.leftX = u;
                    bracket.leftF = fu;
                } else {
                    bracket.rightX = u;
                    bracket.rightF = fu;
                }
            }
            if (testInterval(bracket.leftX, bracket.rightX, absoluteAccuracyGoal, relativeAccuracyGoal)) {
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
