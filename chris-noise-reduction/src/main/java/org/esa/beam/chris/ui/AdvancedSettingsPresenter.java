package org.esa.beam.chris.ui;

import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValueContainer;
import org.esa.beam.chris.operators.DestripingFactorsOp;
import org.esa.beam.chris.operators.DropoutCorrectionOp;
import org.esa.beam.framework.gpf.annotations.ParameterDefinitionFactory;

import java.util.HashMap;

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision:$ $Date:$
 */
public class AdvancedSettingsPresenter {
    private HashMap<String,Object> destripingParameter;
    private ValueContainer destripingContainer;
    private HashMap<String,Object> dropOutCorrectionParameter;
    private ValueContainer dropOutCorrectionContainer;

    public AdvancedSettingsPresenter() {
        Factory factory = new Factory(new ParameterDefinitionFactory());

        destripingParameter = new HashMap<String, Object>();
        destripingContainer = factory.createMapBackedValueContainer(DestripingFactorsOp.class, destripingParameter);

        dropOutCorrectionParameter = new HashMap<String, Object>();
        dropOutCorrectionContainer = factory.createMapBackedValueContainer(DropoutCorrectionOp.class, dropOutCorrectionParameter);

    }

    public ValueContainer getDestripingContainer() {
        return destripingContainer;
    }

    public HashMap<String, Object> getDestripingParameter() {
        return destripingParameter;
    }

    public HashMap<String, Object> getDropOutCorrectionParameter() {
        return dropOutCorrectionParameter;
    }

    public ValueContainer getDropOutCorrectionContainer() {
        return dropOutCorrectionContainer;
    }

    public AdvancedSettingsPresenter createCopy() {
        return new AdvancedSettingsPresenter();
    }
}
