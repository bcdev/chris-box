/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.chris.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.operators.FindClustersOp;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.OpImage;
import javax.media.jai.operator.MultiplyDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision$ $Date$
 */
public class CloudLabeler {

    private final Product radianceProduct;
    private Product reflectanceProduct;
    private Product featureProduct;
    private EMCluster[] clusters;
    private Product clusterMapProduct;
    private ProductSceneView rgbSceneView;


    public CloudLabeler(Product radianceProduct) {
        this.radianceProduct = radianceProduct;
    }

    public Band getClusterMapBand() {
        return clusterMapProduct.getBand("class_indices");
    }

    public Product getRadianceProduct() {
        return radianceProduct;
    }

    public Band getCloudProductBand() {
        return radianceProduct.getBand("cloud_product");
    }

    public void performClusterAnalysis(ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("Performing cluster analysis...", 1);
            // 1. Extract features
            reflectanceProduct = createReflectanceProduct(radianceProduct);
            featureProduct = createFeatureProduct(reflectanceProduct);

            // 2. Find clusters
            clusters = findClusters(featureProduct);

            // 3. Cluster labeling
            clusterMapProduct = createClusterMapProduct(featureProduct, clusters);
        } finally {
            pm.done();
        }
    }

    public void createRgbSceneView() throws IOException {
//        final RasterDataNode[] rgbBands = getRgbBands(radianceProduct);
//        ProductSceneImage productSceneImage = ProductSceneImage.create(rgbBands[0], rgbBands[1], rgbBands[2],
//                                                                       ProgressMonitor.NULL);
//        rgbSceneView = new ProductSceneView(productSceneImage);
//
//        assignImageInfo(rgbSceneView.getSourceImage());
    }

    public ProductSceneView getRgbSceneView() {
        return rgbSceneView;
    }

    private void assignImageInfo(RenderedImage rgbImage) {
//        final int[] r = new int[clusters.length];
//        final int[] g = new int[clusters.length];
//        final int[] b = new int[clusters.length];
//        final int[] clusterCount = new int[clusters.length];
//
//        final RenderedImage image = getClusterMapBand().getImage();
//        final Raster membershipImageData = image.getData();
//        final Raster rgbImageData = rgbImage.getData();
//        for (int y = 0; y < image.getHeight(); y++) {
//            for (int x = 0; x < image.getWidth(); x++) {
//                final int clusterIndex = membershipImageData.getSample(x, y, 0);
//                r[clusterIndex] += rgbImageData.getSample(x, y, 0);
//                g[clusterIndex] += rgbImageData.getSample(x, y, 1);
//                b[clusterIndex] += rgbImageData.getSample(x, y, 2);
//                clusterCount[clusterIndex]++;
//            }
//        }
//
//        for (int i = 0; i < clusterCount.length; i++) {
//            if (clusterCount[i] > 0) {
//                r[i] /= clusterCount[i];
//                g[i] /= clusterCount[i];
//                b[i] /= clusterCount[i];
//            }
//        }
//
//        final IndexCoding indexCoding = getClusterMapBand().getIndexCoding();
//        final String[] classNames = indexCoding.getIndexNames();
//        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[classNames.length];
//        for (int index = 0; index < points.length; index++) {
//            String className = classNames[index];
//            final int sample = indexCoding.getIndexValue(className);
//            final Color color = new Color(r[index], g[index], b[index]);
//            points[index] = new ColorPaletteDef.Point(sample, color, className);
//        }
//        final ColorPaletteDef def = new ColorPaletteDef(points);
//        final ImageInfo imageInfo = new ImageInfo(def);
//        getClusterMapBand().setImageInfo(imageInfo);
    }

    private static RasterDataNode[] getRgbBands(Product product) {
        return new RasterDataNode[]{
                findBandImage(product, 650.0f),
                findBandImage(product, 550.0f),
                findBandImage(product, 450.0f)
        };
    }

    private static Band findBandImage(Product product, float wavelength) {
        final Band[] bands = product.getBands();

        float minDist = Float.POSITIVE_INFINITY;
        Band bestBand = null;

        for (Band band : bands) {
            if (band.getSpectralBandIndex() != -1) {
                float currentDist = Math.abs(wavelength - band.getSpectralWavelength());
                if (minDist > currentDist) {
                    bestBand = band;
                    minDist = currentDist;
                }
            }
        }

        return bestBand;
    }

    public void performLabelingStep(final IndexFilter indexFilter) throws OperatorException {
        final Band clusterMapBand = getClusterMapBand();
        final Band[] featureBands = getFeatureBands(featureProduct);

//        clusterMapBand.setImage(ClassificationOpImage.createImage(featureBands, clusters, indexFilter));
    }

    public static class LabelingPerformer {
        private final Band[] featureBands;
        private final Band classificationBand;
        private final EMCluster[] clusters;

        public LabelingPerformer(Band[] featureBands, Band classificationBand, EMCluster[] clusters) {
            this.featureBands = featureBands;
            this.classificationBand = classificationBand;
            this.clusters = clusters;
        }

        public void performLabeling(final boolean[] invalids, double[] brightnesses, double[] occurrences) {
            final IndexFilter indexFilter = new IndexFilter() {
                @Override
                public boolean accept(int index) {
                    return !invalids[index];
                }
            };
//            classificationBand.setImage(ClassificationOpImage.createImage(featureBands, clusters, indexFilter));
            // todo - compute brightnesses and occurrences
        }
    }

    public void performCloudProductComputation(final IndexFilter validFilter, final IndexFilter cloudFilter,
                                               boolean computeAbundances, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing cloud product...", 1);

        try {
            if (computeAbundances) {
                // 4. Cloud probabilities
//                final Band[] featureBands = getFeatureBands(featureProduct);
//                final OpImage cloudProbImage =
//                        CloudMaskOpImage.createProbabilisticImage(featureBands, clusters, validFilter, cloudFilter);
                // 5. Endmember extraction
//////                final Operator endmemberOp = new ExtractEndmembersOp(reflectanceProduct,
//////                                                                     featureProduct,
//////                                                                     clusterMapProduct,
//////                                                                     getFeatureBands(featureProduct),
//////                                                                     clusters,
//////                                                                     cloudFilter, validFilter
//////                );
////                final Endmember[] endmembers = (Endmember[]) endmemberOp.getTargetProperty("endmembers");
////                final String[] reflectanceBandNames = (String[]) endmemberOp.getTargetProperty("reflectanceBandNames");
//
//                // 6. Cloud abundances
//                final Product cloudAbundancesProduct = createCloudAbundancesProduct(endmembers, reflectanceBandNames);
//
//                // 7. Cloud probability * cloud abundance
//                addCloudImageToInput(createCloudProductImage(cloudProbImage, cloudAbundancesProduct));
            } else {
                final Band[] featureBands = getFeatureBands(featureProduct);
//                final OpImage cloudMaskImage =
//                        CloudProbabilityOpImage.createBinaryImage(featureBands, clusters, validFilter, cloudFilter);
//                addCloudImageToInput(cloudMaskImage);
            }
        } finally {
            pm.done();
        }
    }

    private void addCloudImageToInput(RenderedImage image) {
        Band targetBand = getCloudProductBand();
        final int width = image.getWidth();
        final int height = image.getHeight();
        if (targetBand == null) {
            targetBand = new Band("cloud_product", ProductData.TYPE_FLOAT64, width, height);
            targetBand.setSynthetic(true);
            targetBand.setDescription("Cloud product");
        }
        ProductData rasterData = targetBand.getRasterData();
        if (rasterData == null) {
            rasterData = targetBand.createCompatibleRasterData();
        }
        final Object data = rasterData.getElems();
        image.getData().getDataElements(0, 0, width, height, data);
        targetBand.setRasterData(rasterData);
        if (!radianceProduct.containsBand(targetBand.getName())) {
            radianceProduct.addBand(targetBand);
        }
        final VisatApp visatApp = VisatApp.getApp();
        final JInternalFrame targetBandFrame = visatApp.findInternalFrame(targetBand);
        if (targetBandFrame != null) {
            visatApp.updateImage((ProductSceneView) targetBandFrame.getContentPane());
        } else {
//            visatApp.openProductSceneView(targetBand, "");
        }
    }

//    private static RenderedImage createCloudProductImage(OpImage cloudProbabilityImage,
//                                                         Product cloudAbundancesProduct) {
//        final RenderedImage cloudProbability = cloudProbabilityImage;
//        final RenderedImage cloudAbundance = cloudAbundancesProduct.getBand("cloud_abundance").getImage();
//        return MultiplyDescriptor.create(cloudProbability, cloudAbundance, null);
//    }

    private Product createCloudAbundancesProduct(Endmember[] endmembers, String[] reflectanceBandNames) {
        final Map<String, Object> parameterMap = new HashMap<String, Object>(3);
        parameterMap.put("sourceBandNames", reflectanceBandNames);
        parameterMap.put("endmembers", endmembers);
        parameterMap.put("unmixingModelName", "Fully Constrained LSU");

        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                                 parameterMap, reflectanceProduct);
    }

    private EMCluster[] findClusters(Product featureProduct) {
        // todo -- make GUI parameters
        final int clusterCount = 14;
        final int iterationCount = 60;
        final int seed = 31415;

        final String[] sourceBandNames = getFeatureBandNames(featureProduct);
        final Comparator<EMCluster> clusterComparator = new Comparator<EMCluster>() {
            @Override
            public int compare(EMCluster c1, EMCluster c2) {
                return Double.compare(c2.getMean()[0], c1.getMean()[0]);
            }
        };

        return FindClustersOp.findClusters(featureProduct, sourceBandNames, clusterCount, iterationCount, seed,
                                           clusterComparator, ProgressMonitor.NULL);
    }

    private String[] getFeatureBandNames(Product featureProduct) {
        final List<String> sourceBandNameList = new ArrayList<String>(5);
        Collections.addAll(sourceBandNameList, "brightness_vis", "brightness_nir", "whiteness_vis", "whiteness_nir");
        if (featureProduct.getProductType().matches("CHRIS_M[15]_FEAT")) {
            sourceBandNameList.add("wv");
        }
        final String[] sourceBandNames = sourceBandNameList.toArray(new String[sourceBandNameList.size()]);
        return sourceBandNames;
    }

    private Band[] getFeatureBands(Product featureProduct) {
        final List<Band> sourceBandList = new ArrayList<Band>(5);
        Collections.addAll(sourceBandList,
                           featureProduct.getBand("brightness_vis"),
                           featureProduct.getBand("brightness_nir"),
                           featureProduct.getBand("whiteness_vis"),
                           featureProduct.getBand("whiteness_nir"));
        if (featureProduct.getProductType().matches("CHRIS_M[15]_FEAT")) {
            sourceBandList.add(featureProduct.getBand("wv"));
        }

        return sourceBandList.toArray(new Band[sourceBandList.size()]);
    }

    private Product createClusterMapProduct(Product featureProduct, EMCluster[] clusters) {
        final Map<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("sourceBandNames", getFeatureBandNames(featureProduct));
        parameterMap.put("clusters", clusters);

        return GPF.createProduct("chris.MakeClusterMap", parameterMap, featureProduct);
    }

    private static Product createReflectanceProduct(Product radianceProduct) {
        final Map<String, Object> emptyMap = Collections.emptyMap();
        return GPF.createProduct("chris.ComputeToaReflectances",
                                 emptyMap,
                                 radianceProduct);
    }

    private static Product createFeatureProduct(Product reflectanceProduct) {
        final Map<String, Object> emptyMap = Collections.emptyMap();
        return GPF.createProduct("chris.ExtractFeatures",
                                 emptyMap,
                                 reflectanceProduct);
    }

    public void disposeSourceProducts() {
        if (clusterMapProduct != null) {
            clusterMapProduct.dispose();
            clusterMapProduct = null;
        }
        if (featureProduct != null) {
            featureProduct.dispose();
            featureProduct = null;
        }
        if (reflectanceProduct != null) {
            reflectanceProduct.dispose();
            reflectanceProduct = null;
        }
    }
}
