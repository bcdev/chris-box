package org.esa.beam.chris.ui;

import com.bc.ceres.binding.Accessor;
import com.bc.ceres.binding.Factory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDefinition;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.converters.BooleanConverter;
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

    private boolean slitApplied = true;
    private ValueContainer slitAppliedContainer;

    public AdvancedSettingsPresenter() {
        Factory factory = new Factory(new ParameterDefinitionFactory());

        destripingParameter = new HashMap<String, Object>();
        destripingContainer = factory.createMapBackedValueContainer(DestripingFactorsOp.class, destripingParameter);

        dropOutCorrectionParameter = new HashMap<String, Object>();
        dropOutCorrectionContainer = factory.createMapBackedValueContainer(DropoutCorrectionOp.class, dropOutCorrectionParameter);

        slitAppliedContainer = new ValueContainer();
        ValueDefinition valueDefinition = new ValueDefinition("slitApplied", Boolean.class);
        valueDefinition.setDefaultValue(Boolean.TRUE);
        valueDefinition.setConverter(new BooleanConverter());
        valueDefinition.setNotNull(true);
        Accessor slitAppliedAccessor = null;
        try {
            slitAppliedAccessor = new ClassFieldAccessor(this, this.getClass().getDeclaredField("slitApplied"));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        slitAppliedContainer.addModel(new ValueModel(valueDefinition, slitAppliedAccessor));
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

    public boolean isSlitApplied() {
        return slitApplied;
    }

    public ValueContainer getSlitAppliedContainer() {
        return slitAppliedContainer;
    }

    public AdvancedSettingsPresenter createCopy() {
        return new AdvancedSettingsPresenter();
    }
}
