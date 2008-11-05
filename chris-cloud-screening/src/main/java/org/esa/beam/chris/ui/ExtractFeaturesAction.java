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
 * Action for extracting features needed for cloud screening.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ExtractFeaturesAction extends AbstractVisatAction {
    private final AtomicReference<ModelessDialog> dialog;

    public ExtractFeaturesAction() {
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
                        OpUtils.findBands(selectedProduct, "toa_refl").length >= 18;

        setEnabled(enabled);
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ExtractFeatures",
                                                     appContext,
                                                     "CHRIS/PROBA Feature Extraction",
                                                     "chrisExtractFeaturesTools");
        dialog.setTargetProductNameSuffix("_FEAT");
        return dialog;
    }
}
