package org.esa.beam.chris.operators;

import com.bc.ceres.core.Assert;

import java.text.MessageFormat;

/**
 * Class for rotating 3-dimensional vectors by means of quaternions.
 * <p/>
 * See http://www.wikipedia.org/wiki/Quaternions for an explanation of
 * Quaternions.
 */
class Quaternion {

    private final double a;
    private final double b;
    private final double c;
    private final double d;

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

        return new Quaternion(x, y, z, c, s);
    }

    /**
     * Returns the Hamiltonian product of two quaternions.
     *
     * @param q1 the 1st quaternion.
     * @param q2 the 2nd quaternion.
     *
     * @return the Hamilton product of {@code q1} and {@code q2}.
     */
    public static Quaternion multiply(Quaternion q1, Quaternion q2) {
        Assert.notNull(q1);
        Assert.notNull(q2);

        final double a = q1.a * q2.a - q1.b * q2.b - q1.c * q2.c - q1.d * q2.d;
        final double b = q1.a * q2.b + q1.b * q2.a + q1.c * q2.d - q1.d * q2.c;
        final double c = q1.a * q2.c - q1.b * q2.d + q1.c * q2.a + q1.d * q2.b;
        final double d = q1.a * q2.d + q1.b * q2.c - q1.c * q2.b + q1.d * q2.a;

        return new Quaternion(a, b, c, d);
    }

    /**
     * Transforms a 3-dimensional vector by the rotation defined by this quaternion.
     *
     * @param u the original vector.
     * @param v the rotated vector. It is legal that {@code v} and {@code u} refer
     *          to the same object.
     *
     * @return the rotated vector.
     */
    public double[] transform(double[] u, double[] v) {
        Assert.notNull(u);
        Assert.argument(u.length == 3);

        if (v != u) {
            Assert.notNull(v);
            Assert.argument(v.length == 3);
        }

        final double ab = a * b;
        final double ac = a * c;
        final double ad = a * d;
        final double bb = b * b;
        final double bc = b * c;
        final double bd = b * d;
        final double cc = c * c;
        final double cd = c * d;
        final double dd = d * d;

        final double x = u[0];
        final double y = u[1];
        final double z = u[2];

        v[0] = 2.0 * ((bc - ad) * y + (ac + bd) * z - (cc + dd) * x) + x;
        v[1] = 2.0 * ((ad + bc) * x - (bb + dd) * y + (cd - ab) * z) + y;
        v[2] = 2.0 * ((bd - ac) * x + (ab + cd) * y - (bb + cc) * z) + z;

        return v;
    }

    /**
     * Transforms a 3-dimensional vector by the inverse of the rotation defined by
     * this quaternion.
     *
     * @param u the original vector.
     * @param v the rotated vector. It is legal that {@code v} and {@code u} refer
     *          to the same object.
     *
     * @return the rotated vector.
     */
    public double[] inverseTransform(double[] u, double[] v) {
        Assert.notNull(u);
        Assert.argument(u.length == 3);

        if (v != u) {
            Assert.notNull(v);
            Assert.argument(v.length == 3);
        }

        final double ab = a * b;
        final double ac = a * c;
        final double ad = a * d;
        final double bb = b * b;
        final double bc = b * c;
        final double bd = b * d;
        final double cc = c * c;
        final double cd = c * d;
        final double dd = d * d;

        final double x = u[0];
        final double y = u[1];
        final double z = u[2];

        v[0] = 2.0 * ((bc + ad) * y + (ac - bd) * z - (cc + dd) * x) + x;
        v[1] = 2.0 * ((bc - ad) * x - (bb + dd) * y + (cd + ab) * z) + y;
        v[2] = 2.0 * ((bd + ac) * x + (cd - ab) * y - (bb + cc) * z) + z;

        return v;
    }

    /**
     * Default constructor.
     * <p/>
     * The rotation defined by the quaternion constructed corresponds to the
     * identity transform.
     */
    public Quaternion() {
        this(1.0, 0.0, 0.0, 0.0);
    }

    /**
     * Copy constructor.
     *
     * @param q the quaternion being copied.
     */
    public Quaternion(Quaternion q) {
        Assert.notNull(q);

        this.a = q.a;
        this.b = q.b;
        this.c = q.c;
        this.d = q.d;
    }

    /**
     * Constructs a new unit quaternion.
     *
     * @param a the scalar part of the quaternion.
     * @param b the i-component of the vector part of the quaternion.
     * @param c the j-component of the vector part of the quaternion.
     * @param d the k-component of the vector part of the quaternion.
     */
    private Quaternion(double a, double b, double c, double d) {
        final double norm = norm4(a, b, c, d);

        this.a = a / norm;
        this.b = b / norm;
        this.c = c / norm;
        this.d = d / norm;
    }

    /**
     * Constructs a new unit quaternion.
     *
     * @param x the x-component of the rotation axis.
     * @param y the y-component of the rotation axis.
     * @param z the z-component of the rotation axis.
     * @param c the cosine of the rotation angle.
     * @param s the sine of the rotation angle.
     */
    private Quaternion(double x, double y, double z, double c, double s) {
        this(c, s * x, s * y, s * z);
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

    @Override
    public String toString() {
        return MessageFormat.format("{0} + i * {1} + j * {2} + k * {3}", a, b, c, d);
    }

    static double norm4(double a, double b, double c, double d) {
        final double aa = a * a;
        final double bb = b * b;
        final double cc = c * c;
        final double dd = d * d;

        return Math.sqrt(aa + bb + cc + dd);
    }
}
