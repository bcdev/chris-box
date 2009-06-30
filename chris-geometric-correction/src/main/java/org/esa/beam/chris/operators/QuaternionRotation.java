package org.esa.beam.chris.operators;

import java.text.MessageFormat;

/**
 * Utility class for calculating 3-dimensional vector rotations
 * defined by unit quaternions.
 */
class QuaternionRotation {

    /**
     * Performs a rotation on n 3-dimensional vectors.
     *
     * @param q the unit-quaternion defining the rotation.
     * @param x the x-components of the n 3-dimensional vectors being rotated.
     * @param y the x-components of the n 3-dimensional vectors being rotated.
     * @param z the x-components of the n 3-dimensional vectors being rotated.
     */
    public static void rotateVectors(Quaternion q, double[] x, double[] y, double[] z) {
        ensureLegalArray(x, "x");
        final int n = x.length;
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);

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
     * @param q the n unit-quaternions, where the ith quaternion defines the
     *          rotation to be applied to the ith vector.
     * @param x the x-components of the n 3-dimensional vectors being rotated.
     * @param y the x-components of the n 3-dimensional vectors being rotated.
     * @param z the x-components of the n 3-dimensional vectors being rotated.
     */
    public static void rotateVectors(Quaternion[] q, double[] x, double[] y, double[] z) {
        ensureLegalArray(q, "q");
        final int n = q.length;
        ensureLegalArray(x, "x", n);
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);

        for (int i = 0; i < n; i++) {
            forward(q[i], x, y, z, i);
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

    private static <T> void ensureLegalArray(T[] a, String name) {
        if (a == null) {
            throw new IllegalArgumentException(MessageFormat.format("{0} == null", name));
        }
        if (a.length == 0) {
            throw new IllegalArgumentException(MessageFormat.format("{0}.length == 0", name));
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                throw new IllegalArgumentException(MessageFormat.format("{0}[{1}] == null", name, i));
            }
        }
    }

    private static void ensureLegalArray(double[] a, String name) {
        if (a == null) {
            throw new IllegalArgumentException(MessageFormat.format("{0} == null", name));
        }
        if (a.length == 0) {
            throw new IllegalArgumentException(MessageFormat.format("{0}.length == 0", name));
        }
    }

    private static void ensureLegalArray(double[] a, String name, int length) {
        ensureLegalArray(a, name);

        if (a.length != length) {
            throw new IllegalArgumentException(MessageFormat.format("{0}.length != {1}", name, length));
        }
    }
}
