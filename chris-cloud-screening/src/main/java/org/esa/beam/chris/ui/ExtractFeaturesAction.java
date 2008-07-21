package org.esa.beam.chris.ui;

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
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                           "CHRIS_M1_REFL",
                           "CHRIS_M2_REFL",
                           "CHRIS_M3_REFL",
                           "CHRIS_M3A_REFL",
                           "CHRIS_M30_REFL",
                           "CHRIS_M4_REFL",
                           "CHRIS_M5_REFL");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final DefaultSingleTargetProductDialog productDialog =
                new DefaultSingleTargetProductDialog("chris.ExtractFeatures",
                                                     getAppContext(),
                                                     "CHRIS/PROBA Feature Extraction",
                                                     "chrisExtractFeaturesTools");
        productDialog.show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct == null
                || CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }
}
