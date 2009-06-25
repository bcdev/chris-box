package org.esa.beam.chris.operators;

import java.io.InputStream;

import junit.framework.TestCase;

public class ImageCenterTimeTest extends TestCase {

    public void testReadImageCenterTimes() {
        final InputStream is = ImageCenterTimeTest.class.getResourceAsStream(
                "Pass2049.Barrax_13350_CHRIS_center_times_20030512_65534");
        ImageCenterTime imageCenterTime = ImageCenterTime.read(is, 13);

        assertNotNull(imageCenterTime);
        assertEquals(3098881.4711724636, imageCenterTime.ict1, 0.000001);
        assertEquals(3098881.4717422836, imageCenterTime.ict2, 0.000001);
        assertEquals(3098881.472312103, imageCenterTime.ict3, 0.000001);
        assertEquals(3098881.4728819225, imageCenterTime.ict4, 0.000001);
        assertEquals(3098881.4734517424, imageCenterTime.ict5, 0.000001);
    }
}
