package org.esa.beam.chris.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.JDialog;

/**
 * Action for masking clouds.
 *
 * @author Marco Zühlke
 * @author Marco Peters
 */
public class CloudMaskAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES, "CHRIS_M1", "CHRIS_M2", "CHRIS_M3", "CHRIS_M30", "CHRIS_M3A", "CHRIS_M4",
                "CHRIS_M5");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
VisatApp visatApp = VisatApp.getApp();
//        final DefaultSingleTargetProductDialog productDialog =
//                new DefaultSingleTargetProductDialog("chris.ExtractFeatures",
//                                                     getAppContext(),
//                                                     "Feature Extraction",
//                                                     "chrisCloudScreeningTools");
        final Product selectedProduct = visatApp.getSelectedProduct();
        visatApp.openProductSceneViewRGB(selectedProduct, "");
        visatApp.openProductSceneView(selectedProduct.getBandAt(0), getHelpId());
//        DockableFrame spectrumView = visatApp.getMainFrame().getDockingManager().getFrame("org.esa.beam.visat.toolviews.spectrum.SpectrumToolView");
//        spectrumView.getContext().setCurrentDockSide(DockContext.DOCK_SIDE_SOUTH);
//        spectrumView.getContext().setInitIndex(0);
        
        DockingManager dockingManager = visatApp.getMainFrame().getDockingManager();
        dockingManager.showFrame("org.esa.beam.visat.toolviews.spectrum.SpectrumToolView");
        dockingManager.showFrame("org.esa.beam.chris.ui.CloudMaskLabelingToolView");
         
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
