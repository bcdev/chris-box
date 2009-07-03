package org.esa.beam.chris.operators;

import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class EcefEciConverterTest {

    @Test
    public void ecefToEci() {
        // ECEF coordinates corresponding to Hamburger Sternwarte (WGS-84)
        // lon = 10.2414° E
        // lat = 53.4800° N
        // alt = 40.0 m
        final double ecefX = 3743300.458313003;
        final double ecefY = 676318.7711106260;
        final double ecefZ = 5102545.269331731;
        final double[] ecef = new double[]{ecefX, ecefY, ecefZ};

        final double[] eci = EcefEciConverter.ecefToEci(Math.PI / 2.0, ecef, ecef.clone());
        final double eciX = eci[0];
        final double eciY = eci[1];
        final double eciZ = eci[2];

        assertEquals(-ecefY, eciX, 5.0E-10);
        assertEquals(ecefX, eciY, 5.0E-10);
        assertEquals(ecefZ, eciZ, 0.0);
    }

}
