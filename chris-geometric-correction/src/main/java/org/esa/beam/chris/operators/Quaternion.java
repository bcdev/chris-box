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
     * Creates a new quaternion from a unit vector defining an axis
     * of rotation and a rotation angle.
     *
     * @param x     the x-component of the rotation axis.
     * @param y     the y-component of the rotation axis.
     * @param z     the z-component of the rotation axis.
     * @param angle the rotation angle (rad).
     *
     * @return the quaternion created.
     */
    public static Quaternion createQuaternion(double x, double y, double z, double angle) {
        final double alpha = angle / 2.0;
        final double c = Math.cos(alpha);
        final double s = Math.sin(alpha);

        return new Quaternion(c, s * x, s * y, s * z);
    }

    /**
     * Creates an array of n quaternions from n unit vectors defining the
     * axes of rotation and a rotation angle.
     *
     * @param x     the x-components of the n unit vectors.
     * @param y     the y-components of the n unit vectors.
     * @param z     the z-components of the n unit vectors.
     * @param angle the rotation angle (rad).
     *
     * @return the n quaternions created.
     */
    public static Quaternion[] createQuaternions(double[] x, double[] y, double[] z, double angle) {
        ensureLegalArray(x, "x", 0);
        final int n = x.length;
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);

        final double alpha = angle / 2.0;
        final double c = Math.cos(alpha);
        final double s = Math.sin(alpha);

        final Quaternion[] quaternions = new Quaternion[n];
        for (int i = 0; i < n; i++) {
            quaternions[i] = new Quaternion(c, s * x[i], s * y[i], s * z[i]);
        }

        return quaternions;
    }

    /**
     * Creates an array of n quaternions from n unit vectors defining the
     * axes of rotation and n corresponding rotation angles.
     *
     * @param x      the x-components of the n unit vectors.
     * @param y      the y-components of the n unit vectors.
     * @param z      the z-components of the n unit vectors.
     * @param angles the n rotation angles (rad).
     *
     * @return the n quaternions created.
     */
    public static Quaternion[] createQuaternions(double[] x, double[] y, double[] z, double[] angles) {
        ensureLegalArray(x, "x", 0);
        final int n = x.length;
        ensureLegalArray(y, "y", n);
        ensureLegalArray(z, "z", n);
        ensureLegalArray(angles, "angles", n);

        final Quaternion[] quaternions = new Quaternion[n];
        for (int i = 0; i < n; i++) {
            final double alpha = angles[i] / 2.0;
            final double c = Math.cos(alpha);
            final double s = Math.sin(alpha);
            quaternions[i] = new Quaternion(c, s * x[i], s * y[i], s * z[i]);
        }

        return quaternions;
    }

    /**
     * Adds two quaternions and stores the result in a third quaternion.
     *
     * @param q1 the 1st quaternion.
     * @param q2 the 2nd quaternion.
     * @param q3 the 3rd quaternion which holds the result of {@code q1} multiplied with {@code q2}.
     *
     * @return the sum of {@code q1} and {@code q2}.
     */
    public static Quaternion add(Quaternion q1, Quaternion q2, Quaternion q3) {
        final double a = q1.a + q2.a;
        final double b = q1.b + q2.b;
        final double c = q1.c + q2.c;
        final double d = q1.d + q2.d;

        q3.a = a;
        q3.b = b;
        q3.c = c;
        q3.d = d;

        return q3;
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
     * Default constructor.
     */
    public Quaternion() {
        this.a = 0.0;
        this.b = 0.0;
        this.c = 0.0;
        this.d = 0.0;
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
     * Adds another quaternion to this quaternion.
     * <p/>
     * Note that the addition is carried out in place,  i.e. the
     * original components of this quaternion are set to the sum
     * of the addition.
     *
     * @param q the quaternion being added.
     *
     * @return the sum of both quaternions.
     */
    public final Quaternion add(Quaternion q) {
        return Quaternion.add(this, q, this);
    }

    /**
     * Multiplies this quaternion with another quaternion.
     * <p/>
     * Note that the multiplication is carried out in place,  i.e. the
     * original components of this quaternion are set to the result of
     * the mulitiplication.
     * <p/>
     * <em>The quaternion group is non-abelian, i.e., in general
     * {@code q1 * q2 != q2 * q1}, for any quaternions {@code q1}
     * and {@code q2}.</em>
     *
     * @param q the other quaternion.
     *
     * @return the Hamilton product of both quaternions.
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
