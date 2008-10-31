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
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.chris.operators.*;
import org.esa.beam.chris.operators.internal.ClassOpImage;
import org.esa.beam.chris.operators.internal.CloudProbabilityOpImage;
import org.esa.beam.chris.util.BandFilter;
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
import org.esa.beam.jai.ImageManager;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.MultiplyDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.text.MessageFormat;
import java.util.*;

/**
 * Cloud screening context.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CloudScreeningContext {
    private static final double WAVELENGTH_R = 650.0;
    private static final double WAVELENGTH_G = 550.0;
    private static final double WAVELENGTH_B = 450.0;

    private final EMCluster[] clusters;
    private final String[] featureBandNames;

    private final boolean[] cloudFlags;
    private final boolean[] invalidFlags;

    private final Product radianceProduct;
    private final Product reflectanceProduct;
    private final Product featureProduct;
    private final Product classProduct;

    private final ProductSceneView rgbView; // todo - move to view?
    private final ProductSceneView classView; // todo - move to view?

    private boolean probabilisticCloudMask = true;

    CloudScreeningContext(AppContext appContext,
                          CloudScreeningFormModel formModel,
                          ProgressMonitor pm) throws Exception {
        final int iterationCount = formModel.getIterationCount();
        final int seed = formModel.getSeed();

        clusters = new EMCluster[formModel.getClusterCount()];
        featureBandNames = formModel.getFeatureBandNames();

        cloudFlags = new boolean[formModel.getClusterCount()];
        invalidFlags = new boolean[formModel.getClusterCount()];

        radianceProduct = formModel.getRadianceProduct();

        try {
            pm.beginTask("Performing cluster analysis...", 100);

            // 1. Reflectances
            final Map<String, Object> emptyMap = Collections.emptyMap();
            reflectanceProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ComputeToaReflectancesOp.class),
                                                   emptyMap,
                                                   radianceProduct);

            // 2. Features
            featureProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ExtractFeaturesOp.class),
                                               emptyMap,
                                               reflectanceProduct);

            // 3. Clustering
            final BrightnessComparator comparator = new BrightnessComparator();
            FindClustersOp.findClusters(featureProduct,
                                        featureBandNames,
                                        clusters,
                                        iterationCount,
                                        seed,
                                        comparator,
                                        SubProgressMonitor.create(pm, 80));

            // 4. Classification
            final Map<String, Object> classificationParameterMap = new HashMap<String, Object>();
            classificationParameterMap.put("sourceBandNames", featureBandNames);
            classificationParameterMap.put("clusters", clusters);
            classProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ClassifyOp.class),
                                             classificationParameterMap,
                                             featureProduct);

            // 5. Scene views
            rgbView = createRgbView(radianceProduct, appContext, SubProgressMonitor.create(pm, 10));
            final Raster rgbRaster = rgbView.getBaseImageLayer().getImage().getData();
            classView = createClassView(classProduct, rgbRaster, appContext, SubProgressMonitor.create(pm, 10));
        } finally {
            pm.done();
        }
    }

    Band performCloudMaskCreation(ProgressMonitor pm) throws OperatorException {
        final IndexFilter validFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return !invalidFlags[index];
            }
        };
        final IndexFilter cloudFilter = new IndexFilter() {
            @Override
            public boolean accept(int index) {
                return cloudFlags[index];
            }
        };

        PlanarImage cloudMaskImage = null;
        try {
            pm.beginTask("Creating cloud mask...", probabilisticCloudMask ? 110 : 100);

            if (probabilisticCloudMask) {
                // 1. Calculate cloud probability
                final OpImage probabilityImage =
                        CloudProbabilityOpImage.createProbabilityImage(featureProduct,
                                                                       featureBandNames,
                                                                       clusters,
                                                                       validFilter,
                                                                       cloudFilter);
                // 2. Extract endmembers
                final Endmember[] endmembers =
                        ExtractEndmembersOp.extractEndmembers(reflectanceProduct,
                                                              featureProduct,
                                                              classProduct,
                                                              featureBandNames,
                                                              clusters,
                                                              cloudFlags,
                                                              invalidFlags,
                                                              SubProgressMonitor.create(pm, 10));

                // 3. Calculate cloud abundance
                final Band[] reflectanceBands =
                        OpUtils.findBands(reflectanceProduct, "toa_refl", ExtractEndmembersOp.BAND_FILTER);
                final String[] reflectanceBandNames = new String[reflectanceBands.length];
                for (int i = 0; i < reflectanceBands.length; ++i) {
                    reflectanceBandNames[i] = reflectanceBands[i].getName();
                }
                final RenderedImage abundanceImage = createCloudAbundanceImage(reflectanceProduct,
                                                                               reflectanceBandNames,
                                                                               endmembers);

                // 4. Calculate cloud mask
                final RenderingHints renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
                renderingHints.put(JAI.KEY_IMAGE_LAYOUT, new ImageLayout(probabilityImage));
                cloudMaskImage = MultiplyDescriptor.create(probabilityImage, abundanceImage, renderingHints);
            } else {
                cloudMaskImage = CloudProbabilityOpImage.createDiscretizedImage(featureProduct,
                                                                                featureBandNames,
                                                                                clusters,
                                                                                validFilter,
                                                                                cloudFilter);
            }
            // 5. Add cloud mask to radiance product
            final Band cloudMaskBand = createSyntheticBand("cloud_product", cloudMaskImage,
                                                           SubProgressMonitor.create(pm, 100));
            if (radianceProduct.containsBand(cloudMaskBand.getName())) {
                radianceProduct.removeBand(radianceProduct.getBand(cloudMaskBand.getName()));
            }
            radianceProduct.addBand(cloudMaskBand);

            return cloudMaskBand;
        } finally {
            if (cloudMaskImage != null) {
                cloudMaskImage.dispose();
            }
            pm.done();
        }
    }

    private void dispose() {
        if (classView != null) {
            classView.dispose();
        }
        if (rgbView != null) {
            rgbView.dispose();
        }
        if (classProduct != null) {
            classProduct.dispose();
        }
        if (featureProduct != null) {
            featureProduct.dispose();
        }
        if (reflectanceProduct != null) {
            reflectanceProduct.dispose();
        }
    }

    ProductSceneView getRgbView() {
        return rgbView;
    }

    ProductSceneView getClassView() {
        return classView;
    }

    boolean isProbabilisticCloudMask() {
        return probabilisticCloudMask;
    }

    void setProbabilisticCloudMask(boolean b) {
        probabilisticCloudMask = b;
    }

    LabelingContext createLabelingContext() {
        return new LabelingContext() {
            @Override
            public String getRadianceProductName() {
                return radianceProduct.getName();
            }

            @Override
            public String getLabel(int index) {
                final ImageInfo imageInfo = getClassBand().getImageInfo();
                return imageInfo.getColorPaletteDef().getPointAt(index).getLabel();
            }

            @Override
            public void setLabel(int index, String label) {
                final ImageInfo imageInfo = getClassBand().getImageInfo();
                imageInfo.getColorPaletteDef().getPointAt(index).setLabel(label);
            }

            @Override
            public Color getColor(int index) {
                final ImageInfo imageInfo = getClassBand().getImageInfo();
                return imageInfo.getColorPaletteDef().getPointAt(index).getColor();
            }

            @Override
            public void setColor(int index, Color color) {
                final ImageInfo imageInfo = getClassBand().getImageInfo();
                imageInfo.getColorPaletteDef().getPointAt(index).setColor(color);
                updateClassificationSceneView();
            }

            @Override
            public boolean isCloud(int index) {
                return cloudFlags[index];
            }

            @Override
            public void setCloud(int index, boolean b) {
                cloudFlags[index] = b;
            }

            @Override
            public boolean isIgnored(int index) {
                return invalidFlags[index];
            }

            @Override
            public void setIgnored(int index, boolean b) {
                if (b != invalidFlags[index]) {
                    invalidFlags[index] = b;
                }
            }

            @Override
            public EMCluster[] getClusters() {
                return clusters;
            }

            @Override
            public RenderedImage getClassificationImage() {
                return getClassBand().getSourceImage();
            }

            @Override
            public int getClassIndex(int x, int y, int currentLevel) {
                // todo - review
                final AffineTransform i2m = classView.getBaseImageLayer().getImageToModelTransform(currentLevel);
                final AffineTransform m2i = classView.getBaseImageLayer().getModelToImageTransform();

                final AffineTransform transform = new AffineTransform();
                transform.concatenate(i2m);
                transform.concatenate(m2i);
                final Point2D point = new Point2D.Double(x, y);
                transform.transform(point, point);

                final int x1 = (int) point.getX();
                final int y1 = (int) point.getY();
                final Raster raster = getClassificationImage().getData(new Rectangle(x1, y1, 1, 1));

                return raster.getSample(x1, y1, 0);
            }

            @Override
            public void recomputeClassificationImage() {
                final IndexFilter indexFilter = new IndexFilter() {
                    @Override
                    public boolean accept(int index) {
                        return !invalidFlags[index];
                    }
                };
                final RenderedImage classificationImage = ClassOpImage.createImage(featureProduct,
                                                                                   featureBandNames,
                                                                                   clusters,
                                                                                   indexFilter);
                getClassBand().setSourceImage(classificationImage);
                updateClassificationSceneView();
            }

            @Override
            public ProductSceneView getRgbView() {
                return CloudScreeningContext.this.getRgbView();
            }

            @Override
            public ProductSceneView getClassView() {
                return CloudScreeningContext.this.getClassView();
            }

            @Override
            public boolean isAnyCloudFlagSet() {
                for (final boolean cloud : cloudFlags) {
                    if (cloud) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public Band performCloudMaskCreation(ProgressMonitor pm) {
                return CloudScreeningContext.this.performCloudMaskCreation(pm);
            }

            private Band getClassBand() {
                return classProduct.getBand("class_indices");
            }

            private void updateClassificationSceneView() {
                classView.getBaseImageLayer().regenerate();
            }
        };
    }

    private static ProductSceneView createRgbView(Product radianceProduct,
                                                  AppContext appContext,
                                                  ProgressMonitor pm) throws Exception {
        final Band r = findBand(radianceProduct, "radiance", WAVELENGTH_R);
        final Band g = findBand(radianceProduct, "radiance", WAVELENGTH_G);
        final Band b = findBand(radianceProduct, "radiance", WAVELENGTH_B);

        return new ProductSceneView(new ProductSceneImage("RGB", r, g, b, appContext.getPreferences(), pm));
    }

    private static ProductSceneView createClassView(Product classProduct,
                                                    Raster rgbRaster,
                                                    AppContext appContext,
                                                    ProgressMonitor pm) throws Exception {
        final Band classBand = classProduct.getBand("class_indices");
        final SampleCoding sampleCoding = classBand.getIndexCoding();
        final int classCount = sampleCoding.getSampleCount();

        final RenderedImage targetImage = classBand.getSourceImage();
        final Raster targetImageData = targetImage.getData();

        final int[] r = new int[classCount];
        final int[] g = new int[classCount];
        final int[] b = new int[classCount];

        // class index color = median RGB image color
        for (int k = 0; k < classCount; ++k) {
            final ArrayList<Integer> rList = new ArrayList<Integer>(100000);
            final ArrayList<Integer> gList = new ArrayList<Integer>(100000);
            final ArrayList<Integer> bList = new ArrayList<Integer>(100000);

            for (int y = 0; y < targetImage.getHeight(); ++y) {
                for (int x = 0; x < targetImage.getWidth(); ++x) {
                    final int classIndex = targetImageData.getSample(x, y, 0);
                    if (classIndex == k) {
                        rList.add(rgbRaster.getSample(x, y, 0));
                        gList.add(rgbRaster.getSample(x, y, 1));
                        bList.add(rgbRaster.getSample(x, y, 2));
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

        // set image info according to median RGB image colors
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[classCount];
        for (int i = 0; i < points.length; ++i) {
            final int value = sampleCoding.getSampleValue(i);
            final Color color = new Color(r[i], g[i], b[i]);
            final String label = sampleCoding.getSampleName(i);

            points[i] = new ColorPaletteDef.Point(value, color, label);
        }
        classBand.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));

        return new ProductSceneView(new ProductSceneImage(classBand, appContext.getPreferences(), pm));
    }

    private static RenderedImage createCloudAbundanceImage(Product reflectanceProduct,
                                                           String[] reflectanceBandNames,
                                                           Endmember[] endmembers) {
        final Map<String, Object> parameterMap = new HashMap<String, Object>(3);
        parameterMap.put("sourceBandNames", reflectanceBandNames);
        parameterMap.put("endmembers", endmembers);
        parameterMap.put("unmixingModelName", "Fully Constrained LSU");

        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        renderingHints.put(GPF.KEY_TILE_SIZE,
                           new Dimension(CloudProbabilityOpImage.TILE_W, CloudProbabilityOpImage.TILE_H));

        final Product product = GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                                                  parameterMap,
                                                  reflectanceProduct,
                                                  renderingHints);

        return product.getBand("cloud_abundance").getSourceImage();
    }

    private static Band createSyntheticBand(String name, RenderedImage sourceImage, ProgressMonitor pm) {
        final int dataType = ImageManager.getProductDataType(sourceImage.getSampleModel().getDataType());
        final Band band = new Band(name, dataType, sourceImage.getWidth(), sourceImage.getHeight());

        band.setRasterData(RasterDataUtils.createRasterData(sourceImage, pm));
        band.setSynthetic(true);

        return band;
    }

    private static Band findBand(Product product, String prefix, final double wavelength) throws Exception {
        final Band band = OpUtils.findBand(product, prefix, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return Math.abs(band.getSpectralWavelength() - wavelength) < band.getSpectralBandwidth();
            }
        });

        if (band == null) {
            throw new Exception(MessageFormat.format(
                    "could not find band with prefix = ''{0}'' and spectral wavelength = {1}", prefix, wavelength));
        }

        return band;
    }

    private static class BrightnessComparator implements Comparator<EMCluster> {
        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getMean()[0], c1.getMean()[0]);
        }
    }
}
