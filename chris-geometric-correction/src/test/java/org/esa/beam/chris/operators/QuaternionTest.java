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

        assertEquals(0.92387953, q.getR(), 0.5E-08);
        assertEquals(0.00000000, q.getI(), 0.0);
        assertEquals(0.38268343, q.getJ(), 0.5E-08);
        assertEquals(0.00000000, q.getK(), 0.0);
    }

    @Test
    public void addQuaternions() {
        final double a1 = 2.0;
        final double b1 = 3.0;
        final double c1 = 5.0;
        final double d1 = 7.0;
        final Quaternion q1 = new Quaternion(a1, b1, c1, d1);

        final double a2 = 11.0;
        final double b2 = 13.0;
        final double c2 = 17.0;
        final double d2 = 19.0;
        final Quaternion q2 = new Quaternion(a2, b2, c2, d2);

        final Quaternion q3 = q1.add(q2);
        assertSame(q1, q3);

        assertEquals(a1 + a2, q1.getR(), 0.0);
        assertEquals(b1 + b2, q1.getI(), 0.0);
        assertEquals(c1 + c2, q1.getJ(), 0.0);
        assertEquals(d1 + d2, q1.getK(), 0.0);
    }

    @Test
    public void multiplyQuaternions() {
        final double a1 = 2.0;
        final double b1 = 3.0;
        final double c1 = 5.0;
        final double d1 = 7.0;
        final Quaternion q1 = new Quaternion(a1, b1, c1, d1);

        final double a2 = 11.0;
        final double b2 = 13.0;
        final double c2 = 17.0;
        final double d2 = 19.0;
        final Quaternion q2 = new Quaternion(a2, b2, c2, d2);

        final Quaternion q3 = q1.multiply(q2);
        assertSame(q1, q3);

        assertEquals(a1 * a2 - b1 * b2 - c1 * c2 - d1 * d2, q1.getR(), 0.0);
        assertEquals(a1 * b2 + b1 * a2 + c1 * d2 - d1 * c2, q1.getI(), 0.0);
        assertEquals(a1 * c2 - b1 * d2 + c1 * a2 + d1 * b2, q1.getJ(), 0.0);
        assertEquals(a1 * d2 + b1 * c2 - c1 * b2 + d1 * a2, q1.getK(), 0.0);
    }

    @Test
    public void rotateVector() {
        final Quaternion q1 = Quaternion.createQuaternion(0.0, 0.0, 1.0, Math.toRadians(32.0));
        final Quaternion q2 = Quaternion.createQuaternion(1.0, 0.0, 0.0, Math.toRadians(116.0));
        q1.multiply(q2);

        final double[] v = {0.0, 1.0, 0.0};
        q1.rotateVector(v);

        assertEquals(+0.23230132, v[0], 0.5E-08);
        assertEquals(-0.37175982, v[1], 0.5E-08);
        assertEquals(+0.89879405, v[2], 0.5E-08);
    }
}
