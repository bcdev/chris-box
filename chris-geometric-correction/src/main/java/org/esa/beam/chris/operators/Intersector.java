package org.esa.beam.chris.operators;

import com.bc.ceres.core.Assert;
import org.esa.beam.chris.util.math.internal.Pow;

/**
 * Utility class for calculating the intersection of a straight line and an ellipsoid.
 */
class Intersector {

    /**
     * Calculates the intersection of a straight line and an ellipsoid.
     * <p/>
     * Note that all parameters must be arrays of length 3.
     *
     * @param point     any point on the straight line. On return contains
     *                  the intersection point closest this point.
     *                  If the straight line and the ellipsoid do not intersect,
     *                  all coordinates are {@link Double#NaN}.
     * @param direction the direction of the straight line.
     * @param center    the center of the ellipsoid.
     * @param radii     the radii of the ellipsoid.
     */
    public static void intersect(double[] point, double[] direction, double[] center, double[] radii) {
        Assert.notNull(point);
        Assert.notNull(direction);
        Assert.notNull(center);
        Assert.notNull(radii);

        Assert.argument(point.length == 3);
        Assert.argument(direction.length == 3);
        Assert.argument(center.length == 3);
        Assert.argument(radii.length == 3);

        double a = 0.0;
        for (int i = 0; i < 3; i++) {
            a += Pow.pow2(direction[i] / radii[i]);
        }

        double b = 0.0;
        for (int i = 0; i < 3; i++) {
            b += direction[i] * (point[i] - center[i]) / (Pow.pow2(radii[i]));
        }

        double c = 0.0;
        for (int i = 0; i < 3; i++) {
            c += Pow.pow2((point[i] - center[i]) / radii[i]);
        }
        c -= 1.0;

        final double d = b * b - a * c;
        final double t;
        if (b > 0.0) {
            t = (-b + Math.sqrt(d)) / a;
        } else {
            t = (-b - Math.sqrt(d)) / a;
        }

        for (int i = 0; i < 3; i++) {
            point[i] += t * direction[i];
        }
    }
}
