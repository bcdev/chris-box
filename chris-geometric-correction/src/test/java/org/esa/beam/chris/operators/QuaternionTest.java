package org.esa.beam.chris.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class QuaternionTest {

    /**
     * Test based on IDL source code.
     */
    @Test
    public void createQuaternion() {
        final double x = 0.0;
        final double y = 1.0;
        final double z = 0.0;
        final double angle = Math.PI / 4.0;

        final Quaternion q = Quaternion.createQuaternion(x, y, z, angle);

        final double a = q.getR();
        final double b = q.getI();
        final double c = q.getJ();
        final double d = q.getK();

        assertEquals(1.0, Quaternion.norm4(a, b, c, d), 2.0E-16);

        assertEquals(0.92387953, a, 0.5E-08);
        assertEquals(0.00000000, b, 0.0);
        assertEquals(0.38268343, c, 0.5E-08);
        assertEquals(0.00000000, d, 0.0);
    }

    @Test
    public void multiplyQuaternions() {
        final Quaternion q1 = Quaternion.createQuaternion(2.0, 3.0, 4.0, Math.toRadians(47.0));
        final double a1 = q1.getR();
        final double b1 = q1.getI();
        final double c1 = q1.getJ();
        final double d1 = q1.getK();

        assertEquals(1.0, Quaternion.norm4(a1, b1, c1, d1), 2.0E-16);

        final Quaternion q2 = Quaternion.createQuaternion(11.0, 13.0, 17.0, Math.toRadians(19.0));
        final double a2 = q2.getR();
        final double b2 = q2.getI();
        final double c2 = q2.getJ();
        final double d2 = q2.getK();

        assertEquals(1.0, Quaternion.norm4(a2, b2, c2, d2), 2.0E-16);

        final Quaternion q3 = Quaternion.multiply(q1, q2);
        final double a3 = q3.getR();
        final double b3 = q3.getI();
        final double c3 = q3.getJ();
        final double d3 = q3.getK();

        assertEquals(1.0, Quaternion.norm4(a3, b3, c3, d3), 2.0E-16);

        assertEquals(a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2, a3, 0.5E-15);
        assertEquals(a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2, b3, 0.5E-15);
        assertEquals(a1 * c2 - b1 * d2 + c1 * a2 + d1 * b2, c3, 0.5E-15);
        assertEquals(a1 * d2 + b1 * c2 - c1 * b2 + d1 * a2, d3, 0.5E-15);
    }

    /**
     * Test based on IDL source code.
     */
    @Test
    public void transform() {
        final Quaternion q1 = Quaternion.createQuaternion(0.0, 0.0, 1.0, Math.toRadians(32.0));
        final Quaternion q2 = Quaternion.createQuaternion(1.0, 0.0, 0.0, Math.toRadians(116.0));
        final Quaternion q3 = Quaternion.multiply(q1, q2);

        final double[] v = {0.0, 1.0, 0.0};
        q3.transform(v, v);

        // expected values
        final double x = 0.23230132;
        final double y = -0.37175982;
        final double z = 0.89879405;

        assertEquals(x, v[0], 0.5E-08);
        assertEquals(y, v[1], 0.5E-08);
        assertEquals(z, v[2], 0.5E-08);
    }

    @Test
    public void inverseTransform() {
        final Quaternion q1 = Quaternion.createQuaternion(0.0, 0.0, 1.0, Math.toRadians(32.0));
        final Quaternion q2 = Quaternion.createQuaternion(1.0, 0.0, 0.0, Math.toRadians(116.0));
        final Quaternion q3 = Quaternion.multiply(q1, q2);

        final double x = 0.23230132;
        final double y = -0.37175982;
        final double z = 0.89879405;
        final double[] v = {x, y, z};
        q3.inverseTransform(v, v);

        assertEquals(0.0, v[0], 1.0E-08);
        assertEquals(1.0, v[1], 1.0E-08);
        assertEquals(0.0, v[2], 1.0E-08);
    }

    public void identityTransform() {
        final Quaternion q = new Quaternion();

        final double[] u = {1.0, 2.0, 3.0};
        final double[] v = {0.0, 0.0, 0.0};
        q.transform(u, v);

        assertEquals(1.0, v[0], 0.5E-08);
        assertEquals(2.0, v[1], 0.5E-08);
        assertEquals(3.0, v[2], 0.5E-08);

        final double[] w = {0.0, 0.0, 0.0};
        q.inverseTransform(v, w);
        assertEquals(1.0, w[0], 0.5E-08);
        assertEquals(2.0, w[1], 0.5E-08);
        assertEquals(3.0, w[2], 0.5E-08);
    }
    
    @Test
    public void norm4() {
        assertEquals(Math.sqrt(86.0), Quaternion.norm4(3.0, 4.0, 5.0, 6.0), 0.0);
    }
}
