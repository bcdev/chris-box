package org.esa.beam.chris.ui;

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

/**
 * Action for computing TOA reflectances.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeToaReflectancesAction extends AbstractVisatAction {
    private static final String DIALOG_TITLE = "CHRIS/PROBA TOA Reflectance Computation";
    private static final String DIALOG_HELP_ID = "chrisToaReflectanceComputationTool";

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ComputeToaReflectances",
                                                     getAppContext(),
                                                     DIALOG_TITLE,
                                                     DIALOG_HELP_ID);
        dialog.getJDialog().setName("chrisToaReflectanceComputationDialog");
        dialog.setTargetProductNameSuffix("_TOA_REFL");
        dialog.show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                selectedProduct.getProductType().startsWith("CHRIS_M") &&
                        OpUtils.findBands(selectedProduct, "radiance").length != 0;

        setEnabled(enabled);
    }
}
