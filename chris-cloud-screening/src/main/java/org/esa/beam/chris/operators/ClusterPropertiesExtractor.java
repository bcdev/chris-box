package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.chris.util.OpUtils;

import java.io.IOException;

/**
 * User: Marco Peters
 * Date: 09.05.2008
 */
public class ClusterPropertiesExtractor {

    private final Product featureProduct;
    private final Product clusterMapProduct;

    /**
     * Constructs a new instance of this class.
     *
     * @param featureProduct    the feature product.
     * @param clusterMapProduct the cluster product.
     */
    public ClusterPropertiesExtractor(Product featureProduct, Product clusterMapProduct) {
        this.featureProduct = featureProduct;
        this.clusterMapProduct = clusterMapProduct;
    }

    public ClusterProperties extractClusterProperties(ProgressMonitor pm) throws IOException {
        final Band brightnessBand = featureProduct.getBand("brightness_vis");
        final Band clusterMapBand = clusterMapProduct.getBand("cluster_map");

        final int h = clusterMapBand.getRasterHeight();
        final int w = clusterMapBand.getRasterWidth();

        final Band[] probabilityBands = OpUtils.findBands(clusterMapProduct, "probability");

        final double[] brightnesses = new double[probabilityBands.length];
        final double[] occurrences = new double[probabilityBands.length];

        final int[] clusterMapPixels = new int[w];
        final double[] brightnessPixels = new double[w];
        for (int y = 0; y < h; ++y) {
            clusterMapBand.readPixels(0, y, w, 1, clusterMapPixels, pm);
            brightnessBand.readPixels(0, y, w, 1, brightnessPixels, pm);
            for (int x = 0; x < w; ++x) {
                final int clusterIndex = clusterMapPixels[x];
                if (clusterIndex >= 0) {
                    brightnesses[clusterIndex] += brightnessPixels[x];
                    ++occurrences[clusterIndex];
                }
            }
        }

        final int sampleCount = w * h;
        for (int k = 0; k < brightnesses.length; ++k) {
            if (occurrences[k] > 0) {
                brightnesses[k] /= occurrences[k];
                occurrences[k] /= sampleCount;
            }
        }

        return new ClusterProperties(brightnesses, occurrences);
    }
}
