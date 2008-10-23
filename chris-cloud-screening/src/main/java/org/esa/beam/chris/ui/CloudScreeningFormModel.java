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
import org.esa.beam.framework.datamodel.Band;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cloud screening form model.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class CloudScreeningFormModel {

    private final ParameterBlock parameterBlock;
    private final ValueContainer valueContainer;

    CloudScreeningFormModel() {
        parameterBlock = new ParameterBlock();
        valueContainer = ValueContainer.createObjectBacked(parameterBlock);
    }

    final Product getRadianceProduct() {
        return parameterBlock.radianceProduct;
    }

    void setRadianceProduct(Product radianceProduct) {
        setValueContainerValue("radianceProduct", radianceProduct);
        if (!isAtmosphericAbsorptionAvailable()) {
            setValueContainerValue("wvSelected", false);
            setValueContainerValue("o2Selected", false);
        }
    }

    final boolean isNirBrightnessSelected() {
        return parameterBlock.isNirBrightnessSelected;
    }

    final void setNirBrightnessSelected(boolean select) {
        setValueContainerValue("nirBrightnessSelected", select);
    }

    final boolean isNirWhitenessSelected() {
        return parameterBlock.isNirWhitenessSelected;
    }

    final void setNirWhitenessSelected(boolean select) {
        setValueContainerValue("nirWhitenessSelected", select);
    }

    final boolean isWvSelected() {
        return parameterBlock.wvSelected;
    }

    void setWvSelected(boolean wvSelected) {
        if (isAtmosphericAbsorptionAvailable()) {
            setValueContainerValue("wvSelected", wvSelected);
        }
    }

    final boolean isO2Selected() {
        return parameterBlock.o2Selected;
    }

    void setO2Selected(boolean o2Selected) {
        if (isAtmosphericAbsorptionAvailable()) {
            setValueContainerValue("o2Selected", o2Selected);
        }
    }

    boolean isAtmosphericAbsorptionAvailable() {
        final Product radianceProduct = getRadianceProduct();
        return radianceProduct != null && radianceProduct.getProductType().matches("CHRIS_M[15].*");
    }

    final int getClusterCount() {
        return parameterBlock.clusterCount;
    }

    final void setClusterCount(int clusterCount) {
        setValueContainerValue("clusterCount", clusterCount);
    }

    final int getIterationCount() {
        return parameterBlock.iterationCount;
    }

    final void setIterationCount(int iterationCount) {
        setValueContainerValue("iterationCount", iterationCount);
    }

    final public int getSeed() {
        return parameterBlock.seed;
    }

    final public void setSeed(int seed) {
        setValueContainerValue("seed", seed);
    }

    String[] getFeatureBandNames() {
        final List<String> nameList = new ArrayList<String>(2);
        Collections.addAll(nameList, "brightness_vis", "whiteness_vis");

        if (isNirBrightnessSelected()) {
            nameList.add("brightness_nir");
        }
        if (isNirWhitenessSelected()) {
            nameList.add("whiteness_nir");
        }
        if (isWvSelected()) {
            nameList.add("wv");
        }
        if (isO2Selected()) {
            nameList.add("o2");
        }

        return nameList.toArray(new String[nameList.size()]);
    }

    private void setValueContainerValue(String name, Object value) {
        try {
            valueContainer.setValue(name, value);
        } catch (ValidationException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Cloud screening parameters.
     */
    private static class ParameterBlock {
        private Product radianceProduct;

        private boolean isNirBrightnessSelected = true;
        private boolean isNirWhitenessSelected = true;
        private boolean wvSelected = false;
        private boolean o2Selected = false;

        private int clusterCount = 14;
        private int iterationCount = 60;
        private int seed = 31415;
    }
}
