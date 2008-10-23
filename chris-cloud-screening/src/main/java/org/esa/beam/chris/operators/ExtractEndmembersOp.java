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
import org.esa.beam.chris.operators.internal.Clusterer;
import org.esa.beam.chris.operators.internal.ExclusiveIndexFilter;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.cluster.EMCluster;
import org.esa.beam.cluster.ProbabilityCalculator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.SampleCoding;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProperty;
import org.esa.beam.unmixing.Endmember;

import java.awt.*;
import java.util.ArrayList;

/**
 * Extracts endmembers for calculating cloud abundances.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ExtractEndmembers",
                  version = "1.0",
                  authors = "Ralf Quast, Marco Zühlke",
                  copyright = "(c) 2008 Brockmann Consult",
                  description = "Extracts endmembers for calculating cloud abundances.",
                  internal = true)
public class ExtractEndmembersOp extends Operator {

    private static final BandFilter BAND_FILTER = new ExclusiveMultiBandFilter(new double[][]{
            {400.0, 440.0},
            {590.0, 600.0},
            {630.0, 636.0},
            {648.0, 658.0},
            {686.0, 709.0},
            {792.0, 799.0},
            {756.0, 775.0},
            {808.0, 840.0},
            {885.0, 985.0},
            {985.0, 1010.0}});

    @SourceProduct(alias = "reflectances")
    private Product reflectanceProduct;
    @SourceProduct(alias = "features")
    private Product featureProduct;
    @SourceProduct(alias = "classification")
    private Product classificationProduct;

    @Parameter
    private Band[] featureBands;
    @Parameter
    private EMCluster[] clusters;
    @Parameter
    private boolean[] cloudClusters;
    @Parameter
    private boolean[] ignoredClusters;

    @TargetProperty
    private Endmember[] endmembers;
    @TargetProperty
    private String[] reflectanceBandNames;

    /**
     * Constructs a new instance of this class.
     */
    public ExtractEndmembersOp() {
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param reflectanceProduct    the reflectance product.
     * @param featureProduct        the feature product.
     * @param classificationProduct the cluster product.
     * @param featureBands          the feature bands.
     * @param clusters              the clusters.
     */
    public ExtractEndmembersOp(Product reflectanceProduct, Product featureProduct, Product classificationProduct,
                               Band[] featureBands, EMCluster[] clusters,
                               boolean[] cloudClusters, boolean[] ignoredClusters
    ) {
        this.reflectanceProduct = reflectanceProduct;
        this.featureProduct = featureProduct;
        this.classificationProduct = classificationProduct;
        this.featureBands = featureBands;
        this.clusters = clusters;
        this.cloudClusters = cloudClusters;
        this.ignoredClusters = ignoredClusters;
    }

    @Override
    public void initialize() throws OperatorException {
        extractEndmembers(cloudClusters, ignoredClusters, ProgressMonitor.NULL);
        setTargetProduct(new Product("EMPTY", "EMPTY_TYPE", 0, 0));
    }

    private void extractEndmembers(final boolean[] cloudClusters, final boolean[] ignoredClusters,
                                   ProgressMonitor pm) {
        final Band[] reflectanceBands = OpUtils.findBands(reflectanceProduct, "toa_refl", BAND_FILTER);
        final Band brBand = featureProduct.getBand("brightness_vis");
        final Band whBand = featureProduct.getBand("whiteness_vis");
        final Band clBand = classificationProduct.getBand("class_indices");

        // set reflectance band names
        reflectanceBandNames = new String[reflectanceBands.length];
        for (int i = 0; i < reflectanceBands.length; ++i) {
            reflectanceBandNames[i] = reflectanceBands[i].getName();
        }

        final double[] wavelengths = OpUtils.getWavelenghts(reflectanceBands);
        final ArrayList<Endmember> endmemberList = new ArrayList<Endmember>();

        // extract cloud endmember
        final double[] cloudReflectances = extractCloudReflectances(reflectanceBands, brBand, whBand, clBand, pm);
        endmemberList.add(new Endmember("cloud", wavelengths, cloudReflectances));

        // extract surface endmembers
        final double[][] surfaceReflectances = extractSurfaceReflectances(reflectanceBands, pm);
        final SampleCoding sampleCoding = clBand.getSampleCoding(); // actually an index-coding
        for (int k = 0; k < clusters.length; ++k) {
            if (!cloudClusters[k] && !ignoredClusters[k]) {
                endmemberList.add(new Endmember(sampleCoding.getSampleName(k), wavelengths, surfaceReflectances[k]));
            }
        }

        endmembers = endmemberList.toArray(new Endmember[endmemberList.size()]);
    }

    private double[] extractCloudReflectances(Band[] reflectanceBands, Band brBand, Band whBand, Band clBand,
                                              ProgressMonitor pm) {
        final int h = reflectanceBands[0].getRasterHeight();
        final int w = reflectanceBands[0].getRasterWidth();

        int cloudEndmemberX = -1;
        int cloudEndmemberY = -1;
        double maxRatio = 0.0;

        final Rectangle sourceRectangle = new Rectangle(0, 0, w, h);
        final Tile clTile = getSourceTile(clBand, sourceRectangle, pm);
        final Tile brTile = getSourceTile(brBand, sourceRectangle, pm);
        final Tile whTile = getSourceTile(whBand, sourceRectangle, pm);

        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                if (cloudClusters[clTile.getSampleInt(x, y)]) {
                    if (whTile.getSampleDouble(x, y) > 0.0) {
                        final double ratio = brTile.getSampleDouble(x, y) / whTile.getSampleDouble(x, y);
                        if (cloudEndmemberX == -1 || cloudEndmemberY == -1 || ratio > maxRatio) {
                            cloudEndmemberX = x;
                            cloudEndmemberY = y;
                            maxRatio = ratio;
                        }
                    }
                }
            }
            checkForCancelation(pm);
        }

        final double[] cloudReflectances = new double[reflectanceBands.length];
        final Rectangle rectangle = new Rectangle(cloudEndmemberX, cloudEndmemberY, 1, 1);

        for (int i = 0; i < reflectanceBands.length; ++i) {
            final Tile reflectanceTile = getSourceTile(reflectanceBands[i], rectangle, pm);
            cloudReflectances[i] = reflectanceTile.getSampleDouble(cloudEndmemberX, cloudEndmemberY);
        }

        return cloudReflectances;
    }

    private double[][] extractSurfaceReflectances(Band[] reflectanceBands, ProgressMonitor pm) {
        final int h = reflectanceBands[0].getRasterHeight();
        final int w = reflectanceBands[0].getRasterWidth();

        final double[][] surfaceReflectances = new double[clusters.length][reflectanceBands.length];
        final int[] count = new int[clusters.length];
        final ProbabilityCalculator calculator = Clusterer.createProbabilityCalculator(clusters);
        final double[] posteriors = new double[clusters.length];
        final double[] features = new double[featureBands.length];
        final ExclusiveIndexFilter clusterFilter = new ExclusiveIndexFilter(ignoredClusters);

        final Rectangle rectangle = new Rectangle(0, 0, w, h);
        final Tile[] featureTiles = new Tile[featureBands.length];
        for (int i = 0; i < featureBands.length; ++i) {
            featureTiles[i] = getSourceTile(featureBands[i], rectangle, pm);
        }

        final Tile[] reflectanceTiles = new Tile[reflectanceBands.length];
        for (int i = 0; i < reflectanceBands.length; ++i) {
            reflectanceTiles[i] = getSourceTile(reflectanceBands[i], rectangle, pm);
        }

        for (int y = 0; y < h; ++y) {
            for (int k = 0; k < clusters.length; ++k) {
                if (!cloudClusters[k] && !ignoredClusters[k]) {
                    for (int x = 0; x < w; ++x) {
                        for (int i = 0; i < featureBands.length; ++i) {
                            features[i] = featureTiles[i].getSampleDouble(x, y);
                        }
                        calculator.calculate(features, posteriors, clusterFilter);
                        if (posteriors[k] > 0.5) {
                            for (int i = 0; i < reflectanceBands.length; ++i) {
                                surfaceReflectances[k][i] += reflectanceTiles[i].getSampleDouble(x, y);
                            }
                            ++count[k];
                        }
                    }
                }
            }
            checkForCancelation(pm);
        }

        for (int k = 0; k < clusters.length; ++k) {
            if (!cloudClusters[k] && !ignoredClusters[k]) {
                if (count[k] > 0) {
                    for (int i = 0; i < reflectanceBands.length; ++i) {
                        surfaceReflectances[k][i] /= count[k];
                    }
                }
            }
        }

        return surfaceReflectances;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractEndmembersOp.class);
        }
    }
}
