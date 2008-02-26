/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.chris.operators.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.unmixing.Endmember;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * todo - API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EndmemberExtractor {
    private final Product reflectanceProduct;
    private final Product featureProduct;
    private final Product clusterProduct;

    private final int[] cloudClusterIndexes;
    private final int[] surfaceClusterIndexes;

    public EndmemberExtractor(Product reflectanceProduct, Product featureProduct, Product clusterProduct,
                              int[] cloudClusterIndexes, int[] surfaceClusterIndexes) {

        this.reflectanceProduct = reflectanceProduct;
        this.featureProduct = featureProduct;
        this.clusterProduct = clusterProduct;

        this.cloudClusterIndexes = cloudClusterIndexes;
        this.surfaceClusterIndexes = surfaceClusterIndexes;
    }

    public Endmember[] calculateEndmembers() throws IOException {
        final Band brightnessBand = featureProduct.getBand("brightness_vis");
        final Band whitenessBand = featureProduct.getBand("whiteness_vis");
        final Band membershipBand = clusterProduct.getBand("membership_mask");

        final int h = membershipBand.getRasterHeight();
        final int w = membershipBand.getRasterWidth();

        int maxX = -1;
        int maxY = -1;
        double maxBrightnessOverWhiteness = 0.0;

        for (int y = 0; y < h; ++y) {
            final int[] memberships = membershipBand.readPixels(0, y, w, 1, new int[w]);
            final double[] brightnesses = brightnessBand.readPixels(0, y, w, 1, new double[w]);
            final double[] whitenesses = whitenessBand.readPixels(0, y, w, 1, new double[w]);

            for (int x = 0; x < w; ++x) {
                if (isContained(memberships[x], cloudClusterIndexes)) {
                    if (whitenesses[x] > 0.0) {
                        final double brightnessOverWhiteness = brightnesses[x] / whitenesses[x];

                        if (maxX == -1 || maxY == -1 || brightnessOverWhiteness > maxBrightnessOverWhiteness) {
                            maxX = x;
                            maxY = y;
                            maxBrightnessOverWhiteness = brightnessOverWhiteness;
                        }
                    }
                }
            }
        }

        final Band[] reflectanceBands = findBands(reflectanceProduct, "reflectance");

        final double[] wavelengths = new double[reflectanceBands.length];
        final double[] reflectances = new double[reflectanceBands.length];

        for (int i = 0; i < reflectanceBands.length; ++i) {
            wavelengths[i] = reflectanceBands[i].getSpectralWavelength();
            reflectances[i] = readPixel(maxX, maxY, reflectanceBands[i]);
        }

        final Band[] clusterProbBands = findBands(clusterProduct, "probability");

        final Endmember[] endmembers = new Endmember[surfaceClusterIndexes.length + 1];
        endmembers[0] = new Endmember("Cloud", wavelengths, reflectances);

        ///////////////////////
        double[][] meanRefl = new double[clusterProbBands.length][reflectanceBands.length];
        int[] count = new int[clusterProbBands.length];
        for (int y = 0; y < h; ++y) {
            final double[][] reflectances2 = new double[reflectanceBands.length][w];
            for (int i = 0; i < reflectances2.length; i++) {
                reflectanceBands[i].readPixels(0, y, w, 1, reflectances2[i]);
            }
            for (int clusterId = 0; clusterId < clusterProbBands.length; clusterId++) {
                if (isContained(clusterId, surfaceClusterIndexes)) {
                    final double[] probs = clusterProbBands[clusterId].readPixels(0, y, w, 1, new double[w]);
                    for (int x = 0; x < w; ++x) {
                        if (probs[x] > 0.5) {
                            for (int reflId = 0; reflId < reflectanceBands.length; reflId++) {
                                meanRefl[clusterId][reflId] += reflectances2[reflId][x];
                            }
                            count[clusterId]++;
                        }
                    }
                }
            }
        }

        for (int clusterId = 0, j = 1; clusterId < clusterProbBands.length; clusterId++) {
            if (count[clusterId] > 0) {
                for (int reflId = 0; reflId < reflectanceBands.length; reflId++) {
                    meanRefl[clusterId][reflId] /= count[clusterId];
                }
                endmembers[j] = new Endmember(Integer.toString(j), wavelengths, meanRefl[clusterId]);
                ++j;
            }
        }

        return endmembers;
    }

    private static boolean isContained(int index, int[] cloudClusterIndexes) {
        for (int i = 0; i < cloudClusterIndexes.length; ++i) {
            if (i == index) {
                return true;
            }
        }

        return false;
    }

    private static Band[] findBands(Product product, String prefix) {
        final List<Band> bandList = new ArrayList<Band>();

        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix)) {
                bandList.add(band);
            }
        }
        return bandList.toArray(new Band[bandList.size()]);
    }

    private static double readPixel(int maxX, int maxY, Band band) throws IOException {
        final double[] doubles = band.readPixels(maxX, maxY, 1, 1, new double[1]);
        return doubles[0];
    }
}
