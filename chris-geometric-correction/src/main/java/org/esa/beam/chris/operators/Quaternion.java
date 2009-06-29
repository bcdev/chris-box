package org.esa.beam.chris.operators;

import java.text.MessageFormat;

/**
 * Quaternion class.
 * <p/>
 * See http://www.wikipedia.org/wiki/Quaternions for an explanation of
 * Quaternions.
 */
class Quaternion {

    private final double r;
    private final double i;
    private final double j;
    private final double k;

    /**
     * Creates a new quaternion from a rotation axis and a rotation angle.
     *
     * @param x     the x-component of the rotation axis.
     * @param y     the y-component of the rotation axis.
     * @param z     the z-component of the rotation axis.
     * @param alpha the rotation angle.
     *
     * @return the quaternion.
     */
    public static Quaternion createQuaternion(double x, double y, double z, double alpha) {
        final double c = Math.cos(alpha / 2.0);
        final double s = Math.sin(alpha / 2.0);

        return new Quaternion(c, s * x, s * y, s * z);
    }

    /**
     * Creates an array of quaternions from n rotation axes and a single rotation angle.
     *
     * @param x     the x-components of the n rotation axes.
     * @param y     the y-components of the n rotation axes.
     * @param z     the z-components of the n rotation axes.
     * @param alpha the rotation angle.
     *
     * @return the quaternions.
     */
    public static Quaternion[] createQuaternions(double[] x, double[] y, double[] z, double alpha) {
        ensureLegalArray(x, "x", 0);
        final int n = x.length;
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);

        final double c = Math.cos(alpha / 2.0);
        final double s = Math.sin(alpha / 2.0);

        final Quaternion[] quaternions = new Quaternion[n];
        for (int i = 0; i < n; i++) {
            quaternions[i] = new Quaternion(c, s * x[i], s * y[i], s * z[i]);
        }

        return quaternions;
    }

    /**
     * Creates an array of quaternions from n rotation axes and n associated rotation angles.
     *
     * @param x     the x-components of the n rotation axes.
     * @param y     the y-components of the n rotation axes.
     * @param z     the z-components of the n rotation axes.
     * @param alpha the n rotation angles assosicated with the n rotation axes.
     *
     * @return the quaternions.
     */
    public static Quaternion[] createQuaternions(double[] x, double[] y, double[] z, double[] alpha) {
        ensureLegalArray(x, "x", 0);
        final int n = x.length;
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);
        ensureLegalArray(alpha, "alpha", n);

        final Quaternion[] quaternions = new Quaternion[n];
        for (int i = 0; i < n; i++) {
            final double c = Math.cos(alpha[i] / 2.0);
            final double s = Math.sin(alpha[i] / 2.0);
            quaternions[i] = new Quaternion(c, s * x[i], s * y[i], s * z[i]);
        }

        return quaternions;
    }

    /**
     * Constructs a new quaternion.
     *
     * @param r the scalar part of the quaternion.
     * @param i the i-component of the vector part of the quaternion.
     * @param j the j-component of the vector part of the quaternion.
     * @param k the k-component of the vector part of the quaternion.
     */
    public Quaternion(double r, double i, double j, double k) {
        this.r = r;
        this.i = i;
        this.j = j;
        this.k = k;
    }

    /**
     * Returns the scalar part of the quaternion.
     *
     * @return the scalar part of the quaternion.
     */
    public final double getR() {
        return r;
    }

    /**
     * Returns the i-component of the vector part of the quaternion.
     *
     * @return the i-component of the vector part of the quaternion.
     */
    public final double getI() {
        return i;
    }

    /**
     * Returns the j-component of the vector part of the quaternion.
     *
     * @return the j-component of the vector part of the quaternion.
     */
    public final double getJ() {
        return j;
    }

    /**
     * Returns the k-component of the vector part of the quaternion.
     *
     * @return the k-component of the vector part of the quaternion.
     */
    public final double getK() {
        return k;
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
