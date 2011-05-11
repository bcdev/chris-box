/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.chris.operators.ComputeDestripingFactorsOp;
import org.esa.beam.chris.operators.CorrectDropoutsOp;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class AdvancedSettingsPresenter {

    private Map<String, Object> destripingParameterMap;
    private Map<String, Object> dropoutCorrectionParameterMap;
    private PropertyContainer destripingPropertyContainer;
    private PropertyContainer dropoutCorrectionPropertyContainer;

    public AdvancedSettingsPresenter() {
        this (new HashMap<String, Object>(7), new HashMap<String, Object>(7));
        destripingPropertyContainer.setDefaultValues();
        dropoutCorrectionPropertyContainer.setDefaultValues();
    }

    private AdvancedSettingsPresenter(Map<String, Object> destripingParameterMap,
                                      Map<String, Object> dropoutCorrectionParameterMap) {
        this.destripingParameterMap = new HashMap<String, Object>(destripingParameterMap);
        this.dropoutCorrectionParameterMap = new HashMap<String, Object>(dropoutCorrectionParameterMap);

        initValueContainers();
    }

    private void initValueContainers() {
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        destripingPropertyContainer =
            PropertyContainer.createMapBacked(destripingParameterMap, ComputeDestripingFactorsOp.class, parameterDescriptorFactory);
        dropoutCorrectionPropertyContainer =
            PropertyContainer.createMapBacked(dropoutCorrectionParameterMap, CorrectDropoutsOp.class, parameterDescriptorFactory);
    }

    public PropertyContainer getDestripingPropertyContainer() {
        return destripingPropertyContainer;
    }

    public Map<String, Object> getDestripingParameterMap() {
        return destripingParameterMap;
    }

    public Map<String, Object> getDropoutCorrectionParameterMap() {
        return dropoutCorrectionParameterMap;
    }

    public PropertyContainer getDropoutCorrectionPropertyContainer() {
        return dropoutCorrectionPropertyContainer;
    }

    // todo - replace with clone()
    public AdvancedSettingsPresenter createCopy() {
        return new AdvancedSettingsPresenter(destripingParameterMap, dropoutCorrectionParameterMap);
    }
}
