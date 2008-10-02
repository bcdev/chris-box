package org.esa.beam.chris.ui;

import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.cluster.ProbabilityCalculatorFactory;

class ClusterProperties {

    private final double[] brightnesses;
    private final double[] occurrences;

    ClusterProperties(EMCluster[] clusters, IndexFilter clusterFilter) {
        brightnesses = new double[clusters.length];
        occurrences = new double[clusters.length];

        final ProbabilityCalculator calculator =
                new ProbabilityCalculatorFactory().createProbabilityCalculator(clusters);
        final double[] sums = new double[clusters.length];

        for (final EMCluster cluster : clusters) {
            final double[] posteriors = new double[clusters.length];
            calculator.calculate(cluster.getMean(), posteriors, clusterFilter);

            // accumulate prior probabilities and visual brightnesses for cluster mean
            for (int k = 0; k < clusters.length; ++k) {
                occurrences[k] += cluster.getPriorProbability() * posteriors[k];
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
    }

    double getBrightness(int i) {
        return brightnesses[i];
    }

    double getOccurrence(int i) {
        return occurrences[i];
    }
}
