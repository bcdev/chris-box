package org.esa.beam.chris.operators;

import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

import java.util.ArrayList;
import java.util.List;

class GCP {

    private final double x;
    private final double y;
    private final double lon;
    private final double lat;
    private final double alt;

    GCP(Placemark placemark) {
        this(placemark, 0.0);
    }

    GCP(Placemark placemark, double defaultAltitude) {
        x = placemark.getPixelPos().getX();
        y = placemark.getPixelPos().getY();
        lon = placemark.getGeoPos().getLon();
        lat = placemark.getGeoPos().getLat();
        alt = parseAltitude(placemark.getDescription(), defaultAltitude);
    }

    int getCol() {
        return (int) x;
    }

    int getRow() {
        return (int) y;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    double getLon() {
        return lon;
    }

    double getLat() {
        return lat;
    }

    double getAlt() {
        return alt;
    }

    static GCP[] toGCPs(ProductNodeGroup<Placemark> placemarkGroup, double defaultAltitude) {
        final List<GCP> gcpList = new ArrayList<GCP>(placemarkGroup.getNodeCount());
        for (Placemark placemark : placemarkGroup.toArray(new Placemark[placemarkGroup.getNodeCount()])) {
            if (isValid(placemark)) {
                gcpList.add(new GCP(placemark, defaultAltitude));
            }
        }
        return gcpList.toArray(new GCP[gcpList.size()]);
    }

    static double parseAltitude(String description, double defaultAltitude) {
        final int i = description.indexOf("(alt");
        if (i != -1) {
            final int k = description.indexOf("=", i);
            if (k != -1) {
                final int l = description.indexOf(")", k);
                try {
                    double alt = Double.parseDouble(description.substring(k + 1, l));
                    if (alt > 10) {
                        // altitude is given in meter
                        alt /= 1000.0;
                    }
                    return alt;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return defaultAltitude;
    }

    private static boolean isValid(Placemark placemark) {
        return placemark.getPixelPos() != null
               && placemark.getPixelPos().isValid()
               && placemark.getGeoPos() != null
               && placemark.getGeoPos().isValid();
    }
}
