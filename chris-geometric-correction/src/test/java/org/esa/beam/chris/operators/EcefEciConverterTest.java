package org.esa.beam.chris.operators;

import junit.framework.TestCase;

public class EcefEciConverterTest extends TestCase {

    public void testEcefToEci() {
        final double lon = 10.2414;
        final double lat = 53.4800;
        final double alt = 40.0;

        final double[] ecef = new double[3];

        Conversions.wgsToEcef(lon, lat, alt, ecef);

        double x = ecef[0];
        double y = ecef[1];
        double z = ecef[2];

        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("z = " + z);
        assertEquals(6364406.8, Math.sqrt(x * x + y * y + z * z), 0.1);

        final EcefEciConverter converter = new EcefEciConverter(Math.PI);
        final double[] eci = new double[3];
        converter.ecefToEci(ecef, eci);

        x = eci[0];
        y = eci[1];
        z = eci[2];

        System.out.println("x = " + x);
        System.out.println("y = " + y);
        System.out.println("z = " + z);
        assertEquals(6364406.8, Math.sqrt(x * x + y * y + z * z), 0.1);

        converter.ecefToEci(ecef, ecef);
        assertEquals(x, ecef[0]);
        assertEquals(y, ecef[1]);
        assertEquals(z, ecef[2]);
    }

}
