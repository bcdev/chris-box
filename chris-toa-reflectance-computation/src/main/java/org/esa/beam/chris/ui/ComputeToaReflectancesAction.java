package org.esa.beam.chris.ui;

import org.esa.beam.chris.util.OpUtils;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for computing TOA reflectances.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeToaReflectancesAction extends AbstractVisatAction {
    private final AtomicReference<ModelessDialog> dialog;

    public ComputeToaReflectancesAction() {
        dialog = new AtomicReference<ModelessDialog>();
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                selectedProduct.getProductType().startsWith("CHRIS_M") &&
                        OpUtils.findBands(selectedProduct, "radiance").length != 0;

        setEnabled(enabled);
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ComputeToaReflectances",
                                                     appContext,
                                                     "CHRIS/Proba TOA Reflectance Computation",
                                                     "chrisToaReflectanceComputationTool");
        dialog.getJDialog().setName("chrisToaReflectanceComputationDialog");
        dialog.setTargetProductNameSuffix("_TOA_REFL");

        return dialog;
    }
}
