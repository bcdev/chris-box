package org.esa.beam.chris.ui;

import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.cluster.ProbabilityCalculatorFactory;

import javax.media.jai.Histogram;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.HistogramDescriptor;
import java.awt.image.RenderedImage;

class ClusterProperties {

    private final double[] brightnesses;
    private final double[] occurrences;

    ClusterProperties(RenderedImage classificationImage, EMCluster[] clusters, IndexFilter clusterFilter) {
        brightnesses = new double[clusters.length];
        occurrences = new double[clusters.length];

        final ProbabilityCalculator calculator =
                new ProbabilityCalculatorFactory().createProbabilityCalculator(clusters);
        final double[] sums = new double[clusters.length];

        for (final EMCluster cluster : clusters) {
            final double[] posteriors = new double[clusters.length];
            calculator.calculate(cluster.getMean(), posteriors, clusterFilter);

            // accumulate visual brightnesses for cluster means
            for (int k = 0; k < clusters.length; ++k) {
                brightnesses[k] += cluster.getMean(0) * posteriors[k];
                sums[k] += posteriors[k];
            }
        }
        // calculate mean visual brightnesses
        for (int k = 0; k < clusters.length; ++k) {
            if (sums[k] > 0.0) {
                brightnesses[k] /= sums[k];
            }
        }

        final RenderedOp op = HistogramDescriptor.create(classificationImage, null, 1, 1, new int[]{clusters.length},
                                                         new double[]{0.0}, new double[]{clusters.length}, null);
        final Histogram histogram = (Histogram) op.getProperty("histogram");
        final int totalCounts = classificationImage.getWidth() * classificationImage.getHeight();
        final int[] histogramCounts = histogram.getBins(0);

        for (int k = 0; k < clusters.length; ++k) {
            occurrences[k] = (double) histogramCounts[k] / (double) totalCounts;
        }
    }

    double getBrightness(int i) {
        return brightnesses[i];
    }

    double getOccurrence(int i) {
        return occurrences[i];
    }
}
