package org.esa.beam.chris.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
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
        try {
            destripingPropertyContainer.setDefaultValues();
            dropoutCorrectionPropertyContainer.setDefaultValues();
        } catch (ValidationException e) {
            throw new IllegalStateException(e);
        }
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
