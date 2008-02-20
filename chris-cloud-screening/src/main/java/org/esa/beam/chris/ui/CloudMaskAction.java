package org.esa.beam.chris.ui;

import com.jidesoft.docking.DockingManager;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.chris.operators.ClusterMembershipOpImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.util.*;
import java.util.logging.Level;
import java.io.IOException;

/**
 * Action for masking clouds.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 */
public class CloudMaskAction extends AbstractVisatAction {
    private static List<String> CHRIS_TYPES;
    private Product clusterProduct;

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
            VisatApp.getApp().addProduct(product);
            visatApp.openProductSceneViewRGB(selectedProduct, "");
            visatApp.openProductSceneView(clusterProduct.getBand("membership_mask"), getHelpId());
            visatApp.openProductSceneView(product.getBand("cloud_probability"), getHelpId());
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
        findClustersOpParameterMap.put("clusterDistance", 0.0);

        clusterProduct = GPF.createProduct("chris.FindClusters",
                findClustersOpParameterMap,
                featureProduct);


        final Band memberShipBand = clusterProduct.getBand("membership_mask");
        try {
            memberShipBand.readRasterDataFully(ProgressMonitor.NULL);
            memberShipBand.unloadRasterData();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Band[] probabilityBands = new Band[14];
        for (int i = 0; i < clusterProduct.getNumBands() - 1; ++i) {
            probabilityBands[i] = clusterProduct.getBandAt(i);
        }
        memberShipBand.setImage(ClusterMembershipOpImage.create(probabilityBands, memberShipBand, new int[]{0, 8}));


        final Map<String, Product> sourceProductMap = new HashMap<String, Product>();
        sourceProductMap.put("toaRefl", reflectanceProduct);
        sourceProductMap.put("cluster", clusterProduct);

        final Map<String, Object> cloudProbabilityOpParameterMap = new HashMap<String, Object>();
//        cloudProbabilityOpParameterMap.put("accumulate", new int[]{7, 11, 13});
//        cloudProbabilityOpParameterMap.put("redistribute", new int[]{0, 8});
        cloudProbabilityOpParameterMap.put("accumulate", new int[]{7, 11, 13});
        cloudProbabilityOpParameterMap.put("redistribute", new int[]{0, 8});

        return GPF.createProduct("chris.ComputeCloudProbability", cloudProbabilityOpParameterMap, sourceProductMap);
    }
}
