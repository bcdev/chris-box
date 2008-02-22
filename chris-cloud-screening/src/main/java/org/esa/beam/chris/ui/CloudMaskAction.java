package org.esa.beam.chris.ui;

import com.jidesoft.docking.DockingManager;
import org.esa.beam.chris.operators.MakeClusterMapOp;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.*;
import java.util.logging.Level;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 */
public class CloudMaskAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;
    private Product clusterProduct;
    private Product mapProduct;

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
            Product product = createFinalProduct(selectedProduct);
            VisatApp.getApp().addProduct(clusterProduct);
            VisatApp.getApp().addProduct(mapProduct);
            VisatApp.getApp().addProduct(product);
            visatApp.openProductSceneViewRGB(selectedProduct, "");
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

    private Product createFinalProduct(Product reflectanceProduct) throws OperatorException {
        final HashMap<String, Object> parameterMap = new HashMap<String, Object>();

        final Product featureProduct = GPF.createProduct("chris.ExtractFeatures",
                parameterMap,
                reflectanceProduct);

        final Map<String, Object> findClustersOpParameterMap = new HashMap<String, Object>();
        findClustersOpParameterMap.put("sourceBandNames", new String[]{"brightness_vis",
                "brightness_nir",
                "whiteness_vis",
                "whiteness_nir",
                "wv"});
        findClustersOpParameterMap.put("clusterCount", 14);
        findClustersOpParameterMap.put("iterationCount", 40);

        clusterProduct = GPF.createProduct("chris.FindClusters",
                findClustersOpParameterMap,
                featureProduct);

        mapProduct = GPF.createProduct("chris.MakeClusterMap", new HashMap<String, Object>(), clusterProduct);

        for (Band band : mapProduct.getBands()) {
            if (band.getName().startsWith("prob")) {
                final MakeClusterMapOp.ProbabilityImageBand probBand = (MakeClusterMapOp.ProbabilityImageBand) band;
                probBand.update(new int[]{0, 8});
            }
        }
        final MakeClusterMapOp.MembershipImageBand membershipBand = (MakeClusterMapOp.MembershipImageBand) mapProduct.getBand("membership_mask");
        membershipBand.update();

        final Map<String, Object> accumulateOpParameterMap = new HashMap<String, Object>();
        accumulateOpParameterMap.put("sourceBands",
                new String[]{"probability_10", "probability_11", "probability_13"});
        accumulateOpParameterMap.put("targetBand", "cloud_probability");
        // todo - product name and type

        return GPF.createProduct("chris.Accumulate", accumulateOpParameterMap, mapProduct);
    }
}
