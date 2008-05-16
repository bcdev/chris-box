package org.esa.beam.chris.operators;

/**
 * User: Marco Peters
 * Date: 16.05.2008
 */
public class ClusterProperties {

    private final double[] brightnesses;
    private final double[] occurrences;

    public ClusterProperties(double[] brightnesses, double[] occurrences) {
        this.brightnesses = brightnesses;
        this.occurrences = occurrences;
    }

    public ClusterProperties(int count) {
        this(new double[count], new double[count]);
    }

    public double[] getBrightnesses() {
        return brightnesses;
    }

    public double[] getOccurrences() {
        return occurrences;
    }
}
