package org.esa.beam.chris.operators;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class IctReaderTest extends TestCase {

    public void testReadImageCenterTimes() {
        final InputStream is = IctReaderTest.class.getResourceAsStream(
                "Pass2049.Barrax_13350_CHRIS_center_times_20030512_65534");

        final IctReader ictReader = new IctReader(is);

        assertEquals(106571922.30086310, ictReader.getImageCenterTime(0), 0.0);
        assertEquals(106571971.53328174, ictReader.getImageCenterTime(1), 0.0);
        assertEquals(106572020.76570037, ictReader.getImageCenterTime(2), 0.0);
        assertEquals(106572069.99811901, ictReader.getImageCenterTime(3), 0.0);
        assertEquals(106572119.23053764, ictReader.getImageCenterTime(4), 0.0);
    }

    private static class IctReader {

        public IctReader(File file) throws FileNotFoundException {
            this(new FileInputStream(file));
        }

        IctReader(InputStream is) {
        }

        double getImageCenterTime(int i) {
            return 0.0;
        }
    }
}
