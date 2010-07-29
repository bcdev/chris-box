/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.chris.ui;

import com.bc.ceres.binding.PropertyContainer;
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
class ScreeningFormModel {

    private static final String BR_VIS_NAME = "brightness_vis";
    private static final String WH_VIS_NAME = "whiteness_vis";
    private static final String BR_NIR_NAME = "brightness_nir";
    private static final String WH_NIR_NAME = "whiteness_nir";
    private static final String WV_NAME = "wv";
    private static final String O2_NAME = "o2";

    private final ProductBlock productBlock;
    private final ParameterBlock parameterBlock;

    private final PropertyContainer productPropertyContainer;
    private final PropertyContainer parameterPropertyContainer;

    ScreeningFormModel() {
        productBlock = new ProductBlock();
        parameterBlock = new ParameterBlock();

        final ParameterDescriptorFactory descriptorFactory = new ParameterDescriptorFactory();
        productPropertyContainer = PropertyContainer.createObjectBacked(productBlock, descriptorFactory);
        parameterPropertyContainer = PropertyContainer.createObjectBacked(parameterBlock, descriptorFactory);
    }

    public final PropertyContainer getProductPropertyContainer() {
        return productPropertyContainer;
    }

    public final PropertyContainer getParameterPropertyContainer() {
        return parameterPropertyContainer;
    }

    final Product getSourceProduct() {
        return productBlock.sourceProduct;
    }

    final boolean getUseNirBrightness() {
        return parameterBlock.useNirBr;
    }

    final boolean getUseNirWhiteness() {
        return parameterBlock.useNirWh;
    }

    final boolean getUseWv() {
        return parameterBlock.useWv;
    }

    final boolean getUseO2() {
        return parameterBlock.useO2;
    }

    final int getClusterCount() {
        return parameterBlock.clusterCount;
    }

    final int getIterationCount() {
        return parameterBlock.iterationCount;
    }

    final int getSeed() {
        return parameterBlock.seed;
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

    private static class ProductBlock {
        @SourceProduct
        private Product sourceProduct;
    }

    /**
     * Cloud screening parameters.
     */
    private static class ParameterBlock {
        @Parameter(label = "Number of clusters", defaultValue = "14", interval = "[2,99]")
        private int clusterCount = 14;
        @Parameter(label = "Number of iterations", defaultValue = "30", interval = "[1,999]")
        private int iterationCount = 30;
        @Parameter(label = "Random seed",
                   defaultValue = "31415",
                   description = "The seed used for initializing the EM clustering algorithm.")
        private int seed = 31415;

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
