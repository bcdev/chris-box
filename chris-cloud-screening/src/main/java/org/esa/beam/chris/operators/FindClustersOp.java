/*
 * Copyright (C) 2002-2007 by Brockmann Consult
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
import org.esa.beam.chris.operators.internal.Clusterer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

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

    @SourceProduct
    private Product sourceProduct;

    @Parameter(alias = "features", defaultValue = "brightness_vis,brightness_nir,whiteness_vis,whiteness_nir,wv")
    private String[] sourceBandNames;
    @Parameter(label = "ROI expression", defaultValue = "")
    private String roiExpression;
    @Parameter(label = "Number of clusters", defaultValue = "14")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "20")
    private int iterationCount;
    @Parameter(label = "Cluster distance", defaultValue = "0.0")
    private double clusterDistance;

    public FindClustersOp() {
    }

    public FindClustersOp(Product sourceProduct, String[] sourceBandNames, int clusterCount, int iterationCount,
                          double clusterDistance) {
        this.sourceProduct = sourceProduct;
        this.sourceBandNames = sourceBandNames;
        this.roiExpression = "";
        this.clusterCount = clusterCount;
        this.iterationCount = iterationCount;
        this.clusterDistance = clusterDistance;
    }

    private transient Band[] sourceBands;
    private transient Band[] targetBands;
    private transient Clusterer.Cluster[] clusters;
    private transient Band membershipBand;

    @Override
    public void initialize() throws OperatorException {
        sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final Band sourceBand = sourceProduct.getBand(sourceBandNames[i]);
            if (sourceBand == null) {
                throw new OperatorException("source band not found: " + sourceBandNames[i]);
            }
            sourceBands[i] = sourceBand;
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        final String name = sourceProduct.getName().replace("_FEAT", "_CLU");
        final String type = sourceProduct.getProductType().replace("_FEAT", "_CLU");

        final Product targetProduct = new Product(name, type, width, height);
        targetProduct.setPreferredTileSize(width, height);

        targetBands = new Band[clusterCount];
        for (int i = 0; i < clusterCount; ++i) {
            final Band targetBand = targetProduct.addBand("probability_" + i, ProductData.TYPE_INT8);
            targetBand.setScalingFactor(1.0 / 127.0);
            targetBand.setUnit("dl");
            targetBand.setDescription("Cluster posterior probabilities");

            targetBands[i] = targetBand;
        }

        membershipBand = new ImageBand("membership_mask", ProductData.TYPE_INT8, width, height);
        membershipBand.setDescription("Cluster membership mask");
        targetProduct.addBand(membershipBand);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (clusters == null) {
            try {
                findClusters();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        final Rectangle targetRectangle = targetTile.getRectangle();
        final int sceneWidth = sourceProduct.getSceneRasterWidth();

        for (int i = 0; i < clusterCount; ++i) {
            if (targetBand == targetBands[i]) {
                final double[] probabilities = clusters[i].getPosteriorProbabilities();

                for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                        targetTile.setSample(x, y, probabilities[y * sceneWidth + x]);
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        clusters = null;
    }

    private void findClusters() throws IOException {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        final double[] samples = new double[sceneWidth];

        // todo - valid expression
        final double[][] points = new double[sceneWidth * sceneHeight][sourceBands.length];
        final double[] min = new double[sourceBands.length];
        final double[] max = new double[sourceBands.length];

        for (int i = 0; i < sourceBands.length; i++) {
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;

            for (int y = 0; y < sceneHeight; y++) {
                sourceBands[i].readPixels(0, y, sceneWidth, 1, samples, ProgressMonitor.NULL);

                // todo - no-data
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
        clusters = Clusterer.findClusters(points, clusterCount, iterationCount, clusterDistance);
    }

    public void setClusterCount(int clusterCount) {
        this.clusterCount = clusterCount;
    }

    public void setClusterDistance(double clusterDistance) {
        this.clusterDistance = clusterDistance;
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FindClustersOp.class);
        }
    }


    private class ImageBand extends Band {

        public ImageBand(String name, int dataType, int width, int height) {
            super(name, dataType, width, height);
        }

        @Override
        public void readRasterData(int x, int y, int w, int h, ProductData data, ProgressMonitor pm) throws IOException {
            if (clusters == null) {
                membershipBand.setImage(ClusterMembershipOpImage.create(targetBands, membershipBand));
            }

            final Rectangle rectangle = new Rectangle(x, y, w, h);
            final RenderedImage image = getImage();
            final Raster raster = image.getData(rectangle);

            raster.getDataElements(x, y, w, h, data.getElems());
        }
    }
}
