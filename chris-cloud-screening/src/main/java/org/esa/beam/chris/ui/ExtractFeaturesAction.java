package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
                                                     "Feature Extraction",
                                                     "chrisCloudScreeningTools");
        productDialog.show();
//        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
//
//        try {
//            performAction(selectedProduct);
//        } catch (OperatorException e) {
//            VisatApp.getApp().showErrorDialog(e.getMessage());
//            VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
//        }
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }

    private static void performAction(Product sourceProduct) throws OperatorException {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        final StringBuilder name = new StringBuilder(sourceProduct.getName());
        final int pos = name.lastIndexOf("_REFL");
        if (pos != -1) {
            name.replace(pos, pos + 5, "_FEAT");
        } else {
            name.append("_FEAT");
        }
        parameterMap.put("targetProductName", name.toString());
        final Product targetProduct = GPF.createProduct("chris.ExtractFeatures",
                                                        parameterMap,
                                                        sourceProduct);
        VisatApp.getApp().addProduct(targetProduct);
    }
}
