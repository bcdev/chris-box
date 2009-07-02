package org.esa.beam.chris.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;


public class QuaternionRotationsTest {

    /**
     * Test based on IDL source code.
     */
    @Test
    public void rotateVectors() {
        final Quaternion q1 = Quaternion.createQuaternion(0.0, 0.0, 1.0, Math.toRadians(32.0));
        final Quaternion q2 = Quaternion.createQuaternion(1.0, 0.0, 0.0, Math.toRadians(116.0));

        final Quaternion q3 = q1.multiply(q2);
        assertSame(q1, q3);

        final double[] x = {1.0, 0.0, 0.0};
        final double[] y = {0.0, 1.0, 0.0};
        final double[] z = {0.0, 0.0, 1.0};

        QuaternionRotations.forward(q3, x, y, z, 0);
        QuaternionRotations.forward(q3, x, y, z, 1);
        QuaternionRotations.forward(q3, x, y, z, 2);

        assertEquals(0.84804810, x[0], 0.5E-08);
        assertEquals(0.23230132, x[1], 0.5E-08);
        assertEquals(0.47628828, x[2], 0.5E-08);
        assertEquals(0.52991926, y[0], 0.5E-08);
        assertEquals(-0.37175982, y[1], 0.5E-08);
        assertEquals(-0.76222058, y[2], 0.5E-08);
        assertEquals(0.0, z[0], 0.5E-08);
        assertEquals(0.89879405, z[1], 0.5E-08);
        assertEquals(-0.43837115, z[2], 0.5E-08);

        QuaternionRotations.inverse(q3, x, y, z, 0);
        QuaternionRotations.inverse(q3, x, y, z, 1);
        QuaternionRotations.inverse(q3, x, y, z, 2);

        assertEquals(1.0, x[0], 0.5E-08);
        assertEquals(0.0, x[1], 0.5E-08);
        assertEquals(0.0, x[2], 0.5E-08);
        assertEquals(0.0, y[0], 0.5E-08);
        assertEquals(1.0, y[1], 0.5E-08);
        assertEquals(0.0, y[2], 0.5E-08);
        assertEquals(0.0, z[0], 0.5E-08);
        assertEquals(0.0, z[1], 0.5E-08);
        assertEquals(1.0, z[2], 0.5E-08);
    }
}
