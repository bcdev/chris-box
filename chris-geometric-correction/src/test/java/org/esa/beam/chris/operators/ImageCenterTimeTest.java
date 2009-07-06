package org.esa.beam.chris.operators;

import org.esa.beam.chris.operators.ImageCenterTime.ITCReader;

import java.io.InputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class ImageCenterTimeTest extends TestCase {

    public void testReadImageCenterTimes() throws IOException {
        final InputStream is = ImageCenterTimeTest.class.getResourceAsStream(
                "Pass2049.Barrax_13350_CHRIS_center_times_20030512_65534");
        
        ITCReader reader = new ImageCenterTime.ITCReader(is);
        String[] lastIctRecord = reader.getLastIctRecord();
        assertEquals("2003.132.11.26.05.341", lastIctRecord[0]);
        assertEquals("294944", lastIctRecord[1]);
        assertEquals("+G:294944", lastIctRecord[2]);
        assertEquals("106572018.95829985", lastIctRecord[3]);
        assertEquals("+G:294944", lastIctRecord[4]);
        assertEquals("106571922.30086310", lastIctRecord[5]);
        assertEquals("+G:294944", lastIctRecord[6]);
        assertEquals("106571971.53328174", lastIctRecord[7]);
        assertEquals("+G:294944", lastIctRecord[8]);
        assertEquals("106572020.76570037", lastIctRecord[9]);
        assertEquals("+G:294944", lastIctRecord[10]);
        assertEquals("106572069.99811901", lastIctRecord[11]);
        assertEquals("+G:294944", lastIctRecord[12]);
        assertEquals("106572119.23053764", lastIctRecord[13]);
        
        double[] ictValues = reader.getLastIctValues();
        ImageCenterTime imageCenterTime = ImageCenterTime.create(ictValues, 13);

        assertNotNull(imageCenterTime);
        assertEquals(getUT1(2452771.9711724636), imageCenterTime.ict1, 1.0E-10);
        assertEquals(getUT1(2452771.9717422836), imageCenterTime.ict2, 1.0E-10);
        assertEquals(getUT1(2452771.9723121030), imageCenterTime.ict3, 1.0E-10);
        assertEquals(getUT1(2452771.9728819225), imageCenterTime.ict4, 1.0E-10);
        assertEquals(getUT1(2452771.9734517424), imageCenterTime.ict5, 1.0E-10);
    }

    private static double getUT1(double jd) throws IOException {
        return jd + TimeCalculator.getInstance().deltaUT1(Conversions.jdToMJD(jd));
    }

}
