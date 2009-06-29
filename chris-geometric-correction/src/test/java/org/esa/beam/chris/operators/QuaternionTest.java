package org.esa.beam.chris.operators;

import junit.framework.TestCase;

public class QuaternionTest extends TestCase {

    public void testQuaternionConstruction() {
        final double x = 0.0;
        final double y = 1.0;
        final double z = 0.0;
        final double alpha = Math.PI / 4.0;

        final Quaternion q = Quaternion.createQuaternion(x, y, z, alpha);

        // expected values from IDL source code
        assertEquals(0.00000000, q.getI(), 0.0);
        assertEquals(0.38268343, q.getJ(), 0.5E-08);
        assertEquals(0.00000000, q.getK(), 0.0);
        assertEquals(0.92387953, q.getR(), 0.5E-08);
    }

}
