package org.esa.beam.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.annotations.Parameter;

public class DropoutCorrectionOp  extends AbstractOperator {

    @Parameter(defaultValue = "5", interval = "(1, *)")
    private int includeSpectralBandsCount = 5;

    @Parameter(defaultValue = "4", valueSet = {"2", "4", "8"})
    private int neighbourhoodType;

    public DropoutCorrectionOp(OperatorSpi operatorSpi) {
        super(operatorSpi);
    }

    @Override
    protected Product initialize(ProgressMonitor progressMonitor) throws OperatorException {
        return null;
    }

    @Override
    public void computeBand(Raster raster, ProgressMonitor progressMonitor) throws OperatorException {
    }
}
