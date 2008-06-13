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
package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.operators.internal.Cluster;
import org.esa.beam.chris.operators.internal.Clusterer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.FindClusters",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Finds clusters for features extracted from TOA reflectances.")
public class FindClustersOp extends Operator {

    @SourceProduct(alias = "source", type = "CHRIS_M[1-5][A0]?_FEAT")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(label = "Number of clusters", defaultValue = "14")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "30")
    private int iterationCount;
    @Parameter(label = "Source band names",
               description = " Names of the bands that are used for the cluster analysis.")
    private String[] sourceBandNames;
    @Parameter(label = "Include probabilities", defaultValue = "false")
    private boolean includeProbabilities;

    private transient Comparator<Cluster> clusterComparator;
    private transient Band[] sourceBands;
    private transient Band clusterMapBand;
    private transient Band[] probabilityBands;

    public FindClustersOp() {
    }

    public FindClustersOp(Product sourceProduct,
                          int clusterCount,
                          int iterationCount,
                          String[] sourceBandNames,
                          boolean includeProbabilities,
                          Comparator<Cluster> clusterComparator) {
        this.sourceProduct = sourceProduct;
        this.clusterCount = clusterCount;
        this.iterationCount = iterationCount;
        this.sourceBandNames = sourceBandNames;
        this.includeProbabilities = includeProbabilities;
        this.clusterComparator = clusterComparator;
    }

    @Override
    public void initialize() throws OperatorException {
        if (sourceBandNames != null && sourceBandNames.length > 0) {
            sourceBands = new Band[sourceBandNames.length];
            for (int i = 0; i < sourceBandNames.length; i++) {
                final Band sourceBand = sourceProduct.getBand(sourceBandNames[i]);
                if (sourceBand == null) {
                    throw new OperatorException("source band not found: " + sourceBandNames[i]);
                }
                sourceBands[i] = sourceBand;
            }
        } else {
            sourceBands = sourceProduct.getBands();
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName().replace("_FEAT", "_CLU");
        final String type = sourceProduct.getProductType().replace("_FEAT", "_CLU");

        final Product targetProduct = new Product(name, type, width, height);
        targetProduct.setPreferredTileSize(width, height);

        if (includeProbabilities) {
            probabilityBands = new Band[clusterCount];
            for (int i = 0; i < clusterCount; ++i) {
                final Band targetBand = targetProduct.addBand("probability_" + i, ProductData.TYPE_FLOAT32);
                targetBand.setUnit("dl");
                targetBand.setDescription("Cluster posterior probabilities");

                probabilityBands[i] = targetBand;
            }
        }

        clusterMapBand = new Band("cluster_map", ProductData.TYPE_INT16, width, height);
        clusterMapBand.setDescription("Cluster map");
        targetProduct.addBand(clusterMapBand);

        final IndexCoding indexCoding = new IndexCoding("clusters");
        for (int i = 0; i < sourceBands.length; i++) {
            indexCoding.addIndex("cluster_" + (i + 1), i, "Cluster label");
        }
        targetProduct.getIndexCodingGroup().add(indexCoding);
        clusterMapBand.setSampleCoding(indexCoding);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing clusters...", iterationCount);

        try {
            final int sceneWidth = sourceProduct.getSceneRasterWidth();
            final Clusterer clusterer = createClusterer();

            for (int i = 0; i < iterationCount; ++i) {
                checkForCancelation(pm);
                clusterer.iterate();
                pm.worked(1);
            }

            final Cluster[] clusters;
            if (clusterComparator == null) {
                clusters = clusterer.getClusters();
            } else {
                clusters = clusterer.getClusters(clusterComparator);
            }

            if (includeProbabilities) {
                for (int i = 0; i < clusterCount; ++i) {
                    final Tile targetTile = targetTileMap.get(probabilityBands[i]);
                    final double[] p = clusters[i].getPosteriorProbabilities();

                    for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                        for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                            targetTile.setSample(x, y, p[y * sceneWidth + x]);
                        }
                    }
                }
            }
            final Tile targetTile = targetTileMap.get(clusterMapBand);
            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    final double[] samples = new double[clusterCount];
                    for (int i = 0; i < clusterCount; ++i) {
                        samples[i] = clusters[i].getPosteriorProbabilities()[y * sceneWidth + x];
                    }
                    targetTile.setSample(x, y, findMaxIndex(samples));
                }
            }
        } catch (IOException e) {
            throw new OperatorException(e);
        } finally {
            pm.done();
        }
    }

    private static int findMaxIndex(double[] samples) {
        int index = 0;

        for (int i = 1; i < samples.length; ++i) {
            if (samples[i] > samples[index]) {
                index = i;
            }
        }

        return index;
    }

    private Clusterer createClusterer() throws IOException {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        final double[] samples = new double[sceneWidth];

        final double[][] points = new double[sceneWidth * sceneHeight][sourceBands.length];
        final double[] min = new double[sourceBands.length];
        final double[] max = new double[sourceBands.length];

        for (int i = 0; i < sourceBands.length; i++) {
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;

            for (int y = 0; y < sceneHeight; y++) {
                sourceBands[i].readPixels(0, y, sceneWidth, 1, samples, ProgressMonitor.NULL);

                for (int x = 0; x < sceneWidth; x++) {
                    points[y * sceneWidth + x][i] = samples[x];
                    if (samples[x] < min[i]) {
                        min[i] = samples[x];
                    }
                    if (samples[x] > max[i]) {
                        max[i] = samples[x];
                    }
                }
            }
            for (int j = 0; j < points.length; ++j) {
                points[j][i] = (points[j][i] - min[i]) / (max[i] - min[i]);
            }
        }

        return new Clusterer(points, clusterCount);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FindClustersOp.class);
        }
    }
}
