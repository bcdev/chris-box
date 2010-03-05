/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.chris.operators;


/**
 * Some vector math.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class VectorMath {

    // constants used for array indexes, so the code is more readable.
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /**
     * Calculates the angle between two vectors.
     *
     * @param x1 the x-coordinate of the 1st vector.
     * @param y1 the y-coordinate of the 1st vector.
     * @param z1 the z-coordinate of the 1st vector.
     * @param x2 the x-coordinate of the 2nd vector.
     * @param y2 the y-coordinate of the 2nd vector.
     * @param z2 the z-coordinate of the 2nd vector.
     *
     * @return the angle (radian).
     */
    static double angle(double x1, double y1, double z1, double x2, double y2, double z2) {
        // compute the scalar product
        final double cs = x1 * x2 + y1 * y2 + z1 * z2;
        // compute the vector product
        final double u = y1 * z2 - z1 * y2;
        final double v = z1 * x2 - x1 * z2;
        final double w = x1 * y2 - y1 * x2;

        final double sn = Math.sqrt(u * u + v * v + w * w);
        return polarAngle(cs, sn);
    }

    private static double polarAngle(double x, double y) {
        double angle = 0.0;
        if (x != 0 && y != 0) {
            angle = Math.atan2(y, x);
        }
        if (angle < 0.0) {
            angle = angle + 2.0 * Math.PI;
        }
        return angle;
    }

    /**
     * Given a series satellite positions in ECI and the corresponding times
     * this routine computes the angular velocities.
     * <p/>
     * The result is an array with the angular velocities calculated from
     * consecutive points of the trajectory. The last element is the mean
     * angular velocity calculated from the last and the first position.
     *
     * @param t the trajectory's times (seconds).
     * @param x the trajectory's x coordinates.
     * @param y the trajectory's y coordinates.
     * @param z the trajectory's z coordinates.
     *
     * @return the angular velocities (radian).
     */
    static double[] angularVelocities(double[] t, double[] x, double[] y, double[] z) {
        final double[] w = new double[t.length];
        for (int i = 0; i < t.length; i++) {
            final double a;
            final double b;
            if (i == t.length - 1) {
                a = angle(x[0], y[0], z[0], x[i], y[i], z[i]);
                b = t[i] - t[0];
            } else {
                a = angle(x[i], y[i], z[i], x[i + 1], y[i + 1], z[i + 1]);
                b = t[i + 1] - t[i];
            }
            w[i] = a / b;
        }

        return w;
    }

    /**
     * Returns the transpose of a given matrix.
     *
     * @param matrix the matrix to be transposed.
     *
     * @return the transposed matrix.
     */
    static double[][] transpose(double[][] matrix) {
        final double[][] transposedMatrix = new double[matrix[0].length][matrix.length];
        for (int k = 0; k < transposedMatrix.length; ++k) {
            final double[] components = transposedMatrix[k];
            for (int i = 0; i < matrix.length; i++) {
                components[i] = matrix[i][k];
            }
        }
        return transposedMatrix;
    }

    /**
     * Normalizes a given vector to unit-length.
     *
     * @param vector the vector; on return the vector is normalized.
     *
     * @return the normalized vector.
     */
    static double[] unitVector(double[] vector) {
        final double norm = Math.sqrt(vector[X] * vector[X] + vector[Y] * vector[Y] + vector[Z] * vector[Z]);
        vector[X] /= norm;
        vector[Y] /= norm;
        vector[Z] /= norm;
        return vector;
    }

    /**
     * Normalizes the given vectors to unit-length.
     *
     * @param vectors the vectors; on return the vectors are normalized.
     *
     * @return the normalized vectors.
     */
    static double[][] unitVectors(double[][] vectors) {
        for (final double[] vector : vectors) {
            unitVector(vector);
        }
        return vectors;
    }

    /**
     * Calculates the vector product of two vectors.
     *
     * @param u the 1st vector.
     * @param v the 2nd vector.
     * @param w the vector holding the resulting vector product.
     *
     * @return the vector product of the 1st and the 2nd vector.
     */
    static double[] vectorProduct(double[] u, double[] v, double[] w) {
        w[X] = u[Y] * v[Z] - u[Z] * v[Y];
        w[Y] = u[Z] * v[X] - u[X] * v[Z];
        w[Z] = u[X] * v[Y] - u[Y] * v[X];
        return w;
    }

    /**
     * Calculates the vector product of two vector sequences.
     *
     * @param u the 1st vector sequence.
     * @param v the 2nd vector sequence.
     * @param w the vector sequence holding the resulting vector products.
     *
     * @return the vector products of the 1st and the 2nd vector sequence.
     */
    static double[][] vectorProducts(double[][] u, double[][] v, double[][] w) {
        for (int i = 0; i < u.length; i++) {
            vectorProduct(u[i], v[i], w[i]);
        }
        return w;
    }

    static double[][] vectorProducts(double[] x, double[] y, double[] z, double[] u, double[] v, double[] w) {
        final double[][] products = new double[x.length][3];
        for (int i = 0; i < x.length; i++) {
            final double[] product = products[i];
            product[X] = y[i] * w[i] - z[i] * v[i];
            product[Y] = z[i] * u[i] - x[i] * w[i];
            product[Z] = x[i] * v[i] - y[i] * u[i];
        }
        return products;
    }
}
