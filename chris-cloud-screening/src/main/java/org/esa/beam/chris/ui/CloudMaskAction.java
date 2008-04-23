package org.esa.beam.chris.ui;


import com.bc.ceres.swing.SwingHelper;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Container;
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

    private Map<Product, CloudLabeler> cloudLabelerMap = new WeakHashMap<Product, CloudLabeler>(5);
    private CloudMaskAction.CloudLabelerDisposer cloudLabelerDisposer = new CloudLabelerDisposer();

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
        final VisatApp visatApp = VisatApp.getApp();

        visatApp.getProductManager().addListener(cloudLabelerDisposer);

        final Product reflectanceProduct = visatApp.getSelectedProduct();
        try {
            final CloudLabeler cloudLabeler = getOrCreateCloudLabeler(reflectanceProduct);
            final Band membershipBand = cloudLabeler.getMembershipBand();
            final JInternalFrame internalFrame = visatApp.findInternalFrame(membershipBand);
            if (internalFrame != null) {
                final ProductSceneView productSceneView = getProductSceneView(internalFrame);
                openClusterLabelingWindow(internalFrame, productSceneView, cloudLabeler);
            } else {
                final String message = String.format(
                            "A cluster analysis of the reflectances in '%s'\n" +
                                "has to be performed for the following interactive cloud labeling.\n" +
                                "The cluster analysis may take considerable time.\n\n" +
                                "Do you want to continue?", reflectanceProduct.getDisplayName());
                final int answer = visatApp.showQuestionDialog("CHRIS Cloud Screening", message,
                                                               "chrisbox.preLabling.showWarning");
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
                InternalFrameListener internalFrameListener = new InternalFrameOpenHandler(membershipBand,
                                                                                           visatApp, reflectanceProduct, cloudLabeler);
                visatApp.addInternalFrameListener(internalFrameListener);
                visatApp.openProductSceneView(membershipBand, "");
            }
        } catch (Exception e) {
            visatApp.showErrorDialog(e.getMessage());
            visatApp.getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private CloudLabeler getOrCreateCloudLabeler(Product reflectanceProduct) {
        CloudLabeler cloudLabeler = cloudLabelerMap.get(reflectanceProduct);
        if (cloudLabeler == null) {
            cloudLabeler = createCloudLabeler(reflectanceProduct);
            cloudLabelerMap.put(reflectanceProduct, cloudLabeler);
        }
        return cloudLabeler;
    }

    private ClusterLabelingWindow openClusterLabelingWindow(JInternalFrame internalFrame, final ProductSceneView productSceneView, final CloudLabeler cloudLabeler) {
        final ClusterLabelingWindow clusterLabelingWindow = new ClusterLabelingWindow(productSceneView, cloudLabeler);
        clusterLabelingWindow.pack();
        SwingHelper.centerComponent(clusterLabelingWindow, VisatApp.getApp().getMainFrame());
        clusterLabelingWindow.setVisible(true);
        internalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                if (clusterLabelingWindow.isShowing()) {
                    clusterLabelingWindow.dispose();
                }
            }
        });
        return clusterLabelingWindow;
    }


    private CloudLabeler createCloudLabeler(Product reflectanceProduct) {
        CloudLabeler cloudLabeler = new CloudLabeler(reflectanceProduct);
        cloudLabeler.processStepOne();
        return cloudLabeler;
    }

    @Override
    public void updateState() {
        final Product selectedProduct = VisatApp.getApp().getSelectedProduct();
        final boolean enabled = selectedProduct != null
                && CHRIS_TYPES.contains(selectedProduct.getProductType());

        setEnabled(enabled);
    }

    private ProductSceneView findProductSceneView(Band band) {
        JInternalFrame internalFrame = VisatApp.getApp().findInternalFrame(band);
        return getProductSceneView(internalFrame);
    }

    private ProductSceneView getProductSceneView(JInternalFrame internalFrame) {
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }

    private class InternalFrameOpenHandler extends InternalFrameAdapter {
        private final Band membershipBand;
        private final VisatApp visatApp;
        private final Product reflectanceProduct;
        private final CloudLabeler cloudLabeler;

        public InternalFrameOpenHandler(Band membershipBand, VisatApp visatApp, Product reflectanceProduct, CloudLabeler cloudLabeler) {
            this.membershipBand = membershipBand;
            this.visatApp = visatApp;
            this.reflectanceProduct = reflectanceProduct;
            this.cloudLabeler = cloudLabeler;
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final JInternalFrame internalFrame = e.getInternalFrame();
            ProductSceneView productSceneView = getProductSceneView(internalFrame);
            if (productSceneView.getRaster() == membershipBand) {
                visatApp.removeInternalFrameListener(this);
                internalFrame.setTitle(reflectanceProduct.getDisplayName() + " - Cluster Memberships");
                openClusterLabelingWindow(internalFrame, productSceneView, cloudLabeler);
            }
        }
    }

    private class CloudLabelerDisposer implements ProductManager.Listener {
        public void productAdded(ProductManager.Event event) {

        }

        public void productRemoved(ProductManager.Event event) {
            final CloudLabeler cloudLabeler = cloudLabelerMap.get(event.getProduct());
            if (cloudLabeler != null) {
                cloudLabelerMap.remove(event.getProduct());
                cloudLabeler.dispose();
            }
        }
    }
}
