package org.esa.beam.chris.ui;


import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
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
        final VisatApp visatApp = VisatApp.getApp();
        try {
            Product reflectanceProduct = visatApp.getSelectedProduct();
            visatApp.openProductSceneViewRGB(reflectanceProduct, "");
            final CloudLabeler cloudLabeler = new CloudLabeler(reflectanceProduct);
            cloudLabeler.processStepOne();

            final Band membershipBand = cloudLabeler.getMembershipBand();
            visatApp.openProductSceneView(membershipBand, "");

            final DockingManager dockingManager = visatApp.getMainFrame().getDockingManager();
            final DockableFrame dockableFrame = new DockableFrame("Cluster Labeling");
            final ClusterLabelingToolView clusterLabelingView =
                    new ClusterLabelingToolView(dockableFrame.getKey(), cloudLabeler);

            final AbstractAction closeAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    dockingManager.removeFrame(dockableFrame.getKey());
                }
            };
            dockableFrame.setCloseAction(closeAction);
            dockableFrame.setContentPane(clusterLabelingView.getControl());
            dockingManager.addFrame(dockableFrame);
            dockingManager.showFrame(dockableFrame.getKey());
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
}
