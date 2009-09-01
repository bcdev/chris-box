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
import org.esa.beam.chris.operators.internal.*;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.IndexFilter;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.*;
import javax.media.jai.operator.HistogramDescriptor;
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
class ScreeningContext implements LabelingContext {
    private static final double WAVELENGTH_R = 650.0;
    private static final double WAVELENGTH_G = 550.0;
    private static final double WAVELENGTH_B = 450.0;

    private final EMCluster[] clusters;
    private final String[] featureBandNames;

    private final Product radianceProduct;
    private final Product reflectanceProduct;
    private final Product featureProduct;
    private final Product classProduct;

    private final ProductSceneView colorView;
    private final ProductSceneView classView;

    ScreeningContext(ScreeningFormModel formModel, PropertyMap configuration, ProgressMonitor pm) throws Exception {
        final int iterationCount = formModel.getIterationCount();
        final int seed = formModel.getSeed();

        clusters = new EMCluster[formModel.getClusterCount()];
        featureBandNames = formModel.getFeatureBandNames();
        radianceProduct = formModel.getSourceProduct();

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
            colorView = createColorView(radianceProduct, configuration, SubProgressMonitor.create(pm, 10));
            final Raster rgb = colorView.getBaseImageLayer().getImage().getData();
            classView = createClassView(classProduct, rgb, configuration, SubProgressMonitor.create(pm, 10));
        } finally {
            pm.done();
        }
    }

    Band performCloudMaskCreation(boolean[] cloudyFlags,
                                  boolean[] ignoreFlags,
                                  boolean probabilistic,
                                  ProgressMonitor pm)
            throws OperatorException {
        final IndexFilter validFilter = new ExclusiveIndexFilter(ignoreFlags);
        final IndexFilter cloudFilter = new InclusiveIndexFilter(cloudyFlags);

        final RenderedImage cloudMaskImage;
        try {
            pm.beginTask("Creating cloud mask...", probabilistic ? 110 : 100);

            if (probabilistic) {
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
                                                              cloudyFlags,
                                                              ignoreFlags,
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
            final Band band = createSyntheticBand("cloud_product", cloudMaskImage, SubProgressMonitor.create(pm, 100));
            band.setDescription("Cloud product");

            return band;
        } finally {
            pm.done();
        }
    }

    @Override
    public int getClusterCount() {
        return clusters.length;
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
        classView.getBaseImageLayer().regenerate();
        classView.getLayerCanvas().repaint();
    }

    @Override
    public void regenerateClassView(boolean[] ignoreFlags) {
        final IndexFilter indexFilter = new ExclusiveIndexFilter(ignoreFlags);
        final RenderedImage classImage = ClassOpImage.createImage(featureProduct,
                                                                  featureBandNames,
                                                                  clusters,
                                                                  indexFilter);
        getClassBand().setSourceImage(classImage);
        classView.getBaseImageLayer().regenerate();
        classView.getLayerCanvas().repaint();
    }

    @Override
    public void computeBrightnessValues(double[] brightnessValues, final boolean[] ignoreFlags) {
        final IndexFilter indexFilter = new ExclusiveIndexFilter(ignoreFlags);

        final double[] sums = new double[clusters.length];
        final ProbabilityCalculator pc = Clusterer.createProbabilityCalculator(clusters);
        Arrays.fill(brightnessValues, 0.0);

        for (final EMCluster cluster : clusters) {
            final double[] posteriors = new double[clusters.length];
            pc.calculate(cluster.getMean(), posteriors, indexFilter);

            for (int k = 0; k < clusters.length; ++k) {
                brightnessValues[k] += cluster.getMean(0) * posteriors[k];
                sums[k] += posteriors[k];
            }
        }
        for (int k = 0; k < clusters.length; ++k) {
            if (sums[k] > 0.0) {
                brightnessValues[k] /= sums[k];
            }
        }
    }

    @Override
    public void computeOccurrenceValues(double[] occurrenceValues) {
        final RenderedImage image = getClassBand().getSourceImage();
        final double[] min = {0.0};
        final double[] max = {clusters.length};
        final int[] binCount = {clusters.length};
        final RenderedOp op = HistogramDescriptor.create(image, null, 2, 2, binCount, min, max, null);
        final Histogram histogram = (Histogram) op.getProperty("histogram");

        final int totalCounts = image.getWidth() * image.getHeight();
        final int[] histogramCounts = histogram.getBins(0);

        for (int k = 0; k < clusters.length; ++k) {
            occurrenceValues[k] = (4.0 * histogramCounts[k]) / totalCounts;
        }
    }

    int getClassIndex(int x, int y, int currentLevel) {
        // todo - review with team
        final AffineTransform i2m = classView.getBaseImageLayer().getImageToModelTransform(currentLevel);
        final AffineTransform m2i = classView.getBaseImageLayer().getModelToImageTransform();

        final AffineTransform transform = new AffineTransform();
        transform.concatenate(i2m);
        transform.concatenate(m2i);
        final Point2D point = new Point2D.Double(x, y);
        transform.transform(point, point);

        final int x1 = (int) point.getX();
        final int y1 = (int) point.getY();
        final Raster raster = getClassBand().getSourceImage().getData(new Rectangle(x1, y1, 1, 1));

        return raster.getSample(x1, y1, 0);
    }

    Product getRadianceProduct() {
        return radianceProduct;
    }

    ProductSceneView getColorView() {
        return colorView;
    }

    ProductSceneView getClassView() {
        return classView;
    }

    private Band getClassBand() {
        return classProduct.getBand("class_indices");
    }

    private static ProductSceneView createColorView(Product radianceProduct,
                                                    PropertyMap configuration,
                                                    ProgressMonitor pm) throws Exception {
        final Band r = findBand(radianceProduct, "radiance", WAVELENGTH_R);
        final Band g = findBand(radianceProduct, "radiance", WAVELENGTH_G);
        final Band b = findBand(radianceProduct, "radiance", WAVELENGTH_B);

        return new ProductSceneView(new ProductSceneImage("RGB", r, g, b, configuration, pm));
    }

    private static ProductSceneView createClassView(Product classProduct,
                                                    Raster rgb,
                                                    PropertyMap configuration,
                                                    ProgressMonitor pm) throws Exception {
        final Band classBand = classProduct.getBand("class_indices");
        final SampleCoding sampleCoding = classBand.getIndexCoding();
        final int classCount = sampleCoding.getSampleCount();

        try {
            pm.beginTask("Creating class view...", classCount);

            final RenderedImage classImage = classBand.getSourceImage();
            final Raster classImageData = classImage.getData();

            final int[] r = new int[classCount];
            final int[] g = new int[classCount];
            final int[] b = new int[classCount];

            // class index color = median RGB image color
            for (int k = 0; k < classCount; ++k) {
                final ArrayList<Integer> rList = new ArrayList<Integer>(100000);
                final ArrayList<Integer> gList = new ArrayList<Integer>(100000);
                final ArrayList<Integer> bList = new ArrayList<Integer>(100000);

                for (int y = 0; y < classImage.getHeight(); ++y) {
                    for (int x = 0; x < classImage.getWidth(); ++x) {
                        final int classIndex = classImageData.getSample(x, y, 0);
                        if (classIndex == k) {
                            rList.add(rgb.getSample(x, y, 0));
                            gList.add(rgb.getSample(x, y, 1));
                            bList.add(rgb.getSample(x, y, 2));
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
                pm.worked(1);
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
            return new ProductSceneView(new ProductSceneImage(classBand, configuration, ProgressMonitor.NULL));
        } finally {
            pm.done();
        }
    }

    private static RenderedImage createCloudAbundanceImage(Product reflectanceProduct,
                                                           String[] reflectanceBandNames,
                                                           Endmember[] endmembers) {
        final Map<String, Object> parameterMap = new HashMap<String, Object>(3);
        parameterMap.put("sourceBandNames", reflectanceBandNames);
        parameterMap.put("endmembers", endmembers);
        parameterMap.put("unmixingModelName", "Fully Constrained LSU");

        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        final Dimension tileSize = new Dimension(CloudProbabilityOpImage.TILE_W, CloudProbabilityOpImage.TILE_H);
        renderingHints.put(GPF.KEY_TILE_SIZE, tileSize);

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
        final Band[] bands = OpUtils.findBands(product, prefix);

        if (bands.length == 0) {
            throw new Exception(MessageFormat.format(
                    "could not find band with prefix = ''{0}'' and spectral wavelength = {1} nm", prefix, wavelength));
        }

        return bands[OpUtils.findBandIndex(bands, wavelength)];
    }

    private static class BrightnessComparator implements Comparator<EMCluster> {
        @Override
        public int compare(EMCluster c1, EMCluster c2) {
            return Double.compare(c2.getMean()[0], c1.getMean()[0]);
        }
    }
}
