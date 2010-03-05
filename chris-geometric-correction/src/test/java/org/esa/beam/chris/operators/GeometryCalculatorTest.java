package org.esa.beam.chris.operators;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GeometryCalculatorTest {

    // settings for image CHRIS_BR_050709_56F4_41
    private static final int MODE = 1;
    private static final double TARGET_LON = -2.09;
    private static final double TARGET_LAT = 39.06;
    private static final double TARGET_ALT = 0.7;
    private static final int CHRONOLOGICAL_IMAGE_NUMBER = 2;

    private static final AcquisitionInfo ACQUISITION_INFO =
            new AcquisitionInfo(MODE, TARGET_LON, TARGET_LAT, TARGET_ALT, CHRONOLOGICAL_IMAGE_NUMBER);
    private static final double DELTA_GPS = 13.0;

    private IctDataRecord ictData;
    private List<GpsDataRecord> gpsData;
    private GCP[] gcps;

    @Before
    public void prepare() throws JDOMException, IOException, URISyntaxException {
        readIctData();
        readGpsData();
        readGcpData();
    }

    @Test
    public void verifyCalculation() { // calculation is verified for three arbitrary chosen points
        final GeometryCalculator calculator = new GeometryCalculator(ACQUISITION_INFO);
        calculator.calculate(ictData, gpsData, gcps, true);

        final double lon1 = -2.1467485;
        final double lat1 = 39.082634;
        final double vaa1 = 110.73890;
        final double vza1 = 12.794340;

        assertEquals(lon1, calculator.getLon(150, 31), 2.0E-07);
        assertEquals(lat1, calculator.getLat(150, 31), 5.0E-06);
        assertEquals(vaa1, calculator.getVaa(150, 31), 5.0E-06);
        assertEquals(vza1, calculator.getVza(150, 31), 5.0E-06);

        final double lon2 = -2.0291290;
        final double lat2 = 39.103485;
        final double vaa2 = 103.96323;
        final double vza2 = 11.660949;

        assertEquals(lon2, calculator.getLon(45, 301), 2.0E-07);
        assertEquals(lat2, calculator.getLat(45, 301), 5.0E-06);
        assertEquals(vaa2, calculator.getVaa(45, 301), 5.0E-06);
        assertEquals(vza2, calculator.getVza(45, 301), 5.0E-06);

        final double lon3 = -2.0862782;
        final double lat3 = 39.009377;
        final double vaa3 = 125.21822;
        final double vza3 = 12.938542;

        assertEquals(lon3, calculator.getLon(351, 210), 2.0E-07);
        assertEquals(lat3, calculator.getLat(351, 210), 5.0E-06);
        assertEquals(vaa3, calculator.getVaa(351, 210), 5.0E-06);
        assertEquals(vza3, calculator.getVza(351, 210), 5.0E-06);
    }

    private void readIctData() throws URISyntaxException {
        final URL url = getClass().getResource("Pass5180.Barrax_22260_CHRIS_center_times_20050709_65534");
        final URI uri = url.toURI();

        ictData = PerformGeometricCorrectionOp.readIctData(new File(uri), DELTA_GPS);
    }

    private void readGpsData() throws URISyntaxException {
        final URL url = getClass().getResource("CHRIS_22260_22264_PROBA1_GPS_Data");
        final URI uri = url.toURI();

        gpsData = PerformGeometricCorrectionOp.readGpsData(new File(uri), DELTA_GPS,
                                                           PerformGeometricCorrectionOp.DELAY);
    }

    private void readGcpData() throws JDOMException, IOException {
        final Document document = new SAXBuilder().build(getClass().getResourceAsStream("gcps.xml"));

        @SuppressWarnings({"unchecked"})
        final List<Element> placemarkElementList = document.getRootElement().getChildren("Placemark");
        final List<GCP> gcpList = new ArrayList<GCP>(placemarkElementList.size());

        for (final Element element : placemarkElementList) {
            final double x = Double.parseDouble(element.getChildText("PIXEL_X"));
            final double y = Double.parseDouble(element.getChildText("PIXEL_Y"));
            final double lon = Double.parseDouble(element.getChildText("LONGITUDE"));
            final double lat = Double.parseDouble(element.getChildText("LATITUDE"));
            gcpList.add(new GCP(x, y, lon, lat, TARGET_ALT));
        }

        gcps = gcpList.toArray(new GCP[gcpList.size()]);
    }
}
