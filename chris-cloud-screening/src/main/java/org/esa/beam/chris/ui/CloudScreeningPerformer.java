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
package org.esa.beam.chris.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.chris.operators.ExtractEndmembersOp;
import org.esa.beam.chris.operators.FindClustersOp;
import org.esa.beam.chris.operators.internal.ClassificationOpImage;
import org.esa.beam.chris.operators.internal.CloudMaskOpImage;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.ui.AppContext;
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
import java.util.concurrent.ExecutionException;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class CloudScreeningPerformer {
    private static final double WAVELENGTH_R = 650.0;
    private static final double WAVELENGTH_G = 550.0;
    private static final double WAVELENGTH_B = 450.0;

    private final boolean[] clouds;
    private final boolean[] ignoreds;

    private final AppContext appContext;
    private final CloudScreeningFormModel model;
    private Product classificationProduct;
    private Product featureProduct;

    private Product reflectanceProduct;
    private ProductSceneView rgbSceneView; // todo - move to view?
    private ProductSceneView classificationSceneView; // todo - move to view?

    private Band[] featureBands;
    private EMCluster[] clusters;
    private Band classificationBand;
    private boolean probabilisticCloudMask = true;

    CloudScreeningPerformer(AppContext appContext, CloudScreeningFormModel formModel) {
        this.appContext = appContext;
        this.model = formModel;

        clouds = new boolean[formModel.getClusterCount()];
        ignoreds = new boolean[formModel.getClusterCount()];
    }

    // todo - progress monitoring
    void performClusterAnalysis(ProgressMonitor pm) throws OperatorException {
        final Product radianceProduct = model.getRadianceProduct();
        final Map<String, Object> emptyMap = Collections.emptyMap();

        try {
            pm.beginTask("Performing cluster analysis...", 1);
            // 1. RGB image
            try {
                final Band r = findBand(radianceProduct, "radiance", WAVELENGTH_R);
                final Band g = findBand(radianceProduct, "radiance", WAVELENGTH_G);
                final Band b = findBand(radianceProduct, "radiance", WAVELENGTH_B);

                rgbSceneView = new ProductSceneView(ProductSceneImage.create(r, g, b, ProgressMonitor.NULL));
            } catch (IOException e) {
                throw new OperatorException(e.getMessage(), e);
            }
            // 2. TOA reflectances
            reflectanceProduct = GPF.createProduct("chris.ComputeToaReflectances", emptyMap, radianceProduct);
            // 3. Features
            featureProduct = GPF.createProduct("chris.ExtractFeatures", emptyMap, reflectanceProduct);
            final String[] featureBandNames = model.getFeatureBandNames();
            featureBands = new Band[featureBandNames.length];
            for (int i = 0; i < featureBandNames.length; i++) {
                featureBands[i] = featureProduct.getBand(featureBandNames[i]);
            }
            // 4. Clustering
            final BrightnessComparator bc = new BrightnessComparator();
            clusters = FindClustersOp.findClusters(featureProduct, featureBandNames, model.getClusterCount(),
                                                   model.getIterationCount(), model.getSeed(), bc,
                                                   ProgressMonitor.NULL);
            // 5. Unlabeled classification
            final Map<String, Object> parameterMap = new HashMap<String, Object>();
            parameterMap.put("sourceBandNames", featureBandNames);
            parameterMap.put("clusters", clusters);
            classificationProduct = GPF.createProduct("chris.Classify", parameterMap, featureProduct);
            classificationBand = classificationProduct.getBand("class_indices");
            setImageInfo(classificationBand);

            try {
                classificationSceneView = new ProductSceneView(ProductSceneImage.create(classificationBand,
                                                                                        ProgressMonitor.NULL));
            } catch (IOException e) {
                throw new OperatorException(e.getMessage(), e);
            }
        } finally {
            pm.done();
        }
    }

    private void setImageInfo(Band targetBand) {
        final RenderedImage targetImage = targetBand.getImage();
        final Raster targetImageData = targetImage.getData();
        final Raster rgbImageData = rgbSceneView.getSourceImage().getData();

        final int[] r = new int[clusters.length];
        final int[] g = new int[clusters.length];
        final int[] b = new int[clusters.length];

        // class index color = median RGB image color
        for (int k = 0; k < clusters.length; ++k) {
            final ArrayList<Integer> rList = new ArrayList<Integer>(100000);
            final ArrayList<Integer> gList = new ArrayList<Integer>(100000);
            final ArrayList<Integer> bList = new ArrayList<Integer>(100000);

            for (int y = 0; y < targetImage.getHeight(); ++y) {
                for (int x = 0; x < targetImage.getWidth(); ++x) {
                    final int classIndex = targetImageData.getSample(x, y, 0);
                    if (classIndex == k) {
                        rList.add(rgbImageData.getSample(x, y, 0));
                        gList.add(rgbImageData.getSample(x, y, 1));
                        bList.add(rgbImageData.getSample(x, y, 2));
                    }
                }
            }

            Collections.sort(rList);
            Collections.sort(gList);
            Collections.sort(bList);

            if (rList.size() > 0) {
                r[k] = rList.get(rList.size() / 2);
                g[k] = gList.get(gList.size() / 2);
                b[k] = bList.get(bList.size() / 2);
            } else {
                r[k] = 0;
                g[k] = 0;
                b[k] = 0;
            }
        }

        final SampleCoding sampleCoding = targetBand.getIndexCoding();
        final int sampleCount = sampleCoding.getSampleCount();
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[sampleCount];

        for (int i = 0; i < points.length; ++i) {
            final int value = sampleCoding.getSampleValue(i);
            final Color color = new Color(r[i], g[i], b[i]);
            final String label = sampleCoding.getSampleName(i);

            points[i] = new ColorPaletteDef.Point(value, color, label);
        }

        targetBand.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
    }

    // todo - progress monitoring
    void performCloudProductComputation(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing cloud product...", 1);

        final IndexFilter validFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return !ignoreds[index];
            }
        };
        final IndexFilter cloudFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return clouds[index];
            }
        };

        try {
            if (probabilisticCloudMask) {
                // 4. Cloud probabilities
                final OpImage cloudProbImage =
                        CloudMaskOpImage.createProbabilisticImage(featureBands, clusters, validFilter, cloudFilter);
                // 5. Endmember extraction
                final Endmember[] endmembers = ExtractEndmembersOp.extractEndmembers(reflectanceProduct,
                                                                                     featureProduct,
                                                                                     classificationProduct,
                                                                                     model.getFeatureBandNames(),
                                                                                     clusters,
                                                                                     clouds, ignoreds, pm);

                final Band[] reflectanceBands =
                        OpUtils.findBands(reflectanceProduct, "toa_refl", ExtractEndmembersOp.BAND_FILTER);
                final String[] reflectanceBandNames = new String[reflectanceBands.length];
                for (int i = 0; i < reflectanceBands.length; ++i) {
                    reflectanceBandNames[i] = reflectanceBands[i].getName();
                }
                // 6. Cloud abundances
                final Product cloudAbundancesProduct = createCloudAbundancesProduct(endmembers, reflectanceBandNames);

                // 7. Cloud probability * cloud abundance
                addCloudImageToInput(createCloudProductImage(cloudProbImage, cloudAbundancesProduct));
            } else {
                final OpImage cloudMaskImage =
                        CloudMaskOpImage.createBinaryImage(featureBands, clusters, validFilter, cloudFilter);
                addCloudImageToInput(cloudMaskImage);
            }
        } finally {
            pm.done();
        }
    }

    private static RenderedImage createCloudProductImage(OpImage cloudProbabilityImage,
                                                         Product cloudAbundancesProduct) {
        final RenderedImage cloudAbundance = cloudAbundancesProduct.getBand("cloud_abundance").getImage();
        return MultiplyDescriptor.create(cloudProbabilityImage, cloudAbundance, null);
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
        if (!model.getRadianceProduct().containsBand(targetBand.getName())) {
            model.getRadianceProduct().addBand(targetBand);
        }
        final VisatApp visatApp = VisatApp.getApp();
        final JInternalFrame targetBandFrame = visatApp.findInternalFrame(targetBand);
        if (targetBandFrame != null) {
            visatApp.updateImage((ProductSceneView) targetBandFrame.getContentPane());
        } else {
            visatApp.openProductSceneView(targetBand, "");
        }
    }

    Band getCloudProductBand() {
        return model.getRadianceProduct().getBand("cloud_product");
    }

    void dispose() {
        if (classificationSceneView != null) {
            classificationSceneView.dispose();
            classificationSceneView = null;
        }

        if (rgbSceneView != null) {
            rgbSceneView.dispose();
            rgbSceneView = null;
        }

        classificationBand = null;
        if (classificationProduct != null) {
            classificationProduct.dispose();
            classificationProduct = null;
        }
        for (int i = 0; i < clusters.length; i++) {
            clusters[i] = null;
        }
        clusters = null;
        for (int i = 0; i < featureBands.length; i++) {
            featureBands[i] = null;
        }
        featureBands = null;
        if (featureProduct != null) {
            featureProduct.dispose();
            featureProduct = null;
        }
        if (reflectanceProduct != null) {
            reflectanceProduct.dispose();
            reflectanceProduct = null;
        }
    }

    private Product createCloudAbundancesProduct(Endmember[] endmembers, String[] reflectanceBandNames) {
        final Map<String, Object> parameterMap = new HashMap<String, Object>(3);
        parameterMap.put("sourceBandNames", reflectanceBandNames);
        parameterMap.put("endmembers", endmembers);
        parameterMap.put("unmixingModelName", "Fully Constrained LSU");

        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                                 parameterMap, reflectanceProduct);
    }

    private static Band findBand(Product product, String prefix, double wavelength) {
        final Band[] bands = OpUtils.findBands(product, prefix);
        return bands[OpUtils.findBandIndex(bands, wavelength)];
    }


    public Product getRadianceProduct() {
        return model.getRadianceProduct();
    }

    public String getRadianceProductName() {
        return model.getRadianceProduct().getName();
    }

    public String getRadianceProductDisplayName() {
        return model.getRadianceProduct().getDisplayName();
    }

    public RenderedImage getClassificationImage() {
        return classificationBand.getImage();
    }

    public Band getClassificationBand() {
        return classificationBand;
    }

    public ProductSceneView getRgbSceneView() {
        return rgbSceneView;
    }

    public ProductSceneView getClassificationSceneView() {
        return classificationSceneView;
    }

    EMCluster[] getClusters() {
        return clusters;
    }

    boolean isProbabilisticCloudMask() {
        return probabilisticCloudMask;
    }

    void setProbabilisticCloudMask(boolean b) {
        probabilisticCloudMask = b;
    }

    boolean hasCloudClasses() {
        for (final boolean cloud : clouds) {
            if (cloud) {
                return true;
            }
        }

        return false;
    }

    boolean isCloud(int index) {
        return clouds[index];
    }

    void setCloud(int index, boolean b) {
        clouds[index] = b;
    }

    boolean isIgnored(int index) {
        return ignoreds[index];
    }

    void setIgnored(int index, boolean b) {
        if (b != ignoreds[index]) {
            ignoreds[index] = b;
        }
    }

    void recomputeClassificationImage() {
        final IndexFilter indexFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return !ignoreds[index];
            }
        };
        final RenderedImage classificationImage =
                ClassificationOpImage.createImage(featureBands, clusters, indexFilter);
        classificationBand.setImage(classificationImage);
        updateClassificationSceneView();
    }

    private void updateClassificationSceneView() {
        try {
            classificationSceneView.updateImage(ProgressMonitor.NULL);
        } catch (IOException ignored) {
        }
    }

    private static class BrightnessComparator implements Comparator<EMCluster> {
        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getMean()[0], c1.getMean()[0]);
        }
    }

    void performCloudMaskCreation() {
        final Component parent = appContext.getApplicationWindow();
        final String title = "Generating cloud mask...";

        final ProgressMonitorSwingWorker<Object, Object> worker = new ProgressMonitorSwingWorker<Object, Object>(parent,
                                                                                                                 title) {
            @Override
            protected Object doInBackground(ProgressMonitor pm) throws Exception {
                performCloudProductComputation(pm);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    appContext.handleError(e);
                } catch (ExecutionException e) {
                    appContext.handleError(e.getCause());
                }
            }
        };

        worker.execute();
    }

    LabelingContext createLabelingContext() {
        return new LabelingContext() {
            @Override
            public String getLabel(int index) {
                final ImageInfo imageInfo = getClassificationBand().getImageInfo();
                return imageInfo.getColorPaletteDef().getPointAt(index).getLabel();
            }

            @Override
            public void setLabel(int index, String label) {
                final ImageInfo imageInfo = getClassificationBand().getImageInfo();
                imageInfo.getColorPaletteDef().getPointAt(index).setLabel(label);
            }

            @Override
            public Color getColor(int index) {
                final ImageInfo imageInfo = getClassificationBand().getImageInfo();
                return imageInfo.getColorPaletteDef().getPointAt(index).getColor();
            }

            @Override
            public void setColor(int index, Color color) {
                final ImageInfo imageInfo = getClassificationBand().getImageInfo();
                imageInfo.getColorPaletteDef().getPointAt(index).setColor(color);
                updateClassificationSceneView();
            }

            @Override
            public boolean isCloud(int index) {
                return CloudScreeningPerformer.this.isCloud(index);
            }

            @Override
            public void setCloud(int index, boolean b) {
                CloudScreeningPerformer.this.setCloud(index, b);
            }

            @Override
            public boolean isIgnored(int index) {
                return CloudScreeningPerformer.this.isIgnored(index);
            }

            @Override
            public void setIgnored(int index, boolean b) {
                CloudScreeningPerformer.this.setIgnored(index, b);
            }

            @Override
            public EMCluster[] getClusters() {
                return CloudScreeningPerformer.this.getClusters();
            }

            @Override
            public void recomputeClassificationImage() {
                CloudScreeningPerformer.this.recomputeClassificationImage();
            }

            @Override
            public RenderedImage getClassificationImage() {
                return classificationBand.getImage();
            }

            @Override
            public int getClassIndex(int x, int y) {
                return classificationBand.getRasterData().getElemIntAt(y * classificationBand.getRasterWidth() + x);
            }
        };
    }
}
