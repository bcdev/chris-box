package org.esa.beam.chris.operators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GcpTest {

    @Test
    public void testParseAltitude() {
        final double alt = GCP.parseAltitude("Madrid (alt[m] = 750)", 0.5);
        assertEquals(0.75, alt, 0.0);
    }
}
