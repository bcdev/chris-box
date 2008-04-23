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

import org.esa.beam.chris.operators.ExtractEndmembersOp;
import org.esa.beam.chris.operators.MakeClusterMapOp;
import org.esa.beam.chris.operators.internal.BandFilter;
import org.esa.beam.chris.operators.internal.ExclusiveMultiBandFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.operators.common.BandArithmeticOp;
import org.esa.beam.unmixing.Endmember;
import org.esa.beam.unmixing.SpectralUnmixingOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: $ $Date: $
 */
public class CloudLabeler {

    private final Product reflectanceProduct;
    private Product featureProduct;
    private Product clusterProduct;
    private Product clusterMapProduct;
    private int[] cloudClusterIndexes;
    private int[] backgroundClusterIndexes;
    private int[] surfaceClusterIndexes;
    private boolean computeAbundances;

    public CloudLabeler(Product reflectanceProduct) {
        this.reflectanceProduct = reflectanceProduct;
    }

    public Band getMembershipBand() {
        return clusterMapProduct.getBand("membership_mask");
    }

    public void processStepOne() throws OperatorException {
        // 1. Extract features
        featureProduct = createFeatureProduct();

        // 2. Find clusters
        clusterProduct = createClusterProduct();

        // 3. Cluster labeling
        final int[] backgroundIndexes = new int[0];
        clusterMapProduct = createClusterMapProduct(backgroundIndexes);
    }

    public void processLabelingStep(int[] backgroundIndexes) throws OperatorException {
        for (Band band : clusterMapProduct.getBands()) {
            if (band.getName().startsWith("prob")) {
                final MakeClusterMapOp.ProbabilityImageBand probBand = (MakeClusterMapOp.ProbabilityImageBand) band;
                probBand.update(backgroundIndexes);
            }
        }
        final MakeClusterMapOp.MembershipImageBand membershipBand = (MakeClusterMapOp.MembershipImageBand) clusterMapProduct.getBand("membership_mask");
        membershipBand.update();
    }

    public int[] getCloudClusterIndexes() {
        return cloudClusterIndexes;
    }

    public int[] getSurfaceClusterIndexes() {
        return surfaceClusterIndexes;
    }

    public int[] getBackgroundClusterIndexes() {
        return backgroundClusterIndexes;
    }

    public boolean getComputeAbundances() {
        return computeAbundances;
    }

    public Product processStepTwo(int[] cloudClusterIndexes, int[] backgroundClusterIndexes, int[] surfaceClusterIndexes, boolean computeAbundances) throws OperatorException {
        this.cloudClusterIndexes = cloudClusterIndexes;
        this.backgroundClusterIndexes = backgroundClusterIndexes;
        this.surfaceClusterIndexes = surfaceClusterIndexes;
        this.computeAbundances = computeAbundances;

        // 4. Cluster probabilities
        final Product cloudProbabilityProduct = createCloudProbabilityProduct(cloudClusterIndexes);
        if (!computeAbundances) {
            return cloudProbabilityProduct;
        }

        // 5. Endmember extraction
        final ExtractEndmembersOp endmemberOp = new ExtractEndmembersOp(reflectanceProduct, featureProduct, clusterMapProduct, cloudClusterIndexes,
                                                                        surfaceClusterIndexes);
        final Endmember[] endmembers = (Endmember[]) endmemberOp.getTargetProperty("endmembers");

        // 6. Cloud abundances
        final Product cloudAbundancesProduct = createCloudAbundancesProduct(endmembers);

        // 7. Cloud probability * cloud abundance
        return createCloudMaskProduct(cloudProbabilityProduct, cloudAbundancesProduct);
    }

    private Product createCloudMaskProduct(Product cloudProbabilityProduct, Product cloudAbundancesProduct) {
        BandArithmeticOp.BandDescriptor[] bandDescriptors = new BandArithmeticOp.BandDescriptor[1];
        bandDescriptors[0] = new BandArithmeticOp.BandDescriptor();
        bandDescriptors[0].name = "cloud_probability";
        bandDescriptors[0].expression = "$probability.cloud_probability * $abundance.cloud_abundance";
        bandDescriptors[0].type = ProductData.TYPESTRING_FLOAT32;
        final Map<String, Object> cloudMaskParameterMap = new HashMap<String, Object>();
        cloudMaskParameterMap.put("targetBandDescriptors", bandDescriptors);
        final Map<String, Product> cloudMaskSourceMap = new HashMap<String, Product>();
        cloudMaskSourceMap.put("probability", cloudProbabilityProduct);
        cloudMaskSourceMap.put("abundance", cloudAbundancesProduct);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(BandArithmeticOp.class),
                                 cloudMaskParameterMap, cloudMaskSourceMap);
    }

    private Product createCloudAbundancesProduct(Endmember[] endmembers) {
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
        final String[] reflBands = findBandNames(reflectanceProduct, "reflectance_", bandFilter);
        final Map<String, Object> unmixingParameterMap = new HashMap<String, Object>();
        unmixingParameterMap.put("sourceBandNames", reflBands);
        unmixingParameterMap.put("endmembers", endmembers);
        unmixingParameterMap.put("unmixingModelName", "Fully Constrained LSU");

        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class),
                                 unmixingParameterMap, reflectanceProduct);
    }

    private Product createCloudProbabilityProduct(int[] cloudClusterIndexes) {
        final Map<String, Object> accumulateOpParameterMap = new HashMap<String, Object>();

        final String[] bandNames = this.clusterMapProduct.getBandNames();
        final List<String> bandNameList = new ArrayList<String>();
        for (int i = 0; i < bandNames.length; ++i) {
            if (bandNames[i].startsWith("prob")) {
                if (isContained(i, cloudClusterIndexes)) {
                    bandNameList.add(bandNames[i]);
                }
            }
        }
        final String[] cloudProbabilityBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        accumulateOpParameterMap.put("sourceBands", cloudProbabilityBandNames);
        accumulateOpParameterMap.put("targetBand", "cloud_probability");
        // todo - product name and type

        return GPF.createProduct("chris.Accumulate", accumulateOpParameterMap, clusterMapProduct);
    }

    private Product createClusterMapProduct(int[] backgroundIndexes) {
        final Map<String, Object> clusterMapParameter = new HashMap<String, Object>();
        clusterMapParameter.put("backgroundClusterIndexes", backgroundIndexes);
        return GPF.createProduct("chris.MakeClusterMap", clusterMapParameter, clusterProduct);
    }

    private Product createClusterProduct() {
        final Map<String, Object> findClustersOpParameterMap = new HashMap<String, Object>();
        findClustersOpParameterMap.put("sourceBandNames", new String[]{"brightness_vis",
                "brightness_nir",
                "whiteness_vis",
                "whiteness_nir",
                "wv"});
        findClustersOpParameterMap.put("clusterCount", 14);
        findClustersOpParameterMap.put("iterationCount", 40);

        return GPF.createProduct("chris.FindClusters",
                                 findClustersOpParameterMap,
                                 featureProduct);
    }

    private static String[] findBandNames(Product product, String prefix, BandFilter filter) {
        final List<String> nameList = new ArrayList<String>();

        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix) && filter.accept(band)) {
                nameList.add(band.getName());
            }
        }

        return nameList.toArray(new String[nameList.size()]);
    }

    private Product createFeatureProduct() {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        return GPF.createProduct("chris.ExtractFeatures",
                                 parameterMap,
                                 reflectanceProduct);
    }

    private static boolean isContained(int index, int[] indexes) {
        for (int i : indexes) {
            if (index == i) {
                return true;
            }
        }

        return false;
    }

    public void dispose() {
        if (featureProduct != null) {
            featureProduct.dispose();
            featureProduct = null;
        }
        if (clusterProduct != null) {
            clusterProduct.dispose();
            clusterProduct = null;
        }
        if (clusterMapProduct != null) {
            clusterMapProduct.dispose();
            clusterMapProduct = null;
        }
    }
}
