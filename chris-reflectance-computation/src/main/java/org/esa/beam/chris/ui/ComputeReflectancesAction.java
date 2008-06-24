package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Action for computing reflectances.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ComputeReflectancesAction extends AbstractVisatAction {

    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                           "CHRIS_M1_NR",
                           "CHRIS_M2_NR",
                           "CHRIS_M3_NR",
                           "CHRIS_M30_NR",
                           "CHRIS_M3A_NR",
                           "CHRIS_M4_NR",
                           "CHRIS_M5_NR");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final String sourceProductName = VisatApp.getApp().getSelectedProduct().getName();
        final SingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ComputeReflectances",
                                                     getAppContext(),
                                                     "Reflectance Computation",
                                                     "chrisReflectanceComputationTool");
        dialog.getTargetProductSelector().getModel().setProductName(sourceProductName.replace("_NR", "_REFL"));
        dialog.show();
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
        final int pos = name.lastIndexOf("_NR");
        if (pos != -1) {
            name.replace(pos, pos + 3, "_REFL");
        } else {
            name.append("_REFL");
        }
        parameterMap.put("targetProductName", name.toString());
        parameterMap.put("copyRadianceBands", true);
        final Product targetProduct = GPF.createProduct("chris.ComputeReflectances",
                                                        parameterMap,
                                                        sourceProduct);
        VisatApp.getApp().addProduct(targetProduct);
    }
}
