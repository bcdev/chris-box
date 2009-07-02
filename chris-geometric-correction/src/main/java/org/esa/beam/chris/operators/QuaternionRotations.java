package org.esa.beam.chris.operators;

import com.bc.ceres.core.Assert;

import java.text.MessageFormat;

/**
 * Utility class for calculating 3-dimensional vector rotations
 * defined by unit quaternions.
 */
class QuaternionRotations {

    /**
     * Performs a rotation on n 3-dimensional vectors.
     *
     * @param q the unit-quaternion defining the rotation.
     * @param x the x-components of the n 3-dimensional vectors being rotated.
     * @param y the x-components of the n 3-dimensional vectors being rotated.
     * @param z the x-components of the n 3-dimensional vectors being rotated.
     */
    public static void rotateVectors(Quaternion q, double[] x, double[] y, double[] z) {
        Assert.notNull(x);
        Assert.notNull(y);
        Assert.notNull(z);

        final int n = x.length;
        Assert.argument(x.length != 0);
        Assert.argument(y.length == n);
        Assert.argument(z.length == n);

        final double a = q.getR();
        final double b = q.getI();
        final double c = q.getJ();
        final double d = q.getK();

        final double ab = a * b;
        final double ac = a * c;
        final double ad = a * d;
        final double bb = b * b;
        final double bc = b * c;
        final double bd = b * d;
        final double cc = c * c;
        final double cd = c * d;
        final double dd = d * d;

        for (int i = 0; i < n; i++) {
            forward(i, x, y, z, ab, ac, ad, bb, bc, bd, cc, cd, dd);
        }
    }

    /**
     * Performs a rotation on n 3-dimensional vectors.
     *
     * @param quaternions the n unit-quaternions, where the ith quaternion defines the
     *                    rotation to be applied to the ith vector.
     * @param x           the x-components of the n 3-dimensional vectors being rotated.
     * @param y           the x-components of the n 3-dimensional vectors being rotated.
     * @param z           the x-components of the n 3-dimensional vectors being rotated.
     */
    public static void rotateVectors(Quaternion[] quaternions, double[] x, double[] y, double[] z) {
        Assert.notNull(quaternions);
        Assert.notNull(x);
        Assert.notNull(y);
        Assert.notNull(z);

        final int n = quaternions.length;
        Assert.argument(quaternions.length != 0);
        Assert.argument(x.length == n);
        Assert.argument(y.length == n);
        Assert.argument(z.length == n);

        for (int i = 0; i < n; i++) {
            Assert.argument(quaternions[i] != null, MessageFormat.format("quaternions[{0}] == null", i));
        }
        for (int i = 0; i < n; i++) {
            forward(quaternions[i], x, y, z, i);
        }
    }

    static void forward(Quaternion q, double[] x, double[] y, double[] z, int i) {
        final double a = q.getR();
        final double b = q.getI();
        final double c = q.getJ();
        final double d = q.getK();

        final double ab = a * b;
        final double ac = a * c;
        final double ad = a * d;
        final double bb = b * b;
        final double bc = b * c;
        final double bd = b * d;
        final double cc = c * c;
        final double cd = c * d;
        final double dd = d * d;

        forward(i, x, y, z, ab, ac, ad, bb, bc, bd, cc, cd, dd);
    }

    static void inverse(Quaternion q, double[] x, double[] y, double[] z, int i) {
        final double a = q.getR();
        final double b = q.getI();
        final double c = q.getJ();
        final double d = q.getK();

        final double ab = a * b;
        final double ac = a * c;
        final double ad = a * d;
        final double bb = b * b;
        final double bc = b * c;
        final double bd = b * d;
        final double cc = c * c;
        final double cd = c * d;
        final double dd = d * d;

        inverse(i, x, y, z, ab, ac, ad, bb, bc, bd, cc, cd, dd);
    }

    private static void forward(int i, double[] x, double[] y, double[] z,
                                double ab, double ac, double ad,
                                double bb, double bc, double bd,
                                double cc, double cd, double dd) {
        final double u;
        final double v;
        final double w;

        u = 2.0 * ((bc - ad) * y[i] + (ac + bd) * z[i] - (cc + dd) * x[i]) + x[i];
        v = 2.0 * ((ad + bc) * x[i] - (bb + dd) * y[i] + (cd - ab) * z[i]) + y[i];
        w = 2.0 * ((bd - ac) * x[i] + (ab + cd) * y[i] - (bb + cc) * z[i]) + z[i];

        x[i] = u;
        y[i] = v;
        z[i] = w;
    }

    private static void inverse(int i, double[] x, double[] y, double[] z,
                                double ab, double ac, double ad,
                                double bb, double bc, double bd,
                                double cc, double cd, double dd) {
        final double u;
        final double v;
        final double w;

        u = 2.0 * ((bc + ad) * y[i] + (ac - bd) * z[i] - (cc + dd) * x[i]) + x[i];
        v = 2.0 * ((bc - ad) * x[i] - (bb + dd) * y[i] + (cd + ab) * z[i]) + y[i];
        w = 2.0 * ((bd + ac) * x[i] + (cd - ab) * y[i] - (bb + cc) * z[i]) + z[i];

        x[i] = u;
        y[i] = v;
        z[i] = w;
    }

}
