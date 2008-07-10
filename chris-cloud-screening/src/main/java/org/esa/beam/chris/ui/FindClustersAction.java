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
 * New class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class FindClustersAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                           "CHRIS_M1_FEAT",
                           "CHRIS_M2_FEAT",
                           "CHRIS_M3_FEAT",
                           "CHRIS_M3A_FEAT",
                           "CHRIS_M30_FEAT",
                           "CHRIS_M4_FEAT",
                           "CHRIS_M5_FEAT");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        final DefaultSingleTargetProductDialog productDialog =
                new DefaultSingleTargetProductDialog("chris.FindClusters",
                                                     getAppContext(),
                                                     "Cluster Analysis",
                                                     "chrisCloudScreeningTools");

        productDialog.show();
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }
}
