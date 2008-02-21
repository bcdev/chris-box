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

/**
 * Cluster class.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 */
public class Cluster {

    private final int n;

    private final double[][] points;
    private final double p;
    private final double h[];
    private final Distribution pdf;

    /**
     * Constructs a new cluster.
     *
     * @param n      the dimension of the point space.
     * @param points the data points.
     * @param p      the cluster prior probability.
     * @param h      the cluster posterior probabilities.
     * @param pdf    the cluster probability density function.
     */
    public Cluster(int n, double[][] points, double p, double[] h, Distribution pdf) {
        // todo - check arguments

        this.n = n;
        this.points = points;
        this.p = p;
        this.h = h;
        this.pdf = pdf;
    }

    /**
     * Returns the data points.
     *
     * @return the data point.
     */
    public final double[][] getPoints() {
        return points;
    }

    /**
     * Returns the cluster posterior probabilities.
     *
     * @return the cluster posterior probabilities.
     */
    public final double[] getPosteriorProbabilities() {
        return h;
    }

    /**
     * Returns the cluster prior probability.
     *
     * @return the cluster prior probability.
     */
    public final double getPriorProbability() {
        return p;
    }

    /**
     * Returns the cluster probability density for a data point.
     *
     * @param point the data point.
     * @return the cluster probability density.
     */
    public final double probabilityDensity(double[] point) {
        if (point.length != n) {
            throw new IllegalArgumentException("point.length != n");
        }

        return pdf.probabilityDensity(point);
    }

    /**
     * Returns the cluster mean.
     *
     * @return the cluster mean.
     */
    public double[] getMean() {
        return pdf.getMean();
    }
}
