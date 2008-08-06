package org.esa.beam.chris.ui;

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Action for extracting features needed for cloud screening.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ExtractFeaturesAction extends AbstractVisatAction {
    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ExtractFeatures",
                                                     getAppContext(),
                                                     "CHRIS/PROBA Feature Extraction",
                                                     "chrisExtractFeaturesTools");
        dialog.setTargetProductNameSuffix("_FEAT");
        dialog.show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                selectedProduct.getProductType().startsWith("CHRIS_M") &&
                        OpUtils.findBands(selectedProduct, "toa_refl").length > 17;

        setEnabled(enabled);
    }
}
