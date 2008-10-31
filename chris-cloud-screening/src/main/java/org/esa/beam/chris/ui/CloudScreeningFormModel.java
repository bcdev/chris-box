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

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloud screening form model.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CloudScreeningFormModel {

    private static final String BR_VIS_NAME = "brightness_vis";
    private static final String WH_VIS_NAME = "whiteness_vis";
    private static final String BR_NIR_NAME = "brightness_nir";
    private static final String WH_NIR_NAME = "whiteness_nir";
    private static final String WV_NAME = "wv";
    private static final String O2_NAME = "o2";

    private final ProductBlock productBlock;
    private final ParameterBlock parameterBlock;

    private final ValueContainer productValueContainer;
    private final ValueContainer parameterValueContainer;

    private boolean featureAvailability;

    CloudScreeningFormModel() {
        productBlock = new ProductBlock();
        parameterBlock = new ParameterBlock();

        final ParameterDescriptorFactory descriptorFactory = new ParameterDescriptorFactory();
        productValueContainer = ValueContainer.createObjectBacked(productBlock, descriptorFactory);
        parameterValueContainer = ValueContainer.createObjectBacked(parameterBlock, descriptorFactory);
    }

    public final ValueContainer getProductValueContainer() {
        return productValueContainer;
    }

    public final ValueContainer getParameterValueContainer() {
        return parameterValueContainer;
    }

    final Product getRadianceProduct() {
        return productBlock.radianceProduct;
    }

    void setRadianceProduct(Product radianceProduct) {
        setValueContainerValue(productValueContainer, "radianceProduct", radianceProduct);
    }

    boolean updateFeatureAvailability(Product radianceProduct) {
        featureAvailability = radianceProduct != null && radianceProduct.getProductType().matches("CHRIS_M[15].*");
        if (!featureAvailability) {
            setValueContainerValue(parameterValueContainer, "useWv", false);
            setValueContainerValue(parameterValueContainer, "useO2", false);
        }

        return featureAvailability;
    }

    final boolean getUseNirBrightness() {
        return parameterBlock.useNirBr;
    }

    final void setUseNirBrightness(boolean b) {
        setValueContainerValue(parameterValueContainer, "useNirBr", b);
    }

    final boolean getUseNirWhiteness() {
        return parameterBlock.useNirWh;
    }

    final void setUseNirWhiteness(boolean b) {
        setValueContainerValue(parameterValueContainer, "useNirWh", b);
    }

    final boolean getUseWv() {
        return parameterBlock.useWv;
    }

    void setUseWv(boolean b) {
        if (featureAvailability) {
            setValueContainerValue(parameterValueContainer, "useWv", b);
        }
    }

    final boolean getUseO2() {
        return parameterBlock.useO2;
    }

    void setUseO2(boolean b) {
        if (featureAvailability) {
            setValueContainerValue(parameterValueContainer, "useO2", b);
        }
    }

    final int getClusterCount() {
        return parameterBlock.clusterCount;
    }

    final void setClusterCount(int clusterCount) {
        setValueContainerValue(parameterValueContainer, "clusterCount", clusterCount);
    }

    final int getIterationCount() {
        return parameterBlock.iterationCount;
    }

    final void setIterationCount(int iterationCount) {
        setValueContainerValue(parameterValueContainer, "iterationCount", iterationCount);
    }

    final int getSeed() {
        return parameterBlock.seed;
    }

    final void setSeed(int seed) {
        setValueContainerValue(parameterValueContainer, "seed", seed);
    }

    String[] getFeatureBandNames() {
        final List<String> nameList = new ArrayList<String>(6);

        nameList.add(BR_VIS_NAME);
        nameList.add(WH_VIS_NAME);

        if (getUseNirBrightness()) {
            nameList.add(BR_NIR_NAME);
        }
        if (getUseNirWhiteness()) {
            nameList.add(WH_NIR_NAME);
        }
        if (getUseWv()) {
            nameList.add(WV_NAME);
        }
        if (getUseO2()) {
            nameList.add(O2_NAME);
        }

        return nameList.toArray(new String[nameList.size()]);
    }

    private static void setValueContainerValue(ValueContainer valueContainer, String name, Object value) {
        try {
            valueContainer.setValue(name, value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static class ProductBlock {
        @SourceProduct
        private Product radianceProduct;
    }

    /**
     * Cloud screening parameters.
     */
    private static class ParameterBlock {
        @Parameter(label = "Number of clusters", defaultValue = "14", interval = "[2,99]")
        private int clusterCount;
        @Parameter(label = "Number of iterations", defaultValue = "30", interval = "[1,999]")
        private int iterationCount;
        @Parameter(label = "Random seed",
                   defaultValue = "31415",
                   description = "The seed used for initializing the EM clustering algorithm.")
        private int seed;

        @Parameter(label = "Use NIR brightness", defaultValue = "true")
        private boolean useNirBr = true;
        @Parameter(label = "Use NIR whiteness", defaultValue = "true")
        private boolean useNirWh = true;
        @Parameter(label = "Use atmospheric water vapour feature", defaultValue = "false")
        private boolean useWv = false;
        @Parameter(label = "Use atmospheric oxygen feature", defaultValue = "false")
        private boolean useO2 = false;
    }
}
