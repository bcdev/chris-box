package org.esa.beam.chris.operators;

import java.text.MessageFormat;

/**
 * Quaternion class.
 * <p/>
 * See http://www.wikipedia.org/wiki/Quaternions for an explanation of
 * Quaternions.
 */
class Quaternion {

    private double a;
    private double b;
    private double c;
    private double d;

    /**
     * Creates a new quaternion from a rotation axis and a rotation angle.
     *
     * @param x     the x-component of the rotation axis.
     * @param y     the y-component of the rotation axis.
     * @param z     the z-component of the rotation axis.
     * @param alpha the rotation angle.
     *
     * @return the quaternion created.
     */
    public static Quaternion createQuaternion(double x, double y, double z, double alpha) {
        final double c = Math.cos(alpha / 2.0);
        final double s = Math.sin(alpha / 2.0);

        return new Quaternion(c, s * x, s * y, s * z);
    }

    /**
     * Creates an array of N quaternions from N rotation axes and a single rotation angle.
     *
     * @param x     the x-components of the N rotation axes.
     * @param y     the y-components of the N rotation axes.
     * @param z     the z-components of the N rotation axes.
     * @param alpha the rotation angle.
     *
     * @return the N quaternions created.
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
     * Creates an array of N quaternions from N rotation axes and N associated rotation angles.
     *
     * @param x     the x-components of the N rotation axes.
     * @param y     the y-components of the N rotation axes.
     * @param z     the z-components of the N rotation axes.
     * @param alpha the N rotation angles.
     *
     * @return the N quaternions created.
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
     * Multiplies two quaternions and stores the result in a third quaternion.
     *
     * @param q1 the 1st quaternion.
     * @param q2 the 2nd quaternion.
     * @param q3 the 3rd quaternion which holds the result of {@code q1} multiplied with {@code q2}.
     *
     * @return the Hamilton product of {@code q1} and {@code q2}.
     */
    public static Quaternion multiply(Quaternion q1, Quaternion q2, Quaternion q3) {
        final double a = q1.a * q2.a - q1.b * q2.b - q1.c * q2.c - q1.d * q2.d;
        final double b = q1.a * q2.b + q1.b * q2.a + q1.c * q2.d - q1.d * q2.c;
        final double c = q1.a * q2.c - q1.b * q2.d + q1.c * q2.a + q1.d * q2.b;
        final double d = q1.a * q2.d + q1.b * q2.c - q1.c * q2.b + q1.d * q2.a;

        q3.a = a;
        q3.b = b;
        q3.c = c;
        q3.d = d;

        return q3;
    }

    /**
     * Constructs a new quaternion.
     *
     * @param a the scalar part of the quaternion.
     * @param b the i-component of the vector part of the quaternion.
     * @param c the j-component of the vector part of the quaternion.
     * @param d the k-component of the vector part of the quaternion.
     */
    public Quaternion(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /**
     * Ccopy constructor.
     *
     * @param q the quaternion being copied.
     */
    public Quaternion(Quaternion q) {
        this.a = q.a;
        this.b = q.b;
        this.c = q.c;
        this.d = q.d;
    }

    /**
     * Returns the real (or scalar) part of the quaternion.
     *
     * @return the real (or scalar) part of the quaternion.
     */
    public final double getR() {
        return a;
    }

    /**
     * Returns the i-component of the vector part of the quaternion.
     *
     * @return the i-component of the vector part of the quaternion.
     */
    public final double getI() {
        return b;
    }

    /**
     * Returns the j-component of the vector part of the quaternion.
     *
     * @return the j-component of the vector part of the quaternion.
     */
    public final double getJ() {
        return c;
    }

    /**
     * Returns the k-component of the vector part of the quaternion.
     *
     * @return the k-component of the vector part of the quaternion.
     */
    public final double getK() {
        return d;
    }

    /**
     * Multiplies this quaternion with another quaternion.
     * <p/>
     * Note that the multiplication is carried out in place,  i.e. the
     * original components of this quaternion are set to the result of
     * the mulitiplication.
     *
     * @param q the other quaternion.
     *
     * @return the Hamilton product of this quaternion and the other quaternion.
     */
    public final Quaternion multiply(Quaternion q) {
        return Quaternion.multiply(this, q, this);
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} + i * {1} + j * {2} + k * {3}", a, b, c, d);
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
