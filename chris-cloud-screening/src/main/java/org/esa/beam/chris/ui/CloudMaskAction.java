package org.esa.beam.chris.ui;

import com.jidesoft.docking.DockingManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 */
public class CloudMaskAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;

    static {
        CHRIS_TYPES = new ArrayList<String>();
        Collections.addAll(CHRIS_TYPES,
                "CHRIS_M1_REFL",
                "CHRIS_M2_REFL",
                "CHRIS_M3_REFL",
                "CHRIS_M30_REFL",
                "CHRIS_M3A_REFL",
                "CHRIS_M4_REFL",
                "CHRIS_M5_REFL");
    }

    @Override
    public void actionPerformed(CommandEvent commandEvent) {
        VisatApp visatApp = VisatApp.getApp();
//        DockableFrame spectrumView = visatApp.getMainFrame().getDockingManager().getFrame("org.esa.beam.visat.toolviews.spectrum.SpectrumToolView");
//        spectrumView.getContext().setCurrentDockSide(DockContext.DOCK_SIDE_SOUTH);
//        spectrumView.getContext().setInitIndex(0);

        try {
            final Product selectedProduct = visatApp.getSelectedProduct();
            Product clusterProduct = performAction(selectedProduct);
            VisatApp.getApp().addProduct(clusterProduct);
            visatApp.openProductSceneViewRGB(selectedProduct, "");
            visatApp.openProductSceneView(clusterProduct.getBand("membership_mask"), getHelpId());
            DockingManager dockingManager = visatApp.getMainFrame().getDockingManager();
//            dockingManager.showFrame("org.esa.beam.visat.toolviews.spectrum.SpectrumToolView");
            dockingManager.showFrame("org.esa.beam.chris.ui.CloudMaskLabelingToolView");
        } catch (OperatorException e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
            VisatApp.getApp().getLogger().log(Level.SEVERE, e.getMessage(), e);
        }

    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }

    private Product performAction(Product sourceProduct) throws OperatorException {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        final Product featureProduct = GPF.createProduct("chris.ExtractFeatures",
                parameterMap,
                sourceProduct);

        final HashMap<String, Object> clusterOpParameterMap = new HashMap<String, Object>();
        clusterOpParameterMap.put("sourceBandNames", new String[]{"brightness_vis",
                "brightness_nir",
                "whiteness_vis",
                "whiteness_nir",
                "wv"});
        clusterOpParameterMap.put("clusterCount", 14);
        clusterOpParameterMap.put("iterationCount", 20);
        clusterOpParameterMap.put("clusterDistance", 0.0);

        final Product clusterProduct = GPF.createProduct("chris.FindClusters",
                clusterOpParameterMap,
                featureProduct);

        final HashMap<String, Object> cloudMaskParameterMap = new HashMap<String, Object>();
        cloudMaskParameterMap.put("sourceBandNames", new String[]{"brightness_vis",
                "brightness_nir",
                "whiteness_vis",
                "whiteness_nir",
                "wv"});
        cloudMaskParameterMap.put("clusterCount", 14);
        cloudMaskParameterMap.put("iterationCount", 20);
        cloudMaskParameterMap.put("clusterDistance", 0.0);

        return clusterProduct;
    }
}
