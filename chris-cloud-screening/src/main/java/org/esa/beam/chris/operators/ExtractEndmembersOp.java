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
import org.esa.beam.chris.operators.internal.ExclusiveMultiBandFilter;
import org.esa.beam.chris.util.BandFilter;
import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
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

    @SourceProduct(alias = "reflectances")
    private Product reflectanceProduct;
    @SourceProduct(alias = "features")
    private Product featureProduct;
    @SourceProduct(alias = "clusters")
    private Product clusterMapProduct;

    @Parameter(alias = "cloudClusterIndexes", notEmpty = true, notNull = true)
    private int[] cloudClusterIndexes;
    @Parameter(alias = "surfaceClusterIndexes", notEmpty = true, notNull = true)
    private int[] surfaceClusterIndexes;

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
     * @param clusterMapProduct     the cluster product.
     * @param cloudClusterIndexes   the cloud cluster indexes.
     * @param surfaceClusterIndexes the surface cluster indexes.
     */
    public ExtractEndmembersOp(Product reflectanceProduct, Product featureProduct, Product clusterMapProduct,
                               int[] cloudClusterIndexes, int[] surfaceClusterIndexes) {
        this.reflectanceProduct = reflectanceProduct;
        this.featureProduct = featureProduct;
        this.clusterMapProduct = clusterMapProduct;
        this.cloudClusterIndexes = cloudClusterIndexes;
        this.surfaceClusterIndexes = surfaceClusterIndexes;
    }

    @Override
    public void initialize() throws OperatorException {
        setTargetProperties(ProgressMonitor.NULL);
        setTargetProduct(new Product("EMPTY", "EMPTY_TYPE", 0, 0));
    }

    // todo - refactor
    private void setTargetProperties(ProgressMonitor pm) {
        final Band brightnessBand = featureProduct.getBand("brightness_vis");
        final Band whitenessBand = featureProduct.getBand("whiteness_vis");
        final Band clusterMapBand = clusterMapProduct.getBand("class_indices");

        final IndexCoding indexCoding = (IndexCoding) clusterMapBand.getSampleCoding();
        final String[] labels = indexCoding.getIndexNames();

        final BandFilter bandFilter = new ExclusiveMultiBandFilter(new double[][]{
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
        final Band[] reflectanceBands = OpUtils.findBands(reflectanceProduct, "toa_refl", bandFilter);

        reflectanceBandNames = new String[reflectanceBands.length];
        for (int i = 0; i < reflectanceBands.length; ++i) {
            reflectanceBandNames[i] = reflectanceBands[i].getName();
        }

        final double[] wavelengths = getSpectralWavelengths(reflectanceBands);
        endmembers = new Endmember[surfaceClusterIndexes.length + 1];

        final int height = clusterMapBand.getRasterHeight();
        final int width = clusterMapBand.getRasterWidth();

        int cloudEndmemberX = -1;
        int cloudEndmemberY = -1;
        double maxRatio = 0.0;

        for (int y = 0; y < height; ++y) {
            final Rectangle rectangle = new Rectangle(0, y, width, 1);
            final Tile membershipTile = getSourceTile(clusterMapBand, rectangle, pm);
            final Tile brightnessTile = getSourceTile(brightnessBand, rectangle, pm);
            final Tile whitenessTile = getSourceTile(whitenessBand, rectangle, pm);

            for (int x = 0; x < width; ++x) {
                if (isContained(membershipTile.getSampleInt(x, y), cloudClusterIndexes)) {
                    if (whitenessTile.getSampleDouble(x, y) > 0.0) {
                        final double ratio = brightnessTile.getSampleDouble(x, y) / whitenessTile.getSampleDouble(x, y);
                        if (cloudEndmemberX == -1 || cloudEndmemberY == -1 || ratio > maxRatio) {
                            cloudEndmemberX = x;
                            cloudEndmemberY = y;
                            maxRatio = ratio;
                        }
                    }
                }
            }
        }
        final double[] reflectances = new double[reflectanceBands.length];

        for (int i = 0; i < reflectanceBands.length; ++i) {
            final Rectangle rectangle = new Rectangle(cloudEndmemberX, cloudEndmemberY, 1, 1);
            final Tile reflectanceTile = getSourceTile(reflectanceBands[i], rectangle, pm);

            reflectances[i] = reflectanceTile.getSampleDouble(cloudEndmemberX, cloudEndmemberY);
        }
        final Endmember em = new Endmember("cloud", wavelengths, reflectances);
        endmembers[0] = em;


        final Band[] probabilityBands = OpUtils.findBands(clusterMapProduct, "probability");
        System.out.println("probabilityBands.length = " + probabilityBands.length);
        ///////////////////////
        final double[][] meanReflectances = new double[probabilityBands.length][reflectanceBands.length];
        final int[] count = new int[probabilityBands.length];

        for (int y = 0; y < height; ++y) {
            final Rectangle rectangle = new Rectangle(0, y, width, 1);

            for (int k = 0; k < probabilityBands.length; ++k) {
                if (isContained(k, surfaceClusterIndexes)) {
                    final Tile probabilityTile = getSourceTile(probabilityBands[k], rectangle, pm);
                    for (int x = 0; x < width; ++x) {
                        if (probabilityTile.getSampleDouble(x, y) > 0.5) {
                            for (int i = 0; i < reflectanceBands.length; ++i) {
                                final Tile reflectanceTile = getSourceTile(reflectanceBands[i], rectangle, pm);
                                meanReflectances[k][i] += reflectanceTile.getSampleDouble(x, y);
                            }
                            ++count[k];
                        }
                    }
                }
            }
        }

        for (int k = 0, j = 0; k < probabilityBands.length; ++k) {
            if (count[k] > 0) {
                for (int i = 0; i < reflectanceBands.length; ++i) {
                    meanReflectances[k][i] /= count[k];
                }
                final int index = surfaceClusterIndexes[j];
                final String label = labels[index];
                endmembers[j + 1] = new Endmember(label, wavelengths, meanReflectances[k]);
                ++j;
            }
        }
    }

    private static double[] getSpectralWavelengths(Band[] bands) {
        final double[] wavelengths = new double[bands.length];
        for (int i = 0; i < bands.length; ++i) {
            wavelengths[i] = (double) bands[i].getSpectralWavelength();
        }
        return wavelengths;
    }

    private static boolean isContained(int index, int[] indexes) {
        for (int i : indexes) {
            if (index == i) {
                return true;
            }
        }

        return false;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractEndmembersOp.class);
        }
    }
}
