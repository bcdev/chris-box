package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import de.gkss.hs.datev2004.Clucov;
import de.gkss.hs.datev2004.DataSet;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.FindClucovClusters",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Finds clusters for features extracted from TOA reflectances.")
public class FindClucovClustersOp extends Operator {
    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(alias = "features")
    private String[] sourceBandNames;
    @Parameter
    private String roiExpression;
    @Parameter
    private int clusterCount;

    private transient Band[] sourceBands;
    private transient Map<Short, Band> likelihoodBandMap;
    private transient Band groupBand;
    private transient Band sumBand;
    private transient Clucov clucov;


    @Override
    public void initialize() throws OperatorException {
        sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            String featureBandName = sourceBandNames[i];
            Band band = sourceProduct.getBand(featureBandName);
            if (band == null) {
                throw new OperatorException("feature band not found: " + featureBandName);
            }
            sourceBands[i] = band;
        }

        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product("clucov", "clucov", width, height);
        targetProduct.setPreferredTileSize(width, height);

        try {
            computeClusters();
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        clusterCount = clucov.clusterMap.keySet().size();
        likelihoodBandMap = new HashMap<Short, Band>(clusterCount);
        for (short i : clucov.clusterMap.keySet()) {
            final Band targetBand = targetProduct.addBand("probability_" + i, ProductData.TYPE_FLOAT64);
            targetBand.setUnit("dl");
            targetBand.setDescription("Cluster probability");

            likelihoodBandMap.put(i, targetBand);
        }
        storeClustersInProduct();

        groupBand = targetProduct.addBand("group", ProductData.TYPE_UINT8);
        groupBand.setUnit("dl");
        groupBand.setDescription("Cluster group");
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (clucov == null) {
            try {
                computeClusters();
                clusterCount = clucov.clusterMap.keySet().size();
                likelihoodBandMap = new HashMap<Short, Band>(clusterCount);
                for (short i : clucov.clusterMap.keySet()) {
                    final Band band = targetProduct.addBand("probability_" + i, ProductData.TYPE_FLOAT64);
                    targetBand.setUnit("dl");
                    targetBand.setDescription("Cluster probability");

                    likelihoodBandMap.put(i, band);
                }
                storeClustersInProduct();
                targetProduct.setModified(true);
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }

        if (targetBand == groupBand) {
            Rectangle rectangle = targetTile.getRectangle();
            final int sourceWidth = sourceProduct.getSceneRasterWidth();
            DataSet ds = clucov.ds;
//            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
//                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
//                    int dsIndex = y * sourceWidth + x;
//                    targetTile.setSample(x, y, ds.group[dsIndex]);
//                }
//            }
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    int dsIndex = y * sourceWidth + x;
                    Clucov.Cluster bestCluster = null;
                    double bestDensity = 0.0;
                    for (Clucov.Cluster actualCluster : clucov.clusterMap.values()) {
                        final double actualDensity = actualCluster.gauss.density(ds.pt[dsIndex]);
                        if (bestCluster != null) {
                            if (bestDensity < actualDensity) {
                                bestDensity = actualDensity;
                                bestCluster = actualCluster;
                            }
                        } else {
                            bestCluster = actualCluster;
                            bestDensity = actualDensity;
                        }
                    }
                    targetTile.setSample(x, y, bestCluster.group);
                }
            }
        } else {
            for (short i : clucov.clusterMap.keySet()) {
                if (targetBand == likelihoodBandMap.get(i)) {
                    final Clucov.Cluster actualCluster = clucov.clusterMap.get(i);
                    Rectangle rectangle = targetTile.getRectangle();
                    final int sourceWidth = sourceProduct.getSceneRasterWidth();
                    DataSet ds = clucov.ds;
                    for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                        for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                            int dsIndex = y * sourceWidth + x;
                            double p = actualCluster.gauss.density(ds.pt[dsIndex]);
                            double sum = 0.0;
                            for (Clucov.Cluster cluster : clucov.clusterMap.values()) {
                                sum += cluster.gauss.density(ds.pt[dsIndex]);
                            }
                            if (sum > 0.0) {
                                p /= sum;
                            }
                            targetTile.setSample(x, y, p);
                        }
                    }
                }
            }
        }
    }

    private void storeClustersInProduct() {
        MetadataElement metadataRoot = targetProduct.getMetadataRoot();
        Set<Short> shorts = clucov.clusterMap.keySet();
        MetadataElement clustersElement = new MetadataElement("clusters");
        metadataRoot.addElement(clustersElement);
        for (Short aShort : shorts) {
            Clucov.Cluster cluster = clucov.clusterMap.get(aShort);
            MetadataElement clusterElement = new MetadataElement("cluster");
            clusterElement.addAttribute(new MetadataAttribute("group", ProductData.createInstance(new short[]{cluster.group}), true));
            clusterElement.addAttribute(new MetadataAttribute("gauss.normfactor", ProductData.createInstance(new double[]{cluster.gauss.normfactor}), true));
            clusterElement.addAttribute(new MetadataAttribute("gauss.cog", ProductData.createInstance(cluster.gauss.cog), true));
            double[][] array = cluster.gauss.covinv.getArray();
            for (int i = 0; i < array.length; i++) {
                clusterElement.addAttribute(new MetadataAttribute("gauss.covinv." + i, ProductData.createInstance(array[i]), true));
            }
            clustersElement.addElement(clusterElement);
        }
    }

    private void computeClusters() throws IOException {
        int width = sourceProduct.getSceneRasterWidth();
        int height = sourceProduct.getSceneRasterHeight();
        double[] scanLine = new double[width];
        double[][] dsVectors = new double[width][sourceBands.length];

        double[] min = new double[sourceBands.length];
        double[] max = new double[sourceBands.length];

        for (int i = 0; i < sourceBands.length; ++i) {
            min[i] = Double.POSITIVE_INFINITY;
            max[i] = Double.NEGATIVE_INFINITY;
        }
        // todo - handle valid expression!
        DataSet ds = new DataSet(width * height, sourceBands.length);
        for (int y = 0; y < height; y++) {
            for (int i = 0; i < sourceBands.length; i++) {
                Band featureBand = sourceBands[i];
                featureBand.readPixels(0, y, width, 1, scanLine, ProgressMonitor.NULL);

                // todo - handle no-data!
                for (int x = 0; x < width; x++) {
                    dsVectors[x][i] = scanLine[x];
                    if (scanLine[x] < min[i]) {
                        min[i] = scanLine[x];
                    }
                    if (scanLine[x] > max[i]) {
                        max[i] = scanLine[x];
                    }
                }
            }
            for (int x = 0; x < width; x++) {
                ds.add(dsVectors[x]);
            }
        }
        for (int j = 0; j < ds.pt.length; ++j) {
            for (int i = 0; i < sourceBands.length; ++i) {
                ds.pt[j][i] = (ds.pt[j][i] - min[i]) / (max[i] - min[i]);
            }
        }

        BeamLogManager.configureSystemLogger(BeamLogManager.createFormatter("clucov", "1.0", "BC"), true);
        clucov = new Clucov(ds, BeamLogManager.getSystemLogger());
        //clucov.
        clucov.initialize(clusterCount);
        clucov.run();
    }

    @Override
    public void dispose() {
        // todo - add any clean-up code here
        clucov = null;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FindClucovClustersOp.class);
        }
    }
}
